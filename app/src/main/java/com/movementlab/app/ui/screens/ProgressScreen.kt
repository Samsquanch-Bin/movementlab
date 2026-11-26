package com.movementlab.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Tracking
import com.movementlab.app.ui.LineChart
import com.movementlab.app.ui.SectionTitle
import com.movementlab.app.ui.StatBlock
import com.movementlab.app.ui.displayToKg
import com.movementlab.app.ui.fmtDateShort
import com.movementlab.app.ui.fmtNum
import com.movementlab.app.ui.fmtValue
import com.movementlab.app.ui.fmtWeight
import com.movementlab.app.ui.kgToDisplay
import java.util.Calendar

@Composable
fun ProgressScreen(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val useLb = data.settings.useLb
    val unit = if (useLb) "lb" else "kg"
    var weightInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp))

        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBlock("${vm.workoutsThisWeek(data)}", "this week")
                StatBlock("${vm.weekStreak(data)}", "week streak")
                StatBlock("${data.sessions.sumOf { s -> s.entries.sumOf { it.sets.size } }}", "total sets")
            }
        }

        // ---- 12-week calendar heatmap (Mon-Sun rows... drawn as columns of weeks)
        SectionTitle("Last 12 weeks")
        val days = data.sessions.groupingBy { AppViewModel.dayKey(it.startedAt) }.eachCount()
        val cal = Calendar.getInstance()
        val todayBack = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -todayBack - 7 * 11) // Monday 11 weeks ago
        val start = cal.timeInMillis
        val empty = MaterialTheme.colorScheme.surfaceContainerHighest
        val filled = MaterialTheme.colorScheme.primary
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (week in 0 until 12) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (dow in 0 until 7) {
                        val c = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DAY_OF_YEAR, week * 7 + dow) }
                        val future = c.timeInMillis > System.currentTimeMillis()
                        val count = days[AppViewModel.dayKey(c.timeInMillis)] ?: 0
                        Box(
                            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(3.dp))
                                .background(if (future) MaterialTheme.colorScheme.surface else if (count > 0) filled else empty),
                        )
                    }
                }
            }
        }
        Text("Each square is a day, each column a week (Mon at top).", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

        // ---- Personal records
        SectionTitle("Personal records")
        val records = data.sessions.flatMap { it.entries }.groupBy { it.exerciseId }
            .mapNotNull { (id, logs) ->
                val e = vm.exercise(id, data) ?: return@mapNotNull null
                val sets = logs.flatMap { it.sets }
                if (sets.isEmpty()) null else Triple(e, sets.maxOf { it.value }, sets.maxOf { it.weightKg })
            }.sortedBy { it.first.name }
        if (records.isEmpty()) {
            Text("Log some workouts to see your bests here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        records.forEach { (e, best, heaviest) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(e.name, Modifier.weight(1f))
                Text(
                    fmtValue(best, e.tracking) + (if (e.tracking == Tracking.REPS) " reps" else "") +
                        if (heaviest > 0) "  ·  ${fmtWeight(heaviest, useLb)}" else "",
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // ---- Body weight
        SectionTitle("Body weight")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Today ($unit)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    weightInput.toDoubleOrNull()?.takeIf { it > 0 }?.let { vm.addBodyWeight(displayToKg(it, useLb)) }
                    weightInput = ""
                },
                enabled = (weightInput.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.padding(start = 8.dp),
            ) { Text("Add") }
        }
        if (data.body.size >= 2) {
            LineChart(data.body.map { kgToDisplay(it.weightKg, useLb).toFloat() }, Modifier.padding(top = 12.dp))
        }
        data.body.reversed().take(10).forEach { b ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fmtDateShort(b.date), Modifier.weight(1f))
                Text("${fmtNum(kgToDisplay(b.weightKg, useLb))} $unit", fontWeight = FontWeight.Bold)
                IconButton(onClick = { vm.deleteBodyEntry(b.date) }) { Icon(Icons.Filled.Close, "Delete entry") }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
