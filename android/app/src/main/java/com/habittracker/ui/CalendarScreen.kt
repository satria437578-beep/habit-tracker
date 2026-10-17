package com.habittracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habittracker.data.Stats
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_INIT = listOf("S", "S", "R", "K", "J", "S", "M")
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("id", "ID"))
private val dayFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

@Composable
fun CalendarScreen(state: UiState, vm: AppViewModel, modifier: Modifier = Modifier, expanded: Boolean = false) {
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (expanded) {
            item {
                TwoPane(
                    expanded = true,
                    leading = { CalendarCard(state, vm) },
                    trailing = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            MetricsRow(state)
                            SectionTitle("Performa minggu ini")
                            WeekRings(state, vm)
                            SectionTitle("Tren 6 minggu")
                            StreakTimeline(state)
                        }
                    },
                )
            }
        } else {
            item { CalendarCard(state, vm) }
            item { MetricsRow(state) }
            item { SectionTitle("Performa minggu ini") }
            item { WeekRings(state, vm) }
            item { SectionTitle("Tren 6 minggu") }
            item { StreakTimeline(state) }
        }
        item { SectionTitle("Habit · ${state.selected.format(dayFmt)}") }
        if (state.selectedHabits.isEmpty()) {
            item { LoadingHint("Tidak ada habit terjadwal hari ini.") }
        }
        items(state.selectedHabits, key = { it.habit.id }) { ui ->
            DayHabitRow(ui, state, vm)
        }
    }
}

@Composable
private fun MetricsRow(state: UiState) {
    StatRow(
        listOf(
            StatSpec("Minggu ini", "${(state.weeklyRate * 100).toInt()}%", MaterialTheme.colorScheme.secondary),
            StatSpec("Bulan ini", "${(state.monthRate * 100).toInt()}%", MaterialTheme.colorScheme.primary),
            StatSpec("Total", "${state.monthDone}", MaterialTheme.colorScheme.tertiary),
        ),
    )
}

@Composable
private fun CalendarCard(state: UiState, vm: AppViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.month.format(monthFmt).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { vm.goToday() }) { Text("Hari ini") }
                IconButton(onClick = { vm.prevMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "bulan sebelumnya") }
                IconButton(onClick = { vm.nextMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "bulan berikutnya") }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                DAY_INIT.forEachIndexed { i, d ->
                    Text(
                        d,
                        Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = HabitPalette[i % HabitPalette.size],
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            state.monthGrid.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day -> DayCell(day, state, vm, Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Rendah", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                repeat(5) { i ->
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(progressColor(i / 4f)))
                }
                Text("Tinggi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayCell(day: LocalDate, state: UiState, vm: AppViewModel, modifier: Modifier) {
    val inMonth = YearMonth.from(day) == state.month
    val ratio = state.ratioOf(day)
    val isSelected = day == state.selected
    val isToday = day == state.date
    val weekdayTone = habitColor(DAY_INIT[day.dayOfWeek.value - 1] + day.dayOfWeek.value)
    val circleBg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val numberColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dot = when {
        ratio == null -> Color.Transparent
        ratio <= 0f -> MaterialTheme.colorScheme.surfaceVariant
        else -> weekdayTone.copy(alpha = (0.45f + 0.55f * ratio).coerceIn(0f, 1f))
    }

    Column(
        modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).clickable { vm.selectDay(day) },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(circleBg)
                .then(
                    if (isToday && !isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(day.dayOfMonth.toString(), color = numberColor, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
    }
}

@Composable
private fun WeekRings(state: UiState, vm: AppViewModel) {
    val start = Stats.weekRange(state.selected).first
    val days = (0..6).map { start.plusDays(it.toLong()) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp)) {
            days.forEach { day ->
                val ratio = state.ratioOf(day)
                val isSelected = day == state.selected
                val tone = habitColor(DAY_INIT[day.dayOfWeek.value - 1] + day.dayOfWeek.value)
                Column(
                    Modifier.weight(1f).clickable { vm.selectDay(day) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressRing(
                        progress = ratio ?: 0f,
                        size = 46.dp,
                        stroke = 5.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else tone,
                        track = tone.copy(alpha = 0.16f),
                    ) {
                        Text(
                            day.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        DAY_INIT[day.dayOfWeek.value - 1],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakTimeline(state: UiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rekor terpanjang", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text("${state.topStreak} hari", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.weekRatios.forEach { (weekStart, ratio) ->
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(ratio.coerceAtLeast(0.06f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (ratio > 0f) progressColor(ratio)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${weekStart.dayOfMonth}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHabitRow(ui: HabitUi, state: UiState, vm: AppViewModel) {
    val habit = ui.habit
    val accent = habitColorOf(habit.category, habit.name)
    val valueText = when (habit.targetType) {
        "numeric" -> "${fmt(ui.today?.value ?: 0.0)}/${fmt(habit.targetValue)} ${habit.unit}".trim()
        "timer" -> "${(ui.today?.duration ?: 0) / 60}m/${habit.targetValue.toInt() / 60}m"
        else -> "Streak ${ui.streak.current}"
    }
    TintedCard(accent = accent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LetterTile(habit.name, accent, size = 40.dp)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(valueText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!ui.today?.note.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(ui.today?.note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ProgressRing(
                progress = if (habit.targetType == "check") (if (ui.done) 1f else 0f) else ui.progress,
                size = 44.dp,
                color = accent,
                track = accent.copy(alpha = 0.16f),
                modifier = Modifier.clickable { vm.tapOn(habit, state.selected) },
            ) {
                if (ui.done) {
                    Icon(Icons.Filled.Check, "selesai", tint = accent, modifier = Modifier.size(22.dp))
                } else {
                    Icon(
                        if (habit.targetType == "numeric") Icons.Filled.Add else Icons.Filled.Check,
                        "check-in",
                        tint = accent.copy(alpha = 0.45f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
