package com.habittracker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

data class Streak(val current: Int, val longest: Int)

object StreakCalculator {
    private val iso = WeekFields.ISO

    fun scheduledDays(habit: Habit): Set<Int> =
        habit.scheduleDays.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()

    fun isScheduled(habit: Habit, date: LocalDate): Boolean = when (habit.scheduleType) {
        "weekdays" -> date.dayOfWeek.value in scheduledDays(habit)
        "weekly" -> false
        else -> true
    }

    fun isComplete(habit: Habit, checkin: Checkin?): Boolean {
        if (checkin == null) return false
        return when (habit.targetType) {
            "numeric" -> checkin.value >= habit.targetValue.coerceAtLeast(1.0)
            "timer" -> checkin.duration >= habit.targetValue.coerceAtLeast(1.0).toInt()
            else -> checkin.value >= 1.0
        }
    }

    fun compute(
        habit: Habit,
        checkins: List<Checkin>,
        freezes: Set<String>,
        today: LocalDate = LocalDate.now(),
    ): Streak {
        val completed = checkins.filter { isComplete(habit, it) }.map { it.date }.toSet()
        return if (habit.scheduleType == "weekly") weekly(habit, completed, today)
        else daily(habit, completed, freezes, today)
    }

    private fun daily(habit: Habit, completed: Set<String>, freezes: Set<String>, today: LocalDate): Streak {
        val start = (completed + freezes).minOrNull()?.let { LocalDate.parse(it) } ?: today
        var run = 0
        var longest = 0
        var d = start
        while (!d.isAfter(today)) {
            val key = d.toString()
            if (isScheduled(habit, d)) {
                when {
                    key in completed -> { run++; if (run > longest) longest = run }
                    key in freezes -> Unit
                    d == today -> Unit
                    else -> run = 0
                }
            }
            d = d.plusDays(1)
        }
        return Streak(run, longest)
    }

    private fun weekly(habit: Habit, completed: Set<String>, today: LocalDate): Streak {
        val target = habit.timesPerWeek.coerceAtLeast(1)
        val byWeek = completed.map { LocalDate.parse(it) }
            .groupBy { it.get(iso.weekBasedYear()) to it.get(iso.weekOfWeekBasedYear()) }
        val first = byWeek.keys.minWithOrNull(compareBy({ it.first }, { it.second })) ?: return Streak(0, 0)
        val todayWeek = today.get(iso.weekBasedYear()) to today.get(iso.weekOfWeekBasedYear())

        var run = 0
        var longest = 0
        var d = weekStart(first.first, first.second)
        while (!d.isAfter(today)) {
            val key = d.get(iso.weekBasedYear()) to d.get(iso.weekOfWeekBasedYear())
            val count = byWeek[key]?.size ?: 0
            when {
                count >= target -> { run++; if (run > longest) longest = run }
                key == todayWeek -> Unit
                else -> run = 0
            }
            d = d.plusWeeks(1)
        }
        return Streak(run, longest)
    }

    private fun weekStart(year: Int, week: Int): LocalDate =
        LocalDate.of(year, 1, 4)
            .with(iso.weekOfWeekBasedYear(), week.toLong())
            .with(iso.dayOfWeek(), DayOfWeek.MONDAY.value.toLong())
}
