package com.habittracker.data

import android.content.Context
import com.habittracker.net.ConnState
import com.habittracker.net.Envelope
import com.habittracker.net.Msg
import com.habittracker.net.WsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SyncRepository(private val context: Context) {
    private val db = AppDb.get(context)
    val store = TokenStore(context)
    val ws = WsClient(store)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notice = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val notice: SharedFlow<String> = _notice.asSharedFlow()

    val habits = db.habits().active()
    val allHabits = db.habits().all()
    val checkins = db.checkins().all()
    val freezes = db.freezes().all()
    val outboxCount = db.outbox().count()
    val connection: StateFlow<ConnState> = ws.state

    init {
        scope.launch { ws.incoming.collect { handle(it) } }
        scope.launch { ws.connectionEvents.collect { onConnected() } }
        ws.connect()
    }

    fun reconnect() {
        store.lastSince = 0
        ws.connect()
    }

    private suspend fun onConnected() {
        flushOutbox()
        pull()
    }

    fun pull() {
        ws.send(Msg.SYNC_PULL, null, JSONObject().put("since", store.lastSince))
    }

    private suspend fun flushOutbox() {
        if (ws.state.value != ConnState.ONLINE) return
        for (item in db.outbox().all()) {
            ws.send(item.type, item.seq.toString(), JSONObject(item.payload))
        }
    }

    private suspend fun enqueue(type: String, data: JSONObject) {
        val seq = db.outbox().add(OutboxItem(type = type, payload = data.toString()))
        if (ws.state.value == ConnState.ONLINE) {
            ws.send(type, seq.toString(), data)
        }
    }

    private suspend fun handle(env: Envelope) {
        when (env.type) {
            Msg.SYNC_STATE -> applyState(env.data)
            Msg.SYNC_UPDATE -> applyRows(env.data, force = true)
            Msg.SYNC_ACK -> onAck(env.id, env.data)
            Msg.ERROR -> _notice.tryEmit("Server error: ${env.data.optString("code")}")
        }
    }

    private suspend fun applyState(data: JSONObject) {
        for (habit in rows(data, "habits")) applyRow("habit", habit, force = false)
        for (checkin in rows(data, "checkins")) applyRow("checkin", checkin, force = false)
        for (freeze in rows(data, "freezes")) applyRow("freeze", freeze, force = false)
        store.lastSince = maxOf(store.lastSince, data.optLong("since", 0))
    }

    private suspend fun onAck(id: String?, data: JSONObject) {
        id?.toLongOrNull()?.let { db.outbox().remove(it) }
        applyRows(data, force = true)
    }

    private suspend fun applyRows(data: JSONObject, force: Boolean) {
        val entity = data.optString("entity")
        for (row in rows(data, "rows")) applyRow(entity, row, force)
    }

    private fun rows(data: JSONObject, key: String): List<JSONObject> {
        val arr: JSONArray = data.optJSONArray(key) ?: return emptyList()
        return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }

    private suspend fun applyRow(entity: String, json: JSONObject, force: Boolean) {
        when (entity) {
            "habit" -> {
                val id = json.getString("id")
                if (deleted(json)) { db.habits().delete(id); return }
                if (!force && db.habits().byId(id)?.dirty == true) return
                db.habits().upsert(json.toHabit())
                db.habits().clearDirty(id)
            }
            "checkin" -> {
                val id = json.getString("id")
                if (deleted(json)) { db.checkins().delete(id); return }
                if (!force && db.checkins().one(json.getString("habit_id"), json.getString("date"))?.dirty == true) return
                db.checkins().upsert(json.toCheckin())
                db.checkins().clearDirty(id)
            }
            "freeze" -> {
                val id = json.getString("id")
                if (deleted(json)) { db.freezes().delete(id); return }
                db.freezes().upsert(json.toFreeze())
                db.freezes().clearDirty(id)
            }
        }
    }

    suspend fun saveHabit(habit: Habit) {
        val h = habit.copy(updatedAt = System.currentTimeMillis(), dirty = true)
        db.habits().upsert(h)
        enqueue(Msg.habitPut(), h.toJson())
    }

    suspend fun archiveHabit(habit: Habit, archived: Boolean) = saveHabit(habit.copy(archived = archived))

    suspend fun deleteHabit(id: String) {
        db.habits().delete(id)
        enqueue(Msg.habitDelete(), JSONObject().put("id", id))
    }

    suspend fun setCheckin(habitId: String, date: String, value: Double, duration: Int = 0, note: String = "") {
        val c = Checkin(
            id = "$habitId:$date", habitId = habitId, date = date,
            value = value, duration = duration, note = note,
            updatedAt = System.currentTimeMillis(), dirty = true,
        )
        db.checkins().upsert(c)
        enqueue(Msg.checkinPut(), c.toJson())
    }

    suspend fun removeCheckin(habitId: String, date: String) {
        val id = "$habitId:$date"
        db.checkins().delete(id)
        enqueue(Msg.checkinDelete(), JSONObject().put("id", id))
    }

    suspend fun tapHabit(habit: Habit, date: String) {
        val existing = db.checkins().one(habit.id, date)
        if (habit.targetType == "numeric") {
            setCheckin(habit.id, date, (existing?.value ?: 0.0) + 1, 0, existing?.note ?: "")
        } else if (existing != null && StreakCalculator.isComplete(habit, existing)) {
            removeCheckin(habit.id, date)
        } else {
            setCheckin(habit.id, date, 1.0, 0, existing?.note ?: "")
        }
    }

    suspend fun setNote(habit: Habit, date: String, note: String) {
        val existing = db.checkins().one(habit.id, date)
        setCheckin(habit.id, date, existing?.value ?: 1.0, existing?.duration ?: 0, note)
    }

    suspend fun habitsOnce(): List<Habit> = db.habits().all().first()

    suspend fun todayHabitsOnce(): List<Pair<Habit, Boolean>> {
        val now = java.time.LocalDate.now()
        val checks = db.checkins().forDate(now.toString()).groupBy { it.habitId }
        return db.habits().all().first()
            .filter { !it.archived && (it.scheduleType == "weekly" || StreakCalculator.isScheduled(it, now)) }
            .sortedBy { it.sortOrder }
            .map { it to StreakCalculator.isComplete(it, checks[it.id]?.firstOrNull()) }
    }

    suspend fun checkInNow(habitId: String) {
        val habit = db.habits().byId(habitId) ?: return
        tapHabit(habit, java.time.LocalDate.now().toString())
    }

    suspend fun setFreeze(habitId: String, date: String, on: Boolean) {
        val id = "$habitId:$date"
        if (on) {
            val f = Freeze(id, habitId, date, System.currentTimeMillis(), dirty = true)
            db.freezes().upsert(f)
            enqueue(Msg.freezePut(), f.toJson())
        } else {
            db.freezes().delete(id)
            enqueue(Msg.freezeDelete(), JSONObject().put("id", id))
        }
    }
}
