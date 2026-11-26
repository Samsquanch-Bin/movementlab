package com.movementlab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.movementlab.app.data.Level
import com.movementlab.app.data.Routine
import com.movementlab.app.data.RoutineItem
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.SectionTitle
import com.movementlab.app.ui.fmtTarget

@Composable
fun RoutinesScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.editRoutine("new") },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New routine") },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            item {
                Text("Routines", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp))
            }
            if (data.routines.isNotEmpty()) {
                item { SectionTitle("My routines") }
                items(data.routines) { RoutineStartCard(it, vm, nav, active != null) }
            }
            Level.values().forEach { level ->
                val list = vm.allRoutines(data).filter { it.builtIn && it.level == level }
                if (list.isNotEmpty()) {
                    item { SectionTitle("${level.label} program") }
                    items(list) { RoutineStartCard(it, vm, nav, active != null) }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
fun RoutineEditorScreen(vm: AppViewModel, nav: Nav, routineId: String) {
    val data by vm.data.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val original = remember(routineId) {
        vm.routine(routineId) ?: Routine("", "", data.settings.level, "", emptyList())
    }
    var draft by remember(routineId) {
        mutableStateOf(if (original.builtIn) original.copy(name = original.name + " (my copy)") else original)
    }
    var dirty by remember { mutableStateOf(routineId == "new") }
    var showPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun edit(f: (Routine) -> Routine) { draft = f(draft); dirty = true }
    fun editItem(i: Int, f: (RoutineItem) -> RoutineItem) = edit { r -> r.copy(items = r.items.mapIndexed { j, it -> if (j == i) f(it) else it }) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (routineId == "new") "New routine" else if (original.builtIn) original.name else "Edit routine") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (!original.builtIn && routineId != "new") {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete routine") }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            if (original.builtIn) {
                item {
                    Text(original.description, style = MaterialTheme.typography.bodyMedium)
                    Text("This is a built-in routine. Any change you save becomes your own copy.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val toStart = if (dirty) vm.saveRoutine(draft).also { draft = it; dirty = false } else original
                            vm.startWorkout(toStart)
                            nav.workout()
                        },
                        enabled = active == null && draft.items.isNotEmpty() && draft.name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) { Text(if (active == null) "Start workout" else "Workout in progress") }
                    if (dirty) {
                        OutlinedButton(
                            onClick = { draft = vm.saveRoutine(draft); dirty = false; nav.back() },
                            enabled = draft.name.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) { Text("Save") }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> edit { it.copy(name = v) } },
                    label = { Text("Routine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { SectionTitle("Exercises") }
            itemsIndexed(draft.items) { i, item ->
                val e = vm.exercise(item.exerciseId, data)
                val tracking = e?.tracking ?: Tracking.REPS
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                e?.name ?: "Unknown exercise",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                if (i > 0) edit { r -> r.copy(items = r.items.toMutableList().also { l -> l.add(i - 1, l.removeAt(i)) }) }
                            }) { Icon(Icons.Filled.KeyboardArrowUp, "Move up") }
                            IconButton(onClick = {
                                if (i < draft.items.size - 1) edit { r -> r.copy(items = r.items.toMutableList().also { l -> l.add(i + 1, l.removeAt(i)) }) }
                            }) { Icon(Icons.Filled.KeyboardArrowDown, "Move down") }
                            IconButton(onClick = { edit { r -> r.copy(items = r.items.filterIndexed { j, _ -> j != i }) } }) {
                                Icon(Icons.Filled.Delete, "Remove")
                            }
                        }
                        Text(fmtTarget(item.sets, item.target, tracking) + " · rest ${item.restSec}s",
                            color = MaterialTheme.colorScheme.primary)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            MiniStepper("Sets", "${item.sets}",
                                { editItem(i) { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } },
                                { editItem(i) { it.copy(sets = (it.sets + 1).coerceAtMost(10)) } })
                            val step = if (tracking == Tracking.SECONDS) 5 else 1
                            MiniStepper(if (tracking == Tracking.SECONDS) "Seconds" else "Reps", "${item.target}",
                                { editItem(i) { it.copy(target = (it.target - step).coerceAtLeast(1)) } },
                                { editItem(i) { it.copy(target = it.target + step) } })
                            MiniStepper("Rest", "${item.restSec}s",
                                { editItem(i) { it.copy(restSec = (it.restSec - 15).coerceAtLeast(0)) } },
                                { editItem(i) { it.copy(restSec = it.restSec + 15) } })
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Icon(Icons.Filled.Add, null)
                    Text("Add exercise")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(vm, onDismiss = { showPicker = false }) { e ->
            val target = if (e.tracking == Tracking.SECONDS) 30 else 10
            edit { it.copy(items = it.items + RoutineItem(e.id, 3, target, data.settings.defaultRestSec)) }
            showPicker = false
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete routine?") },
            text = { Text("Your workout history stays. Only the routine is removed.") },
            confirmButton = { TextButton(onClick = { vm.deleteRoutine(original.id); confirmDelete = false; nav.back() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MiniStepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(value, fontWeight = FontWeight.Bold)
            TextButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}
