package com.habittracker.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.habittracker.HabitApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context, HabitApp.repo.habitsOnce())
            } finally {
                pending.finish()
            }
        }
    }
}
