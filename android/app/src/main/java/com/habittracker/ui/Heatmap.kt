package com.habittracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields

private val iso = WeekFields.ISO

@Composable
fun Heatmap(data: Map<LocalDate, Int>, modifier: Modifier = Modifier, weeks: Int = 20) {
    val today = LocalDate.now()
    val end = today.with(iso.dayOfWeek(), DayOfWeek.SUNDAY.value.toLong())
    val start = end.minusDays((weeks * 7 - 1).toLong())
    val max = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    val empty = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier) {
        Text("Aktivitas $weeks minggu", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(124.dp)) {
            val gap = 3.dp.toPx()
            val cell = (size.width - gap * (weeks - 1)) / weeks
            var day = start
            while (!day.isAfter(end)) {
                val col = ((day.toEpochDay() - start.toEpochDay()) / 7).toInt()
                val row = day.dayOfWeek.value - 1
                val count = data[day] ?: 0
                val level = if (count == 0) 0f else (0.35f + 0.65f * (count.toFloat() / max)).coerceAtMost(1f)
                val color = if (level == 0f) empty else heatColor(level)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(col * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(cell * 0.22f),
                )
                day = day.plusDays(1)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sedikit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            (0..4).forEach { i ->
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(HeatRamp[i]))
            }
            Text("Banyak", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
