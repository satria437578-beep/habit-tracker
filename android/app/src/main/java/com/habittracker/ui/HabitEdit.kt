package com.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habittracker.data.Habit
import java.util.UUID

private val DAYS = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

@Composable
fun HabitEditDialog(
    initial: Habit?,
    onDismiss: () -> Unit,
    onSave: (Habit) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category.orEmpty()) }
    var scheduleType by remember { mutableStateOf(initial?.scheduleType ?: "daily") }
    var days by remember {
        mutableStateOf(initial?.scheduleDays?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet())
    }
    var perWeek by remember { mutableStateOf((initial?.timesPerWeek ?: 3).coerceAtLeast(1)) }
    var targetType by remember { mutableStateOf(initial?.targetType ?: "check") }
    var targetValue by remember {
        val raw = initial?.targetValue ?: 0.0
        val shown = if (initial?.targetType == "timer") raw / 60.0 else raw
        mutableStateOf(if (shown == 0.0) "" else fmt(shown))
    }
    var unit by remember { mutableStateOf(initial?.unit.orEmpty()) }
    var reminder by remember { mutableStateOf(initial?.reminder.orEmpty()) }

    val tone = habitColorOf(category, name.ifBlank { "Habit" })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Habit baru" else "Edit habit") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LetterTile(name.ifBlank { "H" }, tone)
                    Column {
                        Text("Warna otomatis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (category.isBlank()) "Isi kategori agar warna bervariasi" else "Kategori: $category",
                            style = MaterialTheme.typography.labelSmall,
                            color = tone,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                OutlinedTextField(name, { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Kategori/tag") }, modifier = Modifier.fillMaxWidth())

                Text("Jadwal", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Option("Harian", scheduleType == "daily", MaterialTheme.colorScheme.primary) { scheduleType = "daily" }
                    Option("Hari tertentu", scheduleType == "weekdays", MaterialTheme.colorScheme.secondary) { scheduleType = "weekdays" }
                    Option("N/minggu", scheduleType == "weekly", MaterialTheme.colorScheme.tertiary) { scheduleType = "weekly" }
                }
                if (scheduleType == "weekdays") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DAYS.forEachIndexed { i, d ->
                            Option(d, (i + 1) in days, HabitPalette[i % HabitPalette.size]) {
                                days = if ((i + 1) in days) days - (i + 1) else days + (i + 1)
                            }
                        }
                    }
                }
                if (scheduleType == "weekly") {
                    Stepper("Target per minggu", perWeek) { perWeek = it.coerceIn(1, 7) }
                }

                Text("Tipe target", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Option("Centang", targetType == "check", MaterialTheme.colorScheme.primary) { targetType = "check" }
                    Option("Numerik", targetType == "numeric", MaterialTheme.colorScheme.secondary) { targetType = "numeric" }
                    Option("Timer", targetType == "timer", MaterialTheme.colorScheme.tertiary) { targetType = "timer" }
                }
                when (targetType) {
                    "numeric" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(targetValue, { targetValue = it }, label = { Text("Target") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(unit, { unit = it }, label = { Text("Satuan") }, modifier = Modifier.weight(1f))
                    }
                    "timer" -> OutlinedTextField(targetValue, { targetValue = it }, label = { Text("Menit") }, modifier = Modifier.fillMaxWidth())
                }

                OutlinedTextField(reminder, { reminder = it }, label = { Text("Pengingat (HH:mm, kosongkan = tidak)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val value = when (targetType) {
                        "timer" -> (targetValue.toDoubleOrNull() ?: 0.0) * 60
                        "numeric" -> targetValue.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }
                    onSave(
                        Habit(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            category = category.trim(),
                            scheduleType = scheduleType,
                            scheduleDays = if (scheduleType == "weekdays") days.sorted().joinToString(",") else "",
                            timesPerWeek = if (scheduleType == "weekly") perWeek else 0,
                            targetType = targetType,
                            targetValue = value,
                            unit = unit.trim(),
                            reminder = reminder.trim(),
                            archived = initial?.archived ?: false,
                            sortOrder = initial?.sortOrder ?: 0,
                            updatedAt = System.currentTimeMillis(),
                            dirty = true,
                        )
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = {
            Row {
                if (initial != null) TextButton(onClick = onDelete) { Text("Hapus") }
                TextButton(onClick = onDismiss) { Text("Batal") }
            }
        },
    )
}

@Composable
private fun Option(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.22f),
            selectedLabelColor = color,
        ),
    )
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        TextButton(onClick = { onChange(value - 1) }) { Text("-") }
        Text("$value", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
