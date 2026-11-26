package com.movementlab.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Level
import com.movementlab.app.ui.Nav
import com.movementlab.app.ui.SectionTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(vm: AppViewModel, nav: Nav) {
    val data by vm.data.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) "Backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingImport = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }.getOrNull()
            if (pendingImport == null) Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Level")
            Text("Sets which program shows on the Home screen.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Level.values().forEach { l ->
                    FilterChip(selected = data.settings.level == l, onClick = { vm.setLevel(l) }, label = { Text(l.label) })
                }
            }

            SectionTitle("Weight units")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !data.settings.useLb, onClick = { vm.setUseLb(false) }, label = { Text("kg") })
                FilterChip(selected = data.settings.useLb, onClick = { vm.setUseLb(true) }, label = { Text("lb") })
            }

            SectionTitle("Default rest timer")
            Text("Used for exercises added during a quick workout.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { s ->
                    FilterChip(selected = data.settings.defaultRestSec == s, onClick = { vm.setDefaultRest(s) },
                        label = { Text(if (s % 60 == 0) "${s / 60} min" else "${s}s") })
                }
            }

            SectionTitle("Backup")
            Text("Save all workouts, routines and stats to a file, e.g. before switching phones.",
                style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = {
                    val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportLauncher.launch("movementlab-backup-$stamp.json")
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) { Text("Export backup") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restore from backup") }

            SectionTitle("About")
            Text("MovementLab ${runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""}")
            Text("Demo videos open YouTube. Tap the pencil on any exercise to set your own link.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces everything currently in the app with the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    val ok = vm.importJson(text)
                    Toast.makeText(context, if (ok) "Backup restored" else "That file isn't a MovementLab backup", Toast.LENGTH_LONG).show()
                    pendingImport = null
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
}
