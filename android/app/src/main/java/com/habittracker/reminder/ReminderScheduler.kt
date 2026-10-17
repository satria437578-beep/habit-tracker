package com.habittracker.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.habittracker.data.Habit
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    fun schedule(context: Context, habit: Habit) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        cancel(context, habit)
        val time = parseTime(habit.reminder) ?: return
        val next = nextTrigger(habit, time) ?: return
        val trigger = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pending(context, habit)
        try {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, habit: Habit) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, habit))
    }

    fun rescheduleAll(context: Context, habits: List<Habit>) {
        habits.filter { it.reminder.isNotBlank() && !it.archived }.forEach { schedule(context, it) }
    }

    private fun pending(context: Context, habit: Habit): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(ReminderReceiver.EXTRA_ID, habit.id)
            putExtra(ReminderReceiver.EXTRA_NAME, habit.name)
        }
        return PendingIntent.getBroadcast(context, habit.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun parseTime(value: String): LocalTime? = runCatching {
        val parts = value.split(':')
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }.getOrNull()

    private fun nextTrigger(habit: Habit, time: LocalTime): LocalDateTime? {
        val now = LocalDateTime.now()
        var day = LocalDate.now()
        repeat(8) {
            if (isScheduledOn(habit, day)) {
                val candidate = LocalDateTime.of(day, time)
                if (candidate.isAfter(now)) return candidate
            }
            day = day.plusDays(1)
        }
        return null
    }

    private fun isScheduledOn(habit: Habit, day: LocalDate): Boolean = when (habit.scheduleType) {
        "weekdays" -> day.dayOfWeek.value in habit.scheduleDays
            .split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        else -> true
    }
}
