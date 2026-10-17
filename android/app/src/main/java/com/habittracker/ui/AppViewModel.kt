package com.habittracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habittracker.HabitApp
import com.habittracker.data.Checkin
import com.habittracker.data.Freeze
import com.habittracker.data.Habit
import com.habittracker.data.Stats
import com.habittracker.data.Streak
import com.habittracker.data.StreakCalculator
import com.habittracker.net.ConnState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitUi(
    val habit: Habit,
    val streak: Streak,
    val today: Checkin?,
    val done: Boolean,
    val progress: Float,
    val badges: List<Int>,
)

data class UiState(
    val date: LocalDate = LocalDate.now(),
    val selected: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val today: List<HabitUi> = emptyList(),
    val selectedHabits: List<HabitUi> = emptyList(),
    val archived: List<Habit> = emptyList(),
    val connection: ConnState = ConnState.OFFLINE,
    val pending: Int = 0,
    val weeklyRate: Float = 0f,
    val monthlyRate: Float = 0f,
    val bestWeekday: Int? = null,
    val trend: List<Float> = emptyList(),
    val heatmap: Map<LocalDate, Int> = emptyMap(),
    val dayRatios: Map<LocalDate, Float> = emptyMap(),
    val monthGrid: List<LocalDate> = emptyList(),
    val monthRate: Float = 0f,
    val monthDone: Int = 0,
    val topStreak: Int = 0,
    val weekRatios: List<Pair<LocalDate, Float>> = emptyList(),
    val checkinsByHabit: Map<String, List<Checkin>> = emptyMap(),
    val freezesByHabit: Map<String, Set<String>> = emptyMap(),
) {
    fun checkinsOf(habitId: String): List<Checkin> = checkinsByHabit[habitId].orEmpty()
    fun freezesOf(habitId: String): Set<String> = freezesByHabit[habitId].orEmpty()
    fun ratioOf(date: LocalDate): Float? = dayRatios[date]
}

private data class Raw(
    val habits: List<Habit>,
    val checkins: List<Checkin>,
    val freezes: List<Freeze>,
    val connection: ConnState,
)

class AppViewModel : ViewModel() {
    private val repo = HabitApp.repo
    private val today = MutableStateFlow(LocalDate.now())
    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow(LocalDate.now())

    private val raw = combine(repo.allHabits, repo.checkins, repo.freezes, repo.connection) { h, c, f, conn ->
        Raw(h, c, f, conn)
    }

