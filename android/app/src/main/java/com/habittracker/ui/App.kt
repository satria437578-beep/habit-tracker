package com.habittracker.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habittracker.data.Habit
import com.habittracker.reminder.ReminderScheduler

@Composable
fun AppRoot(vm: AppViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Habit?>(null) }
    var showEdit by remember { mutableStateOf(false) }

    val tabs = listOf(
        NavTab("Hari Ini", Icons.Filled.Home, "Rencanakan"),
        NavTab("Kalender", Icons.Filled.DateRange, "Rencanakan"),
        NavTab("Statistik", Icons.Filled.Star, "Kerjakan"),
        NavTab("Pomodoro", Icons.Filled.PlayArrow, "Kerjakan"),
        NavTab("Atur", Icons.Filled.Settings, "Lainnya"),
    )

    BoxWithConstraints {
        val expanded = maxWidth >= TabletBreakpoint
        HabitScaffold(
            tabs = tabs,
            selected = tab,
            onSelect = { tab = it },
            expanded = expanded,
            floatingActionButton = {
                if (tab == 0) {
                    FloatingActionButton(
                        onClick = { editing = null; showEdit = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah habit")
                    }
                }
            },
        ) { modifier ->
            when (tab) {
                0 -> TodayScreen(state, vm, modifier, expanded = expanded, onEdit = { editing = it; showEdit = true })
                1 -> CalendarScreen(state, vm, modifier, expanded = expanded)
                2 -> StatsScreen(state, modifier, expanded = expanded)
                3 -> PomodoroScreen(modifier)
                else -> SettingsScreen(state, vm, modifier)
            }
        }
    }

    if (showEdit) {
        HabitEditDialog(
            initial = editing,
            onDismiss = { showEdit = false },
            onSave = { vm.save(it); ReminderScheduler.schedule(context, it); showEdit = false },
            onDelete = {
                editing?.let { ReminderScheduler.cancel(context, it); vm.delete(it) }
                showEdit = false
            },
        )
    }
}
