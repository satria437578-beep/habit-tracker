package com.habittracker.data

import org.json.JSONObject

fun Habit.toJson(withId: Boolean = true): JSONObject = JSONObject().apply {
    if (withId) put("id", id)
    put("name", name)
    put("category", category)
    put("scheduleType", scheduleType)
    put("scheduleDays", scheduleDays)
    put("timesPerWeek", timesPerWeek)
    put("targetType", targetType)
    put("targetValue", targetValue)
    put("unit", unit)
    put("reminder", reminder)
    put("archived", archived)
    put("sortOrder", sortOrder)
}

fun Checkin.toJson(): JSONObject = JSONObject().apply {
    put("habitId", habitId)
    put("date", date)
    put("value", value)
    put("duration", duration)
    put("note", note)
}

fun Freeze.toJson(): JSONObject = JSONObject().apply {
    put("habitId", habitId)
    put("date", date)
}

fun JSONObject.toHabit(): Habit = Habit(
    id = getString("id"),
    name = optString("name"),
    category = optString("category"),
    scheduleType = optString("schedule_type", "daily"),
    scheduleDays = optString("schedule_days", ""),
    timesPerWeek = optInt("times_per_week", 0),
    targetType = optString("target_type", "check"),
    targetValue = optDouble("target_value", 0.0),
    unit = optString("unit"),
    reminder = optString("reminder"),
    archived = optInt("archived", 0) == 1,
    sortOrder = optInt("sort_order", 0),
    updatedAt = optLong("updated_at", 0),
    dirty = false,
)

fun JSONObject.toCheckin(): Checkin = Checkin(
    id = getString("id"),
    habitId = getString("habit_id"),
    date = getString("date"),
    value = optDouble("value", 1.0),
    duration = optInt("duration", 0),
    note = optString("note"),
    updatedAt = optLong("updated_at", 0),
    dirty = false,
)

fun JSONObject.toFreeze(): Freeze = Freeze(
    id = getString("id"),
    habitId = getString("habit_id"),
    date = getString("date"),
    updatedAt = optLong("updated_at", 0),
    dirty = false,
)

fun deleted(json: JSONObject): Boolean = json.optInt("deleted", 0) == 1
