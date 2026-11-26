package com.movementlab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Routine
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.SectionTitle
import com.movementlab.app.ui.StatBlock
import com.movementlab.app.ui.fmtDate

@Composable
fun HomeScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val level = data.settings.level
    val program = vm.allRoutines(data).filter { it.builtIn && it.level == level }
    val myRoutines = data.routines
    val lastSession = data.sessions.firstOrNull()

    // Exercises done recently where every set hit the target, and a harder step exists.
    val levelUps = data.sessions.take(10).flatMap { s -> s.entries.map { it.exerciseId } }.distinct()
        .mapNotNull { id -> vm.exercise(id, data) }
        .filter { vm.readyToProgress(it.id, data) }
        .mapNotNull { e -> vm.harder(e)?.let { e to it } }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MovementLab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${level?.label ?: ""} level", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.settings() }) { Icon(Icons.Filled.Settings, "Settings") }
            }
        }

        active?.let { w ->
            item {
                Card(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Workout in progress", fontWeight = FontWeight.Bold)
                        Text(w.routineName)
                        Button(onClick = { nav.workout() }, modifier = Modifier.padding(top = 8.dp)) { Text("Resume") }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBlock("${vm.workoutsThisWeek(data)}", "this week")
                    StatBlock("${vm.weekStreak(data)}", "week streak")
                    StatBlock("${data.sessions.size}", "total")
                }
            }
            lastSession?.let {
                Text("Last workout: ${it.routineName}, ${fmtDate(it.startedAt)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
            }
        }

        if (levelUps.isNotEmpty()) {
            item { SectionTitle("Ready to level up") }
            items(levelUps) { (from, to) ->
                Card(
                    onClick = { nav.exercise(to.id) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${from.name} → ${to.name}", fontWeight = FontWeight.Bold)
                        Text("You hit every target last time. Try the next step.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { SectionTitle("${level?.label ?: "Your"} program") }
        items(program) { r -> RoutineStartCard(r, vm, nav, active != null) }

        if (myRoutines.isNotEmpty()) {
            item { SectionTitle("My routines") }
            items(myRoutines) { r -> RoutineStartCard(r, vm, nav, active != null) }
        }

        item {
            OutlinedButton(
                onClick = {
                    if (active == null) vm.startEmptyWorkout()
                    nav.workout()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            ) { Text("Quick workout (pick exercises as you go)") }
        }
    }
}

@Composable
fun RoutineStartCard(r: Routine, vm: AppViewModel, nav: Nav, workoutActive: Boolean) {
    Card(
        onClick = { nav.editRoutine(r.id) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.Bold)
                Text(
                    r.items.mapNotNull { vm.exercise(it.exerciseId)?.name }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (!workoutActive) {
                IconButton(onClick = { vm.startWorkout(r); nav.workout() }) {
                    Icon(Icons.Filled.PlayArrow, "Start", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
