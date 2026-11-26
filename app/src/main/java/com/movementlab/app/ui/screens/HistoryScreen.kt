package com.movementlab.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Session
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.fmtDate
import com.movementlab.app.ui.fmtDuration
import com.movementlab.app.ui.fmtValue
import com.movementlab.app.ui.fmtWeight

@Composable
fun HistoryScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf<String?>(null) }
    var toDelete by remember { mutableStateOf<Session?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp))
            if (data.sessions.isEmpty()) {
                Text("No workouts yet. Finished workouts show up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(data.sessions, key = { it.id }) { s ->
            val open = expanded == s.id
            Card(
                onClick = { expanded = if (open) null else s.id },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(s.routineName, fontWeight = FontWeight.Bold)
                            Text(
                                "${fmtDate(s.startedAt)} · ${fmtDuration(s.durationSec)} · ${s.entries.sumOf { it.sets.size }} sets",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (open) IconButton(onClick = { toDelete = s }) { Icon(Icons.Filled.Delete, "Delete workout") }
                    }
                    if (open) {
                        s.entries.forEach { log ->
                            val e = vm.exercise(log.exerciseId, data)
                            val tracking = e?.tracking ?: Tracking.REPS
                            TextButton(onClick = { e?.let { nav.exercise(it.id) } }) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(e?.name ?: "Deleted exercise", fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        log.sets.joinToString(", ") {
                                            fmtValue(it.value, tracking) +
                                                if (it.weightKg != 0.0) " (${fmtWeight(it.weightKg, data.settings.useLb)})" else ""
                                        },
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this workout?") },
            text = { Text("${s.routineName}, ${fmtDate(s.startedAt)}. This can't be undone.") },
            confirmButton = { TextButton(onClick = { vm.deleteSession(s.id); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}
