package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    var soundEnabled by remember { mutableStateOf(true) }
    var animationsEnabled by remember { mutableStateOf(true) }
    var engineEnabled by remember { mutableStateOf(true) }
    var selectedTheme by remember { mutableStateOf("Classic") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(bottom = 32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Sound & Music Effects", modifier = Modifier.weight(1f))
            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Animations", modifier = Modifier.weight(1f))
            Switch(checked = animationsEnabled, onCheckedChange = { animationsEnabled = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Use Server-Side Analysis", modifier = Modifier.weight(1f))
            Switch(checked = engineEnabled, onCheckedChange = { engineEnabled = it })
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Board Theme", modifier = Modifier.align(Alignment.Start))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Classic", "Wood", "Dark").forEach { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { selectedTheme = theme },
                    label = { Text(theme) }
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "MECHESS", 
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
        Text(
            "by Ashu mehta", 
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Back")
        }
    }
}
