package com.habittracker.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.habittracker.HabitApp
import com.habittracker.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_CHECK -> handleCheck(context, habitId)
            ACTION_SNOOZE -> snooze(context, intent)
            else -> handleRemind(context, habitId, intent.getStringExtra(EXTRA_NAME).orEmpty())
        }
    }

    private fun handleRemind(context: Context, habitId: String, name: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(notifId(habitId), notification(context, habitId, name))
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                HabitApp.repo.habitsOnce().firstOrNull { it.id == habitId }?.let {
                    ReminderScheduler.schedule(context, it)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleCheck(context: Context, habitId: String) {
        context.getSystemService(NotificationManager::class.java).cancel(notifId(habitId))
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { HabitApp.repo.checkInNow(habitId) } finally { pending.finish() }
        }
    }

    private fun snooze(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_ID) ?: return
        val alarm = context.getSystemService(AlarmManager::class.java)
        val fireAt = System.currentTimeMillis() + 10 * 60 * 1000
        val pi = PendingIntent.getBroadcast(
            context, habitId.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_ID, habitId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } catch (e: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    private fun notification(context: Context, habitId: String, name: String) = NotificationCompat.Builder(context, HabitApp.REMINDER_CHANNEL)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle(name.ifEmpty { "Waktunya check-in" })
        .setContentText("Jangan lupa selesaikan habit hari ini")
        .setAutoCancel(true)
        .setContentIntent(PendingIntent.getActivity(context, habitId.hashCode(), Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
        .addAction(0, "Selesai", broadcast(context, habitId, ACTION_CHECK))
        .addAction(0, "Tunda 10m", broadcast(context, habitId, ACTION_SNOOZE))
        .build()

    private fun broadcast(context: Context, habitId: String, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, (habitId + action).hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(action).putExtra(EXTRA_ID, habitId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notifId(habitId: String) = habitId.hashCode() and 0x7FFFFFFF

    companion object {
        const val ACTION_REMIND = "com.habittracker.reminder.REMIND"
        const val ACTION_CHECK = "com.habittracker.reminder.CHECK"
        const val ACTION_SNOOZE = "com.habittracker.reminder.SNOOZE"
        const val EXTRA_ID = "habitId"
        const val EXTRA_NAME = "habitName"
    }
}
