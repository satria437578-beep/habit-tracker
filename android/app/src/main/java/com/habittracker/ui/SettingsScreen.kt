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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.habittracker.HabitApp
import com.habittracker.export.CsvExporter
import com.habittracker.net.ConnState

@Composable
fun SettingsScreen(state: UiState, vm: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = HabitApp.repo.store
    var url by remember { mutableStateOf(store.serverUrl) }

    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("Pengaturan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Sinkronisasi & arsip", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sinkronisasi server (WebSocket)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(url, { url = it }, label = { Text("ws://host:port") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { store.serverUrl = url.trim(); vm.reconnect() }) { Text("Simpan & sambung") }
                        OutlinedButton(onClick = { vm.syncNow() }) { Text("Sinkron") }
                    }
                    val (status, color) = when (state.connection) {
                        ConnState.ONLINE -> "Tersambung" to MaterialTheme.colorScheme.secondary
                        ConnState.CONNECTING -> "Menyambung..." to MaterialTheme.colorScheme.tertiary
                        ConnState.OFFLINE -> "Offline" to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Dot(color, 8.dp)
                        Text("Status: $status · Antrean: ${state.pending}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Button(onClick = { CsvExporter.export(context, state) }, modifier = Modifier.fillMaxWidth()) {
                Text("Ekspor CSV & bagikan")
            }
        }

        item { SectionTitle("Arsip", MaterialTheme.colorScheme.tertiary) }
        if (state.archived.isEmpty()) {
            item { LoadingHint("Belum ada habit diarsipkan.") }
        }
        items(state.archived, key = { it.id }) { habit ->
            val accent = habitColorOf(habit.category, habit.name)
            TintedCard(accent = accent, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LetterTile(habit.name, accent, size = 36.dp)
                    Text(habit.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { vm.archive(habit, false) }) { Text("Aktifkan") }
                    OutlinedButton(onClick = { vm.delete(habit) }) { Text("Hapus") }
                }
            }
        }
    }
}
