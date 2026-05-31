package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun HomeScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("chess_prefs", android.content.Context.MODE_PRIVATE) }
    var elo by remember { mutableStateOf(sharedPreferences.getInt("elo", 1200)) }
    var gamesPlayed by remember { mutableStateOf(sharedPreferences.getInt("gamesPlayed", 0)) }
    var wins by remember { mutableStateOf(sharedPreferences.getInt("wins", 0)) }

    val alphaAnim = remember { Animatable(0f) }
    val slideAnim = remember { Animatable(50f) }
    
    LaunchedEffect(Unit) {
        elo = sharedPreferences.getInt("elo", 1200)
        gamesPlayed = sharedPreferences.getInt("gamesPlayed", 0)
        wins = sharedPreferences.getInt("wins", 0)
        
        launch {
            alphaAnim.animateTo(1f, tween(800, easing = LinearOutSlowInEasing))
        }
        launch {
            slideAnim.animateTo(0f, tween(800, easing = LinearOutSlowInEasing))
        }
    }

    val rank = when {
        elo >= 2000 -> "Grandmaster"
        elo >= 1600 -> "Master"
        elo >= 1200 -> "Intermediate"
        else -> "Beginner"
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = { navController.navigate("game/puzzles") }, icon = { Icon(Icons.Default.Star, contentDescription = "Puzzles") }, label = { Text("Puzzles") })
                NavigationBarItem(selected = false, onClick = { navController.navigate("game/pvc") }, icon = { Icon(Icons.Default.Person, contentDescription = "Play AI") }, label = { Text("Play AI") })
                NavigationBarItem(selected = false, onClick = { navController.navigate("game/spectate") }, icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Watch") }, label = { Text("Watch") })
                NavigationBarItem(selected = false, onClick = { navController.navigate("settings") }, icon = { Icon(Icons.Default.Menu, contentDescription = "More") }, label = { Text("More") })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).graphicsLayer {
                alpha = alphaAnim.value
                translationY = slideAnim.value
            }.verticalScroll(rememberScrollState())
        ) {
            // User Header
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Good evening, Ashu Mehta", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
            }

            // Stats top row
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Blitz", "$elo", Modifier.weight(1f))
                StatCard("Rapid", "1500", Modifier.weight(1f))
                StatCard("Puzzle", "1200", Modifier.weight(1f))
                StatCard("Streak", "$wins", Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Quick pairing", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            
            // Grid of buttons
            val formats = listOf("1+0" to "Bullet", "2+1" to "Bullet", "3+0" to "Blitz", "3+2" to "Blitz", "5+0" to "Blitz", "5+3" to "Blitz", "10+0" to "Rapid", "10+5" to "Rapid", "15+10" to "Rapid")
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(260.dp)
            ) {
                items(formats.size) { index ->
                    val (time, type) = formats[index]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium,
                        onClick = { navController.navigate("game/pvc") }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                            Text(time, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(type, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Text("Game History", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GameHistoryCard("Win", "vs Stockfish LVL 5", "Rapid • 10+0", "+12 Elo", MaterialTheme.colorScheme.primary)
                GameHistoryCard("Loss", "vs Stockfish LVL 8", "Blitz • 5+0", "-8 Elo", MaterialTheme.colorScheme.error)
                GameHistoryCard("Draw", "vs Local Player", "Bullet • 1+0", "+0 Elo", MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                 Button(onClick = { navController.navigate("game/pvp") }, modifier = Modifier.weight(1f).height(48.dp), shape = MaterialTheme.shapes.medium) {
                     Text("Play Offline 2P")
                 }
                 if (com.example.chess.ChessGameManager.savedEngine != null && !com.example.chess.ChessGameManager.savedEngine!!.isCheckmate()) {
                     Button(onClick = { navController.navigate("game/resume") }, modifier = Modifier.weight(1f).height(48.dp), shape = MaterialTheme.shapes.medium) {
                         Text("Resume Match")
                     }
                 }
            }
        }
    }
}

@Composable
fun GameHistoryCard(result: String, opponent: String, format: String, eloChange: String, resultColor: androidx.compose.ui.graphics.Color) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(result, fontWeight = FontWeight.Bold, color = resultColor, fontSize = 16.sp)
                Text(opponent, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(eloChange, fontWeight = FontWeight.Bold, color = if (eloChange.startsWith("+")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text(format, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FallingPiecesBackground() {
    val piecesLine = listOf("♔", "♕", "♖", "♗", "♘", "♙", "♚", "♛", "♜", "♝", "♞", "♟")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight
        for (i in 0..15) {
            val randomX = remember { (0..100).random() / 100f }
            val animDuration = remember { (5000..10000).random() }
            val delayStart = remember { (0..5000).random() }
            val pieceChar = remember { piecesLine.random() }
            
            val infiniteTransition = rememberInfiniteTransition()
            val yPos by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 1000f, // Use a large value since constraints value might be small if not drawn
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = animDuration, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = StartOffset(delayStart)
                )
            )
            
            Text(
                text = pieceChar,
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.offset(x = width * randomX, y = yPos.dp)
            )
        }
    }
}
