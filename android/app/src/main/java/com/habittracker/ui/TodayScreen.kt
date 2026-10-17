package com.habittracker.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habittracker.data.Habit
import com.habittracker.net.ConnState
import com.habittracker.reminder.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

@Composable
fun TodayScreen(
    state: UiState,
    vm: AppViewModel,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onEdit: (Habit) -> Unit,
) {
    var noteHabit by remember { mutableStateOf<Habit?>(null) }
    val done = state.today.count { it.done }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (expanded) 2 else 1),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TodayBanner(state, done, state.today.size, onSync = { vm.syncNow() })
        }
        if (state.today.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle("Habit hari ini")
            }
        }
        if (state.today.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                CenteredEmpty("Belum ada habit hari ini.\nTekan + untuk menambah.")
            }
        }
        items(state.today, key = { it.habit.id }) { ui ->
            HabitCard(ui = ui, onEdit = { onEdit(ui.habit) }, onNote = { noteHabit = ui.habit }, vm = vm)
        }
    }

    noteHabit?.let { habit ->
        val existing = state.today.firstOrNull { it.habit.id == habit.id }?.today?.note.orEmpty()
        NoteDialog(
            initial = existing,
            onDismiss = { noteHabit = null },
            onSave = { vm.setNote(habit, state.date.toString(), it); noteHabit = null },
        )
    }
}

@Composable
private fun TodayBanner(state: UiState, done: Int, total: Int, onSync: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val gradient = Brush.linearGradient(listOf(cs.primary, Color(0xFF7B4DFF), cs.tertiary))
    val progress = if (total == 0) 0f else done.toFloat() / total
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(Modifier.background(gradient).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(greeting(), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        state.date.format(dateFmt).replaceFirstChar { it.uppercase() },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(10.dp))
                    ConnectionPill(state.connection, state.pending, onSync)
                }
                Spacer(Modifier.size(10.dp))
                ProgressRing(
                    progress = progress,
                    size = 78.dp,
                    stroke = 8.dp,
                    color = Color.White,
                    track = Color.White.copy(alpha = 0.28f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$done/$total", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("selesai", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionPill(connection: ConnState, pending: Int, onSync: () -> Unit) {
    val (text, dot) = when (connection) {
        ConnState.ONLINE -> "Tersambung" to Color(0xFF6DF0B6)
        ConnState.CONNECTING -> "Menyambung..." to Color(0xFFFFD27A)
        ConnState.OFFLINE -> "Offline" to Color(0xFFFF9E9E)
    }
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onSync).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(999.dp)).background(dot))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
        if (pending > 0) Text("· $pending antrean", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun HabitCard(ui: HabitUi, onEdit: () -> Unit, onNote: () -> Unit, vm: AppViewModel) {
    val habit = ui.habit
    var menu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val accent = habitColorOf(habit.category, habit.name)

    TintedCard(accent = accent, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LetterTile(habit.name, accent)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (habit.category.isNotBlank()) Tag(habit.category, accent)
                    Tag(scheduleLabel(habit))
                    if (ui.streak.current > 0) Tag("Streak ${ui.streak.current}", MaterialTheme.colorScheme.tertiary)
                }
            }
            RingButton(ui, vm, accent, onStartTimer = { TimerStarter.start(context, habit) })
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "menu") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Catatan") }, onClick = { menu = false; onNote() })
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onEdit() })
                    DropdownMenuItem(
                        text = { Text("Arsipkan") },
                        onClick = { menu = false; ReminderScheduler.cancel(context, habit); vm.archive(habit, true) },
                    )
                    when (habit.targetType) {
                        "numeric" -> DropdownMenuItem(
                            text = { Text("Reset nilai") },
                            onClick = { menu = false; vm.setValue(habit, LocalDate.now().toString(), 0.0) },
                        )
                        "timer" -> DropdownMenuItem(
                            text = { Text("Mulai timer") },
                            onClick = { menu = false; TimerStarter.start(context, habit) },
                        )
                        else -> DropdownMenuItem(
                            text = { Text("Tandai hari libur") },
                            onClick = { menu = false; vm.setFreeze(habit.id, LocalDate.now().toString(), true) },
                        )
                    }
                }
            }
        }
        if (habit.targetType != "check") {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LinearProgressIndicator(
                    progress = { ui.progress },
                    modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = accent.copy(alpha = 0.16f),
                )
                Text(progressLabel(ui, habit), style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }
        if (!ui.today?.note.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(ui.today?.note.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RingButton(ui: HabitUi, vm: AppViewModel, accent: Color, onStartTimer: () -> Unit) {
    val habit = ui.habit
    val tint = if (ui.done) accent else accent.copy(alpha = 0.85f)
    ProgressRing(
        progress = if (habit.targetType == "check") (if (ui.done) 1f else 0f) else ui.progress,
        size = 48.dp,
        stroke = 5.dp,
        color = tint,
        track = accent.copy(alpha = 0.16f),
        modifier = Modifier.clickable {
            if (habit.targetType == "timer" && !ui.done) onStartTimer() else vm.tap(habit)
        },
    ) {
        when {
            habit.targetType == "timer" && !ui.done -> Icon(Icons.Filled.PlayArrow, "mulai timer", tint = tint, modifier = Modifier.size(22.dp))
            ui.done -> Icon(Icons.Filled.Check, "undo", tint = tint, modifier = Modifier.size(22.dp))
            habit.targetType == "numeric" -> Text("+", style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Bold)
            else -> Icon(Icons.Filled.Check, "check-in", tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}

private fun progressLabel(ui: HabitUi, habit: Habit): String = when (habit.targetType) {
    "numeric" -> "${fmt(ui.today?.value ?: 0.0)}/${fmt(habit.targetValue)} ${habit.unit}".trim()
    "timer" -> "${(ui.today?.duration ?: 0) / 60}m/${habit.targetValue.toInt() / 60}m"
    else -> ""
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> "Selamat pagi"
        hour < 15 -> "Selamat siang"
        hour < 19 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan harian") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

private fun scheduleLabel(habit: Habit): String = when (habit.scheduleType) {
    "weekdays" -> {
        val names = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val days = habit.scheduleDays.split(',').mapNotNull { it.trim().toIntOrNull()?.minus(1) }.filter { it in 0..6 }
        if (days.isEmpty()) "Hari tertentu" else days.joinToString(",") { names[it] }
    }
    "weekly" -> "${habit.timesPerWeek}x per minggu"
    else -> "Harian"
}

object TimerStarter {
    fun start(context: Context, habit: Habit) {
        com.habittracker.timer.TimerService.start(context, habit.id, habit.name, habit.targetValue.toInt().coerceAtLeast(60))
    }
}
