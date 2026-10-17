package com.habittracker.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields

object Stats {
    private val iso = WeekFields.ISO

    fun habitRate(habit: Habit, checkins: List<Checkin>, from: LocalDate, to: LocalDate): Float? {
        if (to.isBefore(from)) return null
        val days = generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val completedDates = checkins.filter { StreakCalculator.isComplete(habit, it) }.map { it.date }.toSet()
        return when (habit.scheduleType) {
            "weekly" -> {
                val weeks = days.map { it.get(iso.weekBasedYear()) to it.get(iso.weekOfWeekBasedYear()) }.distinct().size
                val required = weeks * habit.timesPerWeek.coerceAtLeast(1)
                if (required == 0) null
                else (completedDates.count { LocalDate.parse(it) in days }.toFloat() / required).coerceIn(0f, 1f)
            }
            else -> {
                val due = days.filter { StreakCalculator.isScheduled(habit, it) }
                if (due.isEmpty()) null
                else due.count { it.toString() in completedDates }.toFloat() / due.size
            }
        }
    }

    fun overallRate(habits: List<Habit>, checkinsByHabit: Map<String, List<Checkin>>, from: LocalDate, to: LocalDate): Float {
        val rates = habits.mapNotNull { habitRate(it, checkinsByHabit[it.id].orEmpty(), from, to) }
        return if (rates.isEmpty()) 0f else rates.average().toFloat()
    }

    fun weekRange(today: LocalDate): Pair<LocalDate, LocalDate> {
        val start = today.with(iso.dayOfWeek(), DayOfWeek.MONDAY.value.toLong())
        return start to start.plusDays(6)
    }

    fun monthRange(today: LocalDate): Pair<LocalDate, LocalDate> =
        today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())

    fun bestWeekday(checkins: List<Checkin>): Int? {
        val counts = checkins.groupingBy { LocalDate.parse(it.date).dayOfWeek.value }.eachCount()
        return counts.maxByOrNull { it.value }?.key
    }

    fun trend(habits: List<Habit>, checkinsByHabit: Map<String, List<Checkin>>, today: LocalDate, days: Int = 14): List<Float> =
        (days - 1 downTo 0).map { offset ->
            val day = today.minusDays(offset.toLong())
            val due = habits.filter {
                StreakCalculator.isScheduled(it, day) ||
                    (it.scheduleType == "weekly" && checkinsByHabit[it.id].orEmpty().any { c -> c.date == day.toString() })
            }
            if (due.isEmpty()) 0f
            else due.count { StreakCalculator.isComplete(it, checkinsByHabit[it.id].orEmpty().firstOrNull { c -> c.date == day.toString() }) }.toFloat() / due.size
        }

    fun badges(longest: Int): List<Int> = listOf(7, 30, 100).filter { longest >= it }

    fun dayRatio(habits: List<Habit>, checkinsByHabit: Map<String, List<Checkin>>, date: LocalDate): Float? {
        val due = habits.filter { it.scheduleType != "weekly" && StreakCalculator.isScheduled(it, date) }
        if (due.isEmpty()) return null
        val done = due.count { habit ->
            StreakCalculator.isComplete(habit, checkinsByHabit[habit.id].orEmpty().firstOrNull { it.date == date.toString() })
        }
        return done.toFloat() / due.size
    }

    fun weekRatio(habits: List<Habit>, checkinsByHabit: Map<String, List<Checkin>>, weekStart: LocalDate): Float {
        var due = 0
        var done = 0
        for (i in 0..6) {
            val day = weekStart.plusDays(i.toLong())
            for (habit in habits) {
                if (habit.scheduleType == "weekly" || !StreakCalculator.isScheduled(habit, day)) continue
                due++
                if (StreakCalculator.isComplete(habit, checkinsByHabit[habit.id].orEmpty().firstOrNull { it.date == day.toString() })) done++
            }
        }
        return if (due == 0) 0f else done.toFloat() / due
    }

    fun monthGrid(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1)
        val start = first.with(iso.dayOfWeek(), DayOfWeek.MONDAY.value.toLong())
        val end = month.atEndOfMonth().with(iso.dayOfWeek(), DayOfWeek.SUNDAY.value.toLong())
        val count = ChronoUnit.DAYS.between(start, end).toInt() + 1
        return (0 until count).map { start.plusDays(it.toLong()) }
    }
}
