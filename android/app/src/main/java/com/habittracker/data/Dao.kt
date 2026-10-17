package com.habittracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder, name")
    fun all(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY sortOrder, name")
    fun active(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun byId(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: Habit)

    @Query("UPDATE habits SET dirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM habits")
    suspend fun clear()
}

@Dao
interface CheckinDao {
    @Query("SELECT * FROM checkins")
    fun all(): Flow<List<Checkin>>

    @Query("SELECT * FROM checkins WHERE habitId = :habitId ORDER BY date")
    fun byHabit(habitId: String): Flow<List<Checkin>>

    @Query("SELECT * FROM checkins WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun one(habitId: String, date: String): Checkin?

    @Query("SELECT * FROM checkins WHERE date = :date")
    suspend fun forDate(date: String): List<Checkin>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(checkin: Checkin)

    @Query("UPDATE checkins SET dirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)

    @Query("DELETE FROM checkins WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE checkins SET dirty = 0")
    suspend fun clearAllDirty()
}

@Dao
interface FreezeDao {
    @Query("SELECT * FROM freezes")
    fun all(): Flow<List<Freeze>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(freeze: Freeze)

    @Query("UPDATE freezes SET dirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)

    @Query("DELETE FROM freezes WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox ORDER BY seq")
    suspend fun all(): List<OutboxItem>

    @Insert
    suspend fun add(item: OutboxItem): Long

    @Query("DELETE FROM outbox WHERE seq = :seq")
    suspend fun remove(seq: Long)

    @Query("SELECT COUNT(*) FROM outbox")
    fun count(): Flow<Int>
}
