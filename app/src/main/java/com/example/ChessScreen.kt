package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.chess.ChessEngine
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Position
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(navController: NavController) {
    val engine = remember { ChessEngine() }
    var selectedPos by remember { mutableStateOf<Position?>(null) }
    var updateCounter by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var whiteTime by remember { mutableStateOf(600) } // 10 minutes
    var blackTime by remember { mutableStateOf(600) }
    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<PieceColor?>(null) }
    
    // Timer Coroutine
    LaunchedEffect(engine.currentTurn, isPaused, gameOver) {
        if (!isPaused && !gameOver) {
            while (true) {
                delay(1000L)
                if (engine.currentTurn == PieceColor.WHITE) {
                    whiteTime--
                    if (whiteTime <= 0) { gameOver = true; winner = PieceColor.BLACK }
                } else {
                    blackTime--
                    if (blackTime <= 0) { gameOver = true; winner = PieceColor.WHITE }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MECHESS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("by Ashu Mehta", fontSize = 10.sp, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { /* Sound */ }, modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.CircleShape).size(40.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "Sound", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { navController.navigate("settings") }, modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.CircleShape).size(40.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Default.Close /* using Close instead of unimported Settings */, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            val dummy = updateCounter
            // Opponent Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)) // Avatar Placeholder
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("GrandMaster_AI", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("LVL 8", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.background(MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small).padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Rating: 2450 • Blitz", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(formatTime(blackTime), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Board Container
            Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Eval Bar
                Box(modifier = Modifier.width(8.dp).fillMaxHeight(0.8f).background(MaterialTheme.colorScheme.outline, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.BottomCenter) {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f).background(Color.White, shape = androidx.compose.foundation.shape.CircleShape))
                }

                BoxWithConstraints(modifier = Modifier.weight(1f).aspectRatio(1f).background(MaterialTheme.colorScheme.outline).padding(2.dp)) {
            val squareSize = maxWidth / 8
            
            // Draw Board Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lightColor = com.example.ui.theme.BoardLightSq
                val darkColor = com.example.ui.theme.BoardDarkSq
                
                for (r in 0..7) {
                    for (c in 0..7) {
                        val color = if ((r + c) % 2 == 0) lightColor else darkColor
                        val isSelected = selectedPos?.row == r && selectedPos?.col == c
                        drawRect(
                            color = if (isSelected) Color(0x88FFFF00) else color,
                            topLeft = Offset(c * size.width / 8, r * size.height / 8),
                            size = Size(size.width / 8, size.height / 8)
                        )
                    }
                }
            }
            
            // Draw Pieces and Handle Clicks
            for (r in 0..7) {
                for (c in 0..7) {
                    val piece = engine.pieceAt(r, c)
                    Box(modifier = Modifier
                        .offset(x = squareSize * c, y = squareSize * r)
                        .size(squareSize)
                        .clickable(enabled = !isPaused && !gameOver) {
                            if (selectedPos != null) {
                                if (engine.move(selectedPos!!, Position(r, c))) {
                                    if (engine.isCheckmate()) {
                                        gameOver = true
                                        winner = engine.currentTurn
                                    }
                                    selectedPos = null
                                    updateCounter++ // trigger recomposition
                                } else {
                                    if (piece?.color == engine.currentTurn) {
                                        selectedPos = Position(r, c)
                                    } else {
                                        selectedPos = null
                                    }
                                }
                            } else {
                                if (piece?.color == engine.currentTurn) {
                                    selectedPos = Position(r, c)
                                }
                            }
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            Text(
                                text = getPieceSymbol(piece.type, piece.color),
                                fontSize = 32.sp,
                                color = if (piece.color == PieceColor.WHITE) com.example.ui.theme.BoardPieceWhite else com.example.ui.theme.BoardPieceBlack
                            )
                        }
                    }
                }
            } // closes for r
        } // closes BoxWithConstraints
    } // closes Row (Board Container)
            
    // Action Controls
            Spacer(modifier = Modifier.height(8.dp))
            // Move History
            Row(modifier = Modifier.fillMaxWidth().height(40.dp).background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val moves = engine.moveHistory.toList().chunked(2)
                    items(moves) { movePair ->
                        val whiteMove = movePair.getOrNull(0)
                        val blackMove = movePair.getOrNull(1)
                        val index = moves.indexOf(movePair)
                        val whiteText = whiteMove?.let { "${index+1}. ${getPieceChar(it.pieceMoved.type)}${colChar(it.from.col)}${8 - it.from.row}-${colChar(it.to.col)}${8 - it.to.row}" } ?: ""
                        val blackText = blackMove?.let { "${getPieceChar(it.pieceMoved.type)}${colChar(it.from.col)}${8 - it.from.row}-${colChar(it.to.col)}${8 - it.to.row}" } ?: ""
                        Text(whiteText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        if (blackText.isNotEmpty()) Text(blackText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Game Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { engine.undo(); updateCounter++ }, enabled = !isPaused && !gameOver && engine.moveHistory.isNotEmpty(), modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Undo", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = { gameOver = true; winner = if (engine.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE }, enabled = !gameOver, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text("Resign", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { gameOver = true; winner = null }, enabled = !gameOver, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text("Draw", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { isPaused = !isPaused }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(if (isPaused) "Play" else "Pause", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Player Bottom Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        Text("AM", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Ashu Mehta", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small).padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Ranked: Gold II • Elo: 1842", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small).padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(formatTime(whiteTime), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        } // End weight(1f) column

        // Bottom Navigation Bar
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "Game", tint = MaterialTheme.colorScheme.primary)
                Text("Game", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Analysis", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Analysis", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { navController.navigate("home") }) {
                Icon(androidx.compose.material.icons.Icons.Default.PlayArrow, contentDescription = "Ranking", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Ranking", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        // Winner Text overlay
        if (gameOver) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large).padding(24.dp)) {
                    Text(
                        "Game Over!\n${if (winner == PieceColor.WHITE) "White" else if (winner == PieceColor.BLACK) "Black" else "Draw"} Wins!",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = { /* trigger analysis */ }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text("View Post-Game Analysis")
                    }
                    Text("Tip: Your last knight move weakened the kingside. Try protecting the f2 square earlier.", fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

fun getPieceSymbol(type: PieceType, color: PieceColor): String {
    return when(color) {
        PieceColor.WHITE -> when(type) {
            PieceType.KING -> "♔"
            PieceType.QUEEN -> "♕"
            PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"
            PieceType.KNIGHT -> "♘"
            PieceType.PAWN -> "♙"
        }
        PieceColor.BLACK -> when(type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }
}

fun getPieceChar(type: PieceType): String {
    return when(type) {
        PieceType.KING -> "K"
        PieceType.QUEEN -> "Q"
        PieceType.ROOK -> "R"
        PieceType.BISHOP -> "B"
        PieceType.KNIGHT -> "N"
        PieceType.PAWN -> ""
    }
}

fun colChar(col: Int): Char {
    return ('a' + col)
}
