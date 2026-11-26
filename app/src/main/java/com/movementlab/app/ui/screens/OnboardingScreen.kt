package com.movementlab.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.data.Level

@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    var level by remember { mutableStateOf<Level?>(null) }
    var useLb by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("MovementLab", fontSize = 34.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("Calisthenics routines, demo videos and progress tracking.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text("Where are you starting?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Level.values().forEach { l ->
            val selected = level == l
            Card(
                onClick = { level = l },
                modifier = Modifier.fillMaxWidth(),
                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(l.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(l.blurb, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Text("You can change this any time in Settings.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Weight units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !useLb, onClick = { useLb = false }, label = { Text("kg") })
            FilterChip(selected = useLb, onClick = { useLb = true }, label = { Text("lb") })
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                level?.let {
                    vm.setUseLb(useLb)
                    vm.setLevel(it)
                    onDone()
                }
            },
            enabled = level != null,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Let's go", fontSize = 18.sp) }
    }
}
