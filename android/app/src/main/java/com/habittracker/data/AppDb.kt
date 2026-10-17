package com.habittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Habit::class, Checkin::class, Freeze::class, OutboxItem::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDb : RoomDatabase() {
    abstract fun habits(): HabitDao
    abstract fun checkins(): CheckinDao
    abstract fun freezes(): FreezeDao
    abstract fun outbox(): OutboxDao

    companion object {
        @Volatile private var instance: AppDb? = null

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDb::class.java, "habits.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
