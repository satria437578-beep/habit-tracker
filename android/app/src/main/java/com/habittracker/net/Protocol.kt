package com.habittracker.net

import org.json.JSONObject

object Msg {
    const val AUTH_REGISTER = "auth.register"
    const val AUTH = "auth"
    const val AUTH_OK = "auth.ok"
    const val AUTH_ERR = "auth.err"
    const val SYNC_PULL = "sync.pull"
    const val SYNC_STATE = "sync.state"
    const val SYNC_ACK = "sync.ack"
    const val SYNC_UPDATE = "sync.update"
    const val PING = "ping"
    const val PONG = "pong"
    const val ERROR = "error"

    fun habitPut() = "habit.put"
    fun habitDelete() = "habit.delete"
    fun checkinPut() = "checkin.put"
    fun checkinDelete() = "checkin.delete"
    fun freezePut() = "freeze.put"
    fun freezeDelete() = "freeze.delete"
}

data class Envelope(val type: String, val id: String?, val data: JSONObject)

fun encode(type: String, id: String?, data: JSONObject): String =
    JSONObject().put("type", type).put("id", id ?: JSONObject.NULL).put("data", data).toString()

fun decode(text: String): Envelope {
    val o = JSONObject(text)
    return Envelope(
        type = o.getString("type"),
        id = if (o.isNull("id")) null else o.optString("id"),
        data = o.optJSONObject("data") ?: JSONObject(),
    )
}
