package com.movementlab.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Seed
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.LineChart
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.SectionTitle
import com.movementlab.app.ui.StatBlock
import com.movementlab.app.ui.VideoPlayer
import com.movementlab.app.ui.fmtDateShort
import com.movementlab.app.ui.fmtValue
import com.movementlab.app.ui.fmtWeight
import com.movementlab.app.ui.kgToDisplay
import com.movementlab.app.ui.openUrl

@Composable
fun ExerciseDetailScreen(vm: AppViewModel, nav: Nav, exerciseId: String) {
    val data by vm.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val e = vm.exercise(exerciseId, data)
    var editVideo by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(e?.name ?: "Exercise") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (e?.custom == true) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                },
            )
        },
    ) { pad ->
        if (e == null) {
            Text("Exercise not found.", Modifier.padding(pad).padding(16.dp))
            return@Scaffold
        }
        val useLb = data.settings.useLb
        val points = vm.progress(e.id, data)
        val ladder = vm.ladderOf(e, data)
        val next = vm.harder(e)

        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                listOf(e.category, e.level.label, e.equipment.takeIf { it.isNotBlank() }).filterNotNull().joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            VideoPlayer(e.videoUrl, e.name, Modifier.padding(top = 12.dp))
            TextButton(onClick = { editVideo = true }) {
                Icon(Icons.Filled.Edit, null)
                Text(" Use a different video")
            }

            if (next != null && vm.readyToProgress(e.id, data)) {
                Card(
                    onClick = { nav.exercise(next.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Text("You hit every target last time. Ready for ${next.name}?",
                        Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            if (e.cues.isNotEmpty()) {
                SectionTitle("Form cues")
                e.cues.forEach { Text("•  $it", Modifier.padding(bottom = 4.dp)) }
            }

            if (ladder.size > 1) {
                SectionTitle("Progression")
                ladder.forEachIndexed { i, step ->
                    val current = step.id == e.id
                    TextButton(onClick = { if (!current) nav.exercise(step.id) }) {
                        Text(
                            "${i + 1}. ${step.name}" + if (current) "  ← you are here" else "",
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                            color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text(
                    "Move up a step when you can hit every set at the target for two sessions in a row.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionTitle("Your progress")
            if (points.isEmpty()) {
                Text("No sets logged yet. Add this exercise to a routine or a quick workout.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val best = points.maxOf { it.bestValue }
                val bestW = points.maxOf { it.bestWeightKg }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBlock(fmtValue(best, e.tracking), if (e.tracking == Tracking.SECONDS) "longest hold" else "best set")
                    if (bestW > 0) StatBlock(fmtWeight(bestW, useLb), "heaviest")
                    StatBlock("${points.size}", "sessions")
                }
                Text(if (e.tracking == Tracking.SECONDS) "Longest hold per session" else "Best set per session",
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                LineChart(points.map { it.bestValue.toFloat() })
                if (bestW > 0) {
                    Text("Added weight per session", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                    LineChart(points.map { kgToDisplay(it.bestWeightKg, useLb).toFloat() }, color = MaterialTheme.colorScheme.secondary)
                }
                SectionTitle("Recent sessions")
                data.sessions.take(30).mapNotNull { s -> s.entries.firstOrNull { it.exerciseId == e.id }?.let { s to it } }
                    .take(8).forEach { (s, log) ->
                        Text(
                            "${fmtDateShort(s.startedAt)}:  " + log.sets.joinToString(", ") { set ->
                                fmtValue(set.value, e.tracking) + if (set.weightKg != 0.0) " (${fmtWeight(set.weightKg, useLb)})" else ""
                            },
                            Modifier.padding(bottom = 4.dp),
                        )
                    }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (editVideo && e != null) {
        var url by remember { mutableStateOf(e.videoUrl) }
        AlertDialog(
            onDismissRequest = { editVideo = false },
            title = { Text("Demo video link") },
            text = {
                Column {
                    Text("In YouTube, tap Share → Copy link on any video, then paste it here. It will play inside the app.",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(url, { url = it }, singleLine = true, modifier = Modifier.padding(top = 8.dp))
                    TextButton(onClick = { openUrl(context, Seed.youtubeSearch(e.name)) }) { Text("Search YouTube for ${e.name}") }
                    if (!e.custom) {
                        TextButton(onClick = { url = Seed.defaultVideo(e.id, e.name) }) { Text("Reset to the built-in video") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.setVideo(e.id, url.trim()); editVideo = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editVideo = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete && e != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${e.name}?") },
            text = { Text("It will be removed from your routines. Logged history stays in past workouts.") },
            confirmButton = { TextButton(onClick = { vm.deleteCustomExercise(e.id); confirmDelete = false; nav.back() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
