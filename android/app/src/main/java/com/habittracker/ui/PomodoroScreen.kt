package com.habittracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val FocusChoices = listOf(15, 25, 30, 45, 50)
private val BreakChoices = listOf(5, 10, 15)

@Composable
fun PomodoroScreen(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    var focusMin by rememberSaveable { mutableIntStateOf(25) }
    var breakMin by rememberSaveable { mutableIntStateOf(5) }
    var isFocus by rememberSaveable { mutableStateOf(true) }
    var remaining by rememberSaveable { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable { mutableStateOf(false) }
    var sessions by rememberSaveable { mutableIntStateOf(0) }
    var focusTotal by rememberSaveable { mutableIntStateOf(0) }

    val total = (if (isFocus) focusMin else breakMin) * 60
    val accent = if (isFocus) cs.error else cs.secondary

    fun switchMode(focus: Boolean) {
        if (focus == isFocus) return
        running = false
        isFocus = focus
        remaining = (if (focus) focusMin else breakMin) * 60
    }

    fun pickFocus(min: Int) {
        focusMin = min
        if (isFocus && !running) remaining = min * 60
    }

    fun pickBreak(min: Int) {
        breakMin = min
        if (!isFocus && !running) remaining = min * 60
    }

    LaunchedEffect(running, isFocus, focusMin, breakMin) {
        if (!running) return@LaunchedEffect
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
        }
        if (remaining <= 0) {
            running = false
            if (isFocus) {
                sessions += 1
                focusTotal += focusMin
            }
            isFocus = !isFocus
            remaining = (if (isFocus) focusMin else breakMin) * 60
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("Pomodoro", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Fokus terarah dengan siklus kerja & istirahat",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isFocus) {
                            Button(onClick = { switchMode(true) }) { Text("Fokus") }
                        } else {
                            OutlinedButton(onClick = { switchMode(true) }) { Text("Fokus") }
                        }
                        if (!isFocus) {
                            Button(onClick = { switchMode(false) }) { Text("Istirahat") }
                        } else {
                            OutlinedButton(onClick = { switchMode(false) }) { Text("Istirahat") }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    ProgressRing(
                        progress = if (total == 0) 0f else (total - remaining).toFloat() / total,
                        size = 220.dp,
                        stroke = 14.dp,
                        color = accent,
                        track = accent.copy(alpha = 0.16f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                mmss(remaining),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                            )
                            Text(
                                if (isFocus) "Fokus" else "Istirahat",
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { running = !running }) {
                            Text(if (running) "Jeda" else if (remaining < total) "Lanjut" else "Mulai")
                        }
                        OutlinedButton(onClick = { running = false; remaining = total }) { Text("Reset") }
                        OutlinedButton(
                            onClick = {
                                running = false
                                isFocus = !isFocus
                                remaining = (if (isFocus) focusMin else breakMin) * 60
                            },
                        ) { Text("Lewati") }
                    }
                }
            }
        }
        item {
            StatRow(
                listOf(
                    StatSpec("Sesi selesai hari ini", sessions.toString(), accent),
                    StatSpec("Total waktu fokus", "$focusTotal menit", cs.primary),
                ),
                Modifier.fillMaxWidth(),
            )
        }
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionTitle("Durasi")
                    Spacer(Modifier.height(10.dp))
                    Text("Fokus (menit)", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FocusChoices.forEach { min ->
                            ChoicePill(min.toString(), focusMin == min) { pickFocus(min) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("Istirahat (menit)", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BreakChoices.forEach { min ->
                            ChoicePill(min.toString(), breakMin == min) { pickBreak(min) }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Selesaikan satu sesi fokus untuk menaikkan hitungan. Istirahat pendek otomatis dimulai setelah setiap sesi.",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    if (selected) {
        Button(onClick = onClick, contentPadding = padding) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = padding) { Text(text) }
    }
}

private fun mmss(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}