    val uiState = combine(raw, repo.outboxCount, today, month, selected) { r, pending, date, mo, sel ->
        build(r, pending, date, mo, sel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun refreshDate() {
        today.value = LocalDate.now()
        if (YearMonth.now() != month.value) month.value = YearMonth.now()
    }

    fun syncNow() = repo.pull()
    fun reconnect() = repo.reconnect()

    fun tap(habit: Habit) = viewModelScope.launch { repo.tapHabit(habit, today.value.toString()) }
    fun tapOn(habit: Habit, date: LocalDate) = viewModelScope.launch { repo.tapHabit(habit, date.toString()) }
    fun undo(habit: Habit) = viewModelScope.launch { repo.removeCheckin(habit.id, today.value.toString()) }
    fun setValue(habit: Habit, date: String, value: Double, duration: Int = 0) =
        viewModelScope.launch { repo.setCheckin(habit.id, date, value, duration) }
    fun setNote(habit: Habit, date: String, note: String) =
        viewModelScope.launch { repo.setNote(habit, date, note) }
    fun setFreeze(habitId: String, date: String, on: Boolean) =
        viewModelScope.launch { repo.setFreeze(habitId, date, on) }
    fun save(habit: Habit) = viewModelScope.launch { repo.saveHabit(habit) }
    fun archive(habit: Habit, archived: Boolean) = viewModelScope.launch { repo.archiveHabit(habit, archived) }
    fun delete(habit: Habit) = viewModelScope.launch { repo.deleteHabit(habit.id) }

    fun showMonth(value: YearMonth) { month.value = value }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun goToday() { month.value = YearMonth.now(); selected.value = LocalDate.now() }
    fun selectDay(date: LocalDate) {
        selected.value = date
        if (YearMonth.from(date) != month.value) month.value = YearMonth.from(date)
    }

    private fun build(r: Raw, pending: Int, date: LocalDate, mo: YearMonth, sel: LocalDate): UiState {
        val checkinsByHabit = r.checkins.groupBy { it.habitId }
        val freezesByHabit = r.freezes.groupBy { it.habitId }.mapValues { (_, v) -> v.map { it.date }.toSet() }
        val habitsById = r.habits.associateBy { it.id }
        val active = r.habits.filter { !it.archived }

        fun habitUi(habit: Habit, day: LocalDate): HabitUi {
            val checks = checkinsByHabit[habit.id].orEmpty()
            val check = checks.firstOrNull { it.date == day.toString() }
            val streak = StreakCalculator.compute(habit, checks, freezesByHabit[habit.id].orEmpty(), day)
            return HabitUi(
                habit = habit,
                streak = streak,
                today = check,
                done = StreakCalculator.isComplete(habit, check),
                progress = progressOf(habit, check),
                badges = Stats.badges(streak.longest),
            )
        }

        fun listFor(day: LocalDate): List<HabitUi> =
            active.filter { it.scheduleType == "weekly" || StreakCalculator.isScheduled(it, day) }
                .map { habitUi(it, day) }
                .sortedWith(compareBy({ it.done }, { it.habit.sortOrder }, { it.habit.name }))

        val grid = Stats.monthGrid(mo)
        val ratios = HashMap<LocalDate, Float>()
        for (day in grid) Stats.dayRatio(active, checkinsByHabit, day)?.let { ratios[day] = it }

        val inMonthRatios = ratios.filterKeys { YearMonth.from(it) == mo }.values
        val monthRate = if (inMonthRatios.isEmpty()) 0f else inMonthRatios.average().toFloat()
        val monthDone = r.checkins.count { c ->
            YearMonth.from(LocalDate.parse(c.date)) == mo &&
                habitsById[c.habitId]?.let { StreakCalculator.isComplete(it, c) } == true
        }

        val weekStart = Stats.weekRange(date).first
        val weekRatios = (5 downTo 0).map { offset ->
            val start = weekStart.minusWeeks(offset.toLong())
            start to Stats.weekRatio(active, checkinsByHabit, start)
        }

        val topStreak = active.maxOfOrNull {
            StreakCalculator.compute(it, checkinsByHabit[it.id].orEmpty(), freezesByHabit[it.id].orEmpty(), date).longest
        } ?: 0

        val heat = r.checkins
            .filter { c -> habitsById[c.habitId]?.let { StreakCalculator.isComplete(it, c) } == true }
            .groupBy { LocalDate.parse(it.date) }
            .mapValues { it.value.size }

        val (weekA, weekB) = Stats.weekRange(date)
        val (monthA, monthB) = Stats.monthRange(date)

        return UiState(
            date = date,
            selected = sel,
            month = mo,
            today = listFor(date),
            selectedHabits = listFor(sel),
            archived = r.habits.filter { it.archived },
            connection = r.connection,
            pending = pending,
            weeklyRate = Stats.overallRate(active, checkinsByHabit, weekA, date.coerceAtMost(weekB)),
            monthlyRate = Stats.overallRate(active, checkinsByHabit, monthA, date.coerceAtMost(monthB)),
            bestWeekday = Stats.bestWeekday(r.checkins),
            trend = Stats.trend(active, checkinsByHabit, date),
            heatmap = heat,
            dayRatios = ratios,
            monthGrid = grid,
            monthRate = monthRate,
            monthDone = monthDone,
            topStreak = topStreak,
            weekRatios = weekRatios,
            checkinsByHabit = checkinsByHabit,
            freezesByHabit = freezesByHabit,
        )
    }

    private fun progressOf(habit: Habit, check: Checkin?): Float = when (habit.targetType) {
        "numeric" -> if (habit.targetValue <= 0) 0f else ((check?.value ?: 0.0) / habit.targetValue).toFloat().coerceIn(0f, 1f)
        "timer" -> if (habit.targetValue <= 0) 0f else ((check?.duration ?: 0) / habit.targetValue).toFloat().coerceIn(0f, 1f)
        else -> if (StreakCalculator.isComplete(habit, check)) 1f else 0f
    }
}
