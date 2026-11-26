package com.movementlab.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Exercise
import com.movementlab.app.data.Level
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.Nav

val categories = listOf("All", "Push", "Pull", "Legs", "Core", "Skill", "Mobility", "Custom")

private fun List<Exercise>.filtered(query: String, category: String, level: Level?): List<Exercise> =
    filter { e ->
        (query.isBlank() || e.name.contains(query.trim(), ignoreCase = true)) &&
            (category == "All" || (category == "Custom" && e.custom) || e.category == category) &&
            (level == null || e.level == level)
    }.sortedWith(compareBy<Exercise>({ categories.indexOf(it.category).let { i -> if (i < 0) 99 else i } }, { it.ladder ?: "~" }, { it.step }))

@Composable
private fun FilterBar(
    query: String, onQuery: (String) -> Unit,
    category: String, onCategory: (String) -> Unit,
    level: Level?, onLevel: (Level?) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            label = { Text("Search exercises") }, modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { c -> FilterChip(selected = category == c, onClick = { onCategory(c) }, label = { Text(c) }) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = level == null, onClick = { onLevel(null) }, label = { Text("Any level") })
            Level.values().forEach { l -> FilterChip(selected = level == l, onClick = { onLevel(l) }, label = { Text(l.label) }) }
        }
    }
}

@Composable
private fun ExerciseRow(e: Exercise, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(e.name, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Text(
                listOf(e.category, e.level.label, e.equipment.takeIf { it.isNotBlank() && it != "None" })
                    .filterNotNull().joinToString(" · "),
            )
        },
        trailingContent = { if (e.tracking == Tracking.SECONDS) Text("hold", color = MaterialTheme.colorScheme.secondary) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun LibraryScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var level by remember { mutableStateOf<Level?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    val list = vm.allExercises(data).filtered(query, category, level)

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Custom") })
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Exercises", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp))
                    FilterBar(query, { query = it }, category, { category = it }, level, { level = it })
                }
            }
            items(list, key = { it.id }) { e ->
                ExerciseRow(e) { nav.exercise(e.id) }
                HorizontalDivider()
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }

    if (showAdd) {
        AddExerciseDialog(onDismiss = { showAdd = false }) { name, cat, tracking, url ->
            val e = vm.addCustomExercise(name, cat, tracking, url, emptyList())
            showAdd = false
            nav.exercise(e.id)
        }
    }
}

@Composable
fun ExercisePickerDialog(vm: AppViewModel, onDismiss: () -> Unit, onPick: (Exercise) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var level by remember { mutableStateOf<Level?>(null) }
    val list = vm.allExercises(data).filtered(query, category, level)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add exercise") },
        text = {
            Column {
                FilterBar(query, { query = it }, category, { category = it }, level, { level = it })
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list, key = { it.id }) { e -> ExerciseRow(e) { onPick(e) } }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun AddExerciseDialog(onDismiss: () -> Unit, onAdd: (String, String, Tracking, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Push") }
    var tracking by remember { mutableStateOf(Tracking.REPS) }
    var url by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.drop(1).dropLast(1).forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = tracking == Tracking.REPS, onClick = { tracking = Tracking.REPS }, label = { Text("Reps") })
                    FilterChip(selected = tracking == Tracking.SECONDS, onClick = { tracking = Tracking.SECONDS }, label = { Text("Timed hold") })
                }
                OutlinedTextField(url, { url = it }, label = { Text("Demo video link (optional)") }, singleLine = true)
                Text("Leave blank to search YouTube for the name.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, category, tracking, url.trim()) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
