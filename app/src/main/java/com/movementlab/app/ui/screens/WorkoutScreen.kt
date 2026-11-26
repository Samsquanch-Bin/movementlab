package com.movementlab.app.ui.screens

import android.app.Activity
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.ActiveItem
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Exercise
import com.movementlab.app.data.Session
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.Stepper
import com.movementlab.app.ui.VideoPlayer
import com.movementlab.app.ui.displayToKg
import com.movementlab.app.ui.fmtDuration
import com.movementlab.app.ui.fmtNum
import com.movementlab.app.ui.fmtTarget
import com.movementlab.app.ui.fmtValue
import com.movementlab.app.ui.fmtWeight
import com.movementlab.app.ui.kgToDisplay
import com.movementlab.app.ui.openUrl
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WorkoutScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val restEndsAt by vm.restEndsAt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { now = System.currentTimeMillis(); delay(250) }
    }

    // Keep the screen awake while working out.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Buzz when rest is over.
    val restLeft = restEndsAt?.let { ((it - now) / 1000.0).let { s -> kotlin.math.ceil(s).toInt() } }
    LaunchedEffect(restEndsAt, restLeft != null && restLeft <= 0) {
        if (restLeft != null && restLeft <= 0) {
            runCatching {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Vibrator::class.java)
                v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
            }
            vm.skipRest()
        }
    }

    var showFinish by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<Session?>(null) }

    val w = active
    if (w == null) {
        // Workout was finished or discarded.
        val s = summary
        if (s != null) {
            WorkoutSummary(vm, s) { nav.home() }
        } else {
            LaunchedEffect(Unit) { nav.back() }
        }
        return
    }

    val item = w.items.getOrNull(w.index)
    val exercise = item?.let { vm.exercise(it.exerciseId, data) }
    val useLb = data.settings.useLb

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(w.routineName, maxLines = 1)
                        Text(fmtDuration(((now - w.startedAt) / 1000).toInt()), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { Button(onClick = { showFinish = true }, modifier = Modifier.padding(end = 8.dp)) { Text("Finish") } },
            )
        },
        bottomBar = {
            if (restLeft != null && restLeft > 0) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Rest", style = MaterialTheme.typography.labelMedium)
                            Text("%d:%02d".format(restLeft / 60, restLeft % 60), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { vm.adjustRest(-15) }) { Text("−15s") }
                        TextButton(onClick = { vm.adjustRest(15) }) { Text("+15s") }
                        Button(onClick = { vm.skipRest() }) { Text("Skip") }
                    }
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (w.items.isEmpty()) {
                Text("Add your first exercise to get started.", Modifier.padding(vertical = 24.dp))
            } else {
                val done = w.items.count { it.logged.size >= it.sets }
                LinearProgressIndicator(
                    progress = { done.toFloat() / w.items.size },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
                Text("Exercise ${w.index + 1} of ${w.items.size}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }

            if (item != null && exercise != null) {
                ExercisePanel(vm, w.index, item, exercise, useLb)
            } else if (item != null) {
                Text("This exercise no longer exists.", Modifier.padding(vertical = 16.dp))
                TextButton(onClick = { vm.removeExerciseFromWorkout(w.index) }) { Text("Remove it") }
            }

            if (w.items.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.goTo(w.index - 1) }, enabled = w.index > 0,
                        modifier = Modifier.weight(1f).height(56.dp)) { Text("← Previous") }
                    if (w.index < w.items.size - 1) {
                        Button(onClick = { vm.goTo(w.index + 1) }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Next →") }
                    } else {
                        Button(onClick = { showFinish = true }, modifier = Modifier.weight(1f).height(56.dp)) { Text("Finish") }
                    }
                }
                Text("Up next", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 16.dp))
                w.items.forEachIndexed { i, it ->
                    val name = vm.exercise(it.exerciseId, data)?.name ?: "?"
                    TextButton(onClick = { vm.goTo(i) }) {
                        Text(
                            "${if (it.logged.size >= it.sets) "✓" else "${i + 1}."} $name  ${it.logged.size}/${it.sets}",
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = if (i == w.index) FontWeight.Bold else FontWeight.Normal,
                            color = if (i == w.index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, null); Text("Add exercise")
                }
                if (item != null) {
                    OutlinedButton(onClick = { vm.removeExerciseFromWorkout(w.index) }, modifier = Modifier.weight(1f)) {
                        Text("Skip this one")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        ExercisePickerDialog(vm, onDismiss = { showPicker = false }) { e ->
            vm.addExerciseToWorkout(e.id)
            showPicker = false
        }
    }
    if (showFinish) {
        val logged = w.items.sumOf { it.logged.size }
        AlertDialog(
            onDismissRequest = { showFinish = false },
            title = { Text("Finish workout?") },
            text = { Text(if (logged == 0) "No sets logged yet, so nothing will be saved." else "$logged sets logged.") },
            confirmButton = {
                TextButton(onClick = {
                    showFinish = false
                    summary = vm.finishWorkout()
                }) { Text(if (logged == 0) "End" else "Save workout") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showFinish = false; vm.discardWorkout() }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showFinish = false }) { Text("Keep going") }
                }
            },
        )
    }
}

