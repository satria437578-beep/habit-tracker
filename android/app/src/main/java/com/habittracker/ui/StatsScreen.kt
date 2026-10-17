package com.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habittracker.data.Habit
import com.habittracker.data.Stats
import com.habittracker.data.StreakCalculator
import java.time.LocalDate

private val DAY_NAMES = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

@Composable
fun StatsScreen(state: UiState, modifier: Modifier = Modifier, expanded: Boolean = false) {
    var selected by remember { mutableStateOf<String?>(null) }
    val habits = state.today.map { it.habit } + state.archived

    val heat = if (selected == null) {
        state.heatmap
    } else {
        val habit = habits.firstOrNull { it.id == selected }
        if (habit == null) state.heatmap
        else state.checkinsOf(habit.id)
            .filter { StreakCalculator.isComplete(habit, it) }
            .groupingBy { LocalDate.parse(it.date) }
            .eachCount()
    }

    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Statistik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Ringkasan kebiasaanmu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            StatRow(
                listOf(
                    StatSpec("Minggu ini", "${(state.weeklyRate * 100).toInt()}%", MaterialTheme.colorScheme.secondary),
                    StatSpec("Bulan ini", "${(state.monthlyRate * 100).toInt()}%", MaterialTheme.colorScheme.primary),
                ),
            )
        }
        item {
            StatRow(
                listOf(
                    StatSpec("Hari terbaik", state.bestWeekday?.let { DAY_NAMES[it - 1] } ?: "-", MaterialTheme.colorScheme.tertiary),
                    StatSpec("Rekor", "${state.topStreak} hari", HabitPalette[7]),
                ),
            )
        }

        if (expanded) {
            item {
                TwoPane(
                    expanded = true,
                    leading = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Tren 14 hari")
                            TrendCard(state)
                        }
                    },
                    trailing = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Heatmap kalender")
                            HeatFilter(habits, selected) { selected = it }
                            HeatmapCard(heat)
                        }
                    },
                )
            }
        } else {
            item { SectionTitle("Tren 14 hari") }
            item { TrendCard(state) }
            item { SectionTitle("Heatmap kalender") }
            item { HeatFilter(habits, selected) { selected = it } }
            item { HeatmapCard(heat) }
        }

        item { SectionTitle("Per habit") }
        if (habits.isEmpty()) {
            item { LoadingHint("Belum ada habit.") }
        } else if (expanded) {
            items(habits.chunked(2)) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { habit ->
                        HabitStatCard(habit, state, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            items(habits, key = { it.id }) { habit ->
                HabitStatCard(habit, state, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TrendCard(state: UiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) { BarChart(state.trend) }
    }
}

@Composable
private fun HeatmapCard(heat: Map<LocalDate, Int>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) { Heatmap(heat) }
    }
}

@Composable
private fun HeatFilter(habits: List<Habit>, selected: String?, onSelect: (String?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("Semua") },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
        )
        habits.take(3).forEach { h ->
            FilterChip(
                selected = selected == h.id,
                onClick = { onSelect(h.id) },
                label = { Text(h.name.take(10)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = habitColorOf(h.category, h.name).copy(alpha = 0.22f),
                ),
            )
        }
    }
}

@Composable
private fun HabitStatCard(habit: Habit, state: UiState, modifier: Modifier) {
    val accent = habitColorOf(habit.category, habit.name)
    val checks = state.checkinsOf(habit.id)
    val streak = StreakCalculator.compute(habit, checks, state.freezesOf(habit.id))
    val rate = Stats.habitRate(habit, checks, state.date.minusDays(29), state.date) ?: 0f
    TintedCard(accent = accent, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterTile(habit.name, accent, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Streak ${streak.current} · Rekor ${streak.longest} · 30 hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProgressRing(
                progress = rate,
                size = 46.dp,
                stroke = 5.dp,
                color = accent,
                track = accent.copy(alpha = 0.16f),
            ) {
                Text("${(rate * 100).toInt()}", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            }
        }
        val badges = Stats.badges(streak.longest)
        if (badges.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                badges.forEach { Tag("$it hari", accent) }
            }
        }
    }
}
