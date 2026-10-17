package com.habittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "",
    val scheduleType: String = "daily",
    val scheduleDays: String = "",
    val timesPerWeek: Int = 0,
    val targetType: String = "check",
    val targetValue: Double = 0.0,
    val unit: String = "",
    val reminder: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: Long = 0,
    val dirty: Boolean = false,
)

@Entity(tableName = "checkins")
data class Checkin(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val value: Double = 1.0,
    val duration: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0,
    val dirty: Boolean = false,
)

@Entity(tableName = "freezes")
data class Freeze(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val updatedAt: Long = 0,
    val dirty: Boolean = false,
)

@Entity(tableName = "outbox")
data class OutboxItem(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val type: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
)
