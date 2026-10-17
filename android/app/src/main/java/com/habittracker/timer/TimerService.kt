package com.habittracker.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.habittracker.HabitApp
import com.habittracker.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var elapsed = 0
    private var target = 0
    private var habitId = ""
    private var habitName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { save(); return START_NOT_STICKY }
            else -> {
                habitId = intent?.getStringExtra(EXTRA_ID).orEmpty()
                habitName = intent?.getStringExtra(EXTRA_NAME).orEmpty()
                target = intent?.getIntExtra(EXTRA_TARGET, 60) ?: 60
                if (habitId.isEmpty()) return START_NOT_STICKY
                start()
            }
        }
        return START_STICKY
    }

    private fun start() {
        elapsed = 0
        ServiceCompat.startForeground(
            this,
            NOTIF_ID,
            build("0:00 / ${fmt(target)}"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        job?.cancel()
        job = scope.launch {
            while (isActive) {
                delay(1000)
                elapsed++
                update(build("${fmt(elapsed)} / ${fmt(target)}"))
                if (elapsed >= target) { save(); break }
            }
        }
    }

    private fun save() {
        val duration = elapsed
        job?.cancel()
        if (habitId.isNotEmpty() && duration > 0) {
            scope.launch {
                HabitApp.repo.setCheckin(habitId, LocalDate.now().toString(), 1.0, duration)
            }
        }
        stopSelf()
    }

    private fun build(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, TimerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, HabitApp.TIMER_CHANNEL)
            .setContentTitle(habitName.ifEmpty { "Timer" })
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Selesai", stop)
            .setSilent(true)
            .build()
    }

    private fun update(notification: Notification) {
        getSystemService(android.app.NotificationManager::class.java).notify(NOTIF_ID, notification)
    }

    private fun fmt(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.habittracker.timer.STOP"
        const val EXTRA_ID = "habitId"
        const val EXTRA_NAME = "habitName"
        const val EXTRA_TARGET = "targetSeconds"
        private const val NOTIF_ID = 2001

        fun start(context: Context, habitId: String, name: String, targetSeconds: Int) {
            val intent = Intent(context, TimerService::class.java)
                .putExtra(EXTRA_ID, habitId)
                .putExtra(EXTRA_NAME, name)
                .putExtra(EXTRA_TARGET, targetSeconds)
            context.startForegroundService(intent)
        }
    }
}
