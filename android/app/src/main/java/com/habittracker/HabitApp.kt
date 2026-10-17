package com.habittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.habittracker.data.SyncRepository

class HabitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        repo = SyncRepository(this)
        createChannels()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        listOf(
            Triple(REMINDER_CHANNEL, "Pengingat Habit", NotificationManager.IMPORTANCE_HIGH),
            Triple(TIMER_CHANNEL, "Timer Durasi", NotificationManager.IMPORTANCE_LOW),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }

    companion object {
        const val REMINDER_CHANNEL = "reminders"
        const val TIMER_CHANNEL = "timer"
        lateinit var repo: SyncRepository
            private set
    }
}
