package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text("MECHESS", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
            Text("by Ashu Mehta", fontSize = 12.sp, modifier = Modifier.padding(bottom = 32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 4.sp)

            
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your Rank: Grandmaster", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Current Elo: 2450", color = MaterialTheme.colorScheme.onSurface)
                    Text("Games Played: 142 | Won: 100", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { navController.navigate("game") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp)) {
                Text("Play Local Multiplayer", fontWeight = FontWeight.Bold)
            }
            Button(onClick = { navController.navigate("game") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)) {
                Text("Play vs AI (Beginner)", fontWeight = FontWeight.Bold)
            }
            Button(onClick = { navController.navigate("game/true") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)) {
                Text("Spectate Live Game (AI Demo)", fontWeight = FontWeight.Bold)
            }
            Button(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)) {
                Text("Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}
