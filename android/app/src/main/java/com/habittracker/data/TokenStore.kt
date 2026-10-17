package com.habittracker.data

import android.content.Context
import java.util.UUID

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("habit_sync", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("serverUrl", "ws://10.0.2.2:8080")!!
        set(v) = prefs.edit().putString("serverUrl", v).apply()

    var token: String
        get() = prefs.getString("token", "")!!
        set(v) = prefs.edit().putString("token", v).apply()

    var deviceId: String
        get() {
            val v = prefs.getString("deviceId", null)
            if (v != null) return v
            val id = UUID.randomUUID().toString()
            prefs.edit().putString("deviceId", id).apply()
            return id
        }
        set(v) = prefs.edit().putString("deviceId", v).apply()

    var lastSince: Long
        get() = prefs.getLong("lastSince", 0)
        set(v) = prefs.edit().putLong("lastSince", v).apply()
}