@Composable
private fun ExercisePanel(vm: AppViewModel, index: Int, item: ActiveItem, e: Exercise, useLb: Boolean) {
    val context = LocalContext.current
    val last = remember(e.id) { vm.lastLog(e.id) }
    val easier = vm.easier(e)
    val harder = vm.harder(e)
    val isHold = e.tracking == Tracking.SECONDS

    // Input defaults: what you just did, else what you did last time, else the target.
    var value by remember(index, e.id) {
        mutableStateOf(item.logged.lastOrNull()?.value ?: last?.sets?.firstOrNull()?.value ?: item.target)
    }
    var weightKg by remember(index, e.id) {
        mutableStateOf(item.logged.lastOrNull()?.weightKg ?: last?.sets?.firstOrNull()?.weightKg ?: 0.0)
    }
    var holdStart by remember(index, e.id) { mutableStateOf<Long?>(null) }
    var showVideo by remember(e.id) { mutableStateOf(false) }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(holdStart) {
        while (holdStart != null) { tick = System.currentTimeMillis(); delay(100) }
    }

    Column(Modifier.padding(top = 12.dp)) {
        Text(e.name, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Target: ${fmtTarget(item.sets, item.target, e.tracking)}", color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold)
        last?.let {
            Text(
                "Last time: " + it.sets.joinToString(", ") { s ->
                    fmtValue(s.value, e.tracking) + if (s.weightKg != 0.0) " (${fmtWeight(s.weightKg, useLb)})" else ""
                },
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (harder != null && vm.readyToProgress(e.id)) {
            Text("You hit every target last time. Try ${harder.name}?", color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showVideo = !showVideo }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlayArrow, null); Text(if (showVideo) "Hide" else "Demo")
            }
            OutlinedButton(onClick = { easier?.let { vm.swapExercise(index, it.id) } }, enabled = easier != null,
                modifier = Modifier.weight(1f)) { Text("Easier") }
            OutlinedButton(onClick = { harder?.let { vm.swapExercise(index, it.id) } }, enabled = harder != null,
                modifier = Modifier.weight(1f)) { Text("Harder") }
        }
        if (showVideo) {
            VideoPlayer(e.videoUrl, e.name, Modifier.padding(top = 8.dp))
        }

        // Sets done so far
        item.logged.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Set ${i + 1}", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    fmtValue(s.value, e.tracking) + (if (isHold) "" else " reps") + "  ·  " + fmtWeight(s.weightKg, useLb),
                    fontWeight = FontWeight.Bold,
                    color = if (s.value >= item.target) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = { vm.removeSet(index, i) }) { Icon(Icons.Filled.Close, "Remove set") }
            }
        }

        Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (isHold) {
                    val running = holdStart
                    val elapsed = running?.let { ((tick - it) / 1000).toInt() }
                    Button(
                        onClick = {
                            if (running == null) holdStart = System.currentTimeMillis()
                            else { value = ((System.currentTimeMillis() - running) / 1000).toInt().coerceAtLeast(1); holdStart = null }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = if (running != null) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        else ButtonDefaults.buttonColors(),
                    ) { Text(if (running == null) "Start stopwatch" else "Stop  ${elapsed}s", fontSize = 18.sp) }
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val step = if (isHold) 5 else 1
                    Stepper(if (isHold) "Seconds" else "Reps", "$value",
                        { value = (value - step).coerceAtLeast(0) }, { value += step })
                }
                Spacer(Modifier.height(8.dp))
                val wStep = if (useLb) 5.0 else 2.5
                val shown = kgToDisplay(weightKg, useLb)
                Stepper(
                    "Added weight (${if (useLb) "lb" else "kg"}) · minus = assisted",
                    if (shown == 0.0) "BW" else (if (shown > 0) "+" else "") + fmtNum(shown),
                    { weightKg = displayToKg(((shown - wStep) / wStep).roundToInt() * wStep, useLb) },
                    { weightKg = displayToKg(((shown + wStep) / wStep).roundToInt() * wStep, useLb) },
                )
                Button(
                    onClick = { vm.logSet(index, value, weightKg) },
                    enabled = value > 0,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(64.dp),
                ) {
                    Text("Log set ${item.logged.size + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun WorkoutSummary(vm: AppViewModel, s: Session, onDone: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val ups = s.entries.mapNotNull { log ->
        val e = vm.exercise(log.exerciseId, data) ?: return@mapNotNull null
        val next = vm.harder(e) ?: return@mapNotNull null
        if (log.target > 0 && log.sets.size >= 2 && log.sets.all { it.value >= log.target }) e to next else null
    }
    val totalSets = s.entries.sumOf { it.sets.size }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text("Workout saved 💪") },
        text = {
            Column {
                Text("${s.routineName} · ${fmtDuration(s.durationSec)} · $totalSets sets")
                if (ups.isNotEmpty()) {
                    Text("Ready to level up:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    ups.forEach { (from, to) -> Text("• ${from.name} → ${to.name}") }
                    Text("Swap these in next time with the Harder button, or edit your routine.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
    )
}
