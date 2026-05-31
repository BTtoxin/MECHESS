package com.example

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.navigation.NavController
import com.example.chess.ChessEngine
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.Position
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(navController: NavController, isSpectating: Boolean) {
    val haptics = LocalHapticFeedback.current
    var isFlipped by remember { mutableStateOf(false) }
    val engine = remember { ChessEngine() }
    var selectedPos by remember { mutableStateOf<Position?>(null) }
    var updateState by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var whiteTime by remember { mutableStateOf(600) } // 10 minutes
    var blackTime by remember { mutableStateOf(600) }
    var gameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<PieceColor?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // New Features: Settings
    var showSettings by remember { mutableStateOf(false) }
    var boardTheme by remember { mutableStateOf(0) } // 0: Default, 1: Wood, 2: Dark
    var timerDuration by remember { mutableStateOf(600) }
    var soundEnabled by remember { mutableStateOf(true) }
    val boardAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    
    val lastMove = engine.moveHistory.lastOrNull()
    val isCheck = engine.isKingInCheck(engine.currentTurn)

    // Checkmate Animation
    LaunchedEffect(gameOver) {
        if (gameOver) {
            boardAlpha.animateTo(0f, animationSpec = androidx.compose.animation.core.tween(1000))
        }
    }
    
    LaunchedEffect(isSpectating, isPaused, gameOver) {
        while (isSpectating && !isPaused && !gameOver) {
            delay(5000L)
            engine.getRandomMove()?.let { move ->
                engine.move(move.from, move.to)
                updateState++
            }
        }
    }
    LaunchedEffect(isPaused, gameOver) {
        if (!isPaused && !gameOver) {
            while (true) {
                delay(1000L)
                if (engine.currentTurn == PieceColor.WHITE) {
                    whiteTime--
                    if (whiteTime <= 0) { gameOver = true; winner = PieceColor.BLACK }
                } else if (engine.currentTurn == PieceColor.BLACK) {
                    blackTime--
                    if (blackTime <= 0) { gameOver = true; winner = PieceColor.WHITE }
                }
            }
        }
    }
    
    val capturedPieces = engine.moveHistory.mapNotNull { it.pieceCaptured }
    val whiteCaptured = capturedPieces.filter { it.color == PieceColor.BLACK }
    val blackCaptured = capturedPieces.filter { it.color == PieceColor.WHITE }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Logo",
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Column {
                            Text("MeChess", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Professional", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isFlipped = !isFlipped }) {
                        Icon(Icons.Default.Settings, contentDescription = "Flip Board") // Reuse settings icon for flip, or just add a better one
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).background(MaterialTheme.colorScheme.background)) {
            // Captured by White (Black pieces captured)
            Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Captured: ", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(whiteCaptured) { piece ->
                        Text(getPieceSymbol(piece.type, piece.color), fontSize = 16.sp)
                    }
                }
            }

            // Board Card
            if (isCheck && !gameOver) {
                Text("Check!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(4.dp).border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium), 
                elevation = CardDefaults.elevatedCardElevation(8.dp)
            ) {
                val currentPaused by rememberUpdatedState(isPaused)
                val currentGameOver by rememberUpdatedState(gameOver)
                val currentSpectating by rememberUpdatedState(isSpectating)

                BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.outline).padding(2.dp).graphicsLayer(alpha = boardAlpha.value).pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (!currentPaused && !currentGameOver && !currentSpectating) {
                            val squareSizeX = size.width / 8
                            val squareSizeY = size.height / 8
                            val c = if (isFlipped) 7 - (offset.x / squareSizeX).toInt() else (offset.x / squareSizeX).toInt()
                            val r = if (isFlipped) 7 - (offset.y / squareSizeY).toInt() else (offset.y / squareSizeY).toInt()
                            
                            if (r in 0..7 && c in 0..7) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val piece = engine.pieceAt(r, c)
                                if (selectedPos != null) {
                                    if (engine.move(selectedPos!!, Position(r, c))) {
                                        if (engine.isCheckmate()) {
                                            gameOver = true
                                            winner = engine.currentTurn
                                        }
                                        selectedPos = null
                                        updateState++
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
                            }
                        }
                    }
                }) {
                    val squareSize = maxWidth / 8
                    
                    // Draw Board Background
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val lightColor = com.example.ui.theme.BoardLightSq
                        val darkColor = com.example.ui.theme.BoardDarkSq
                        
                        for (r in 0..7) {
                            for (c in 0..7) {
                                val boardR = if (isFlipped) 7 - r else r
                                val boardC = if (isFlipped) 7 - c else c
                                val color = if ((r + c) % 2 == 0) lightColor else darkColor
                                val isSelected = selectedPos?.row == boardR && selectedPos?.col == boardC
                                val isLastMove = (lastMove?.from?.row == boardR && lastMove?.from?.col == boardC) || (lastMove?.to?.row == boardR && lastMove?.to?.col == boardC)
                                drawRect(
                                    color = if (isSelected) Color(0x88FFFF00) else if (isLastMove) Color(0x6600FF00) else color,
                                    topLeft = Offset(c * size.width / 8, r * size.height / 8),
                                    size = Size(size.width / 8, size.height / 8)
                                )
                            }
                        }
                    }
                    
                    // Draw Pieces
                    for (r in 0..7) {
                        for (c in 0..7) {
                            val boardR = if (isFlipped) 7 - r else r
                            val boardC = if (isFlipped) 7 - c else c
                            val piece = engine.pieceAt(boardR, boardC)
                            val isCheckKing = isCheck && piece?.type == PieceType.KING && piece.color == engine.currentTurn

                            if (piece != null) {
                                Box(
                                    modifier = Modifier
                                        .offset(x = squareSize * c, y = squareSize * r)
                                        .size(squareSize)
                                        .semantics {
                                            contentDescription = "${piece.type} ${piece.color} at ${'a'+boardC}${8-boardR}"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCheckKing) {
                                        Box(modifier = Modifier.size(squareSize * 0.8f).background(Color.Red.copy(alpha = 0.5f), shape = CircleShape))
                                    }
                                    Text(
                                        text = getPieceSymbol(piece.type, piece.color),
                                        fontSize = 32.sp,
                                        color = if (piece.color == PieceColor.WHITE) com.example.ui.theme.BoardPieceWhite else com.example.ui.theme.BoardPieceBlack
                                    )
                                }
                            }
                        }
                    }
                } // closes BoxWithConstraints
            } // closes ElevatedCard

            // Captured by Black (White pieces captured)
            Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Captured: ", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(blackCaptured) { piece ->
                        Text(getPieceSymbol(piece.type, piece.color), fontSize = 16.sp)
                    }
                }
            }                
            
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
                Button(onClick = { engine.undo(); updateState++ }, enabled = !isPaused && !gameOver && engine.moveHistory.isNotEmpty(), modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
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
                Button(onClick = { 
                    engine.getRandomMove()?.let { hint ->
                        selectedPos = hint.from
                    }
                }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)) {
                    Text("Hint", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = { /* Implement copy PGN */ }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    Text("Copy PGN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                Icon(Icons.Default.PlayArrow, contentDescription = "Game", tint = MaterialTheme.colorScheme.primary)
                Text("Game", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Analysis", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Analysis", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { navController.navigate("home") }) {
                Icon(Icons.Default.Home, contentDescription = "Ranking", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Button(onClick = { 
                        engine.reset()
                        gameOver = false
                        isAnalyzing = false
                        selectedPos = null
                        updateState++
                    }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text("New Game")
                    }
                    Button(onClick = { isAnalyzing = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("View Post-Game Analysis")
                    }
                    if (isAnalyzing) {
                        Text("Analysis: This engine is just for simple moves. In a real scenario, this would show heatmaps, move suggestions, and blunder detection.", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        
        // Settings Dialog
        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Game Settings") },
                text = {
                    Column {
                        Text("Timer Duration (mins)")
                        Slider(value = timerDuration.toFloat() / 60, onValueChange = { timerDuration = (it * 60).toInt() }, valueRange = 1f..30f)
                        Text("${timerDuration / 60} mins")
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sound Effects")
                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        whiteTime = timerDuration
                        blackTime = timerDuration
                        showSettings = false 
                    }) { Text("Apply") }
                }
            )
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

fun getPieceChar(type: com.example.chess.PieceType): String {
    return when(type) {
        com.example.chess.PieceType.KING -> "K"
        com.example.chess.PieceType.QUEEN -> "Q"
        com.example.chess.PieceType.ROOK -> "R"
        com.example.chess.PieceType.BISHOP -> "B"
        com.example.chess.PieceType.KNIGHT -> "N"
        com.example.chess.PieceType.PAWN -> ""
    }
}

// Function to generate PGN
fun generatePGN(moves: List<com.example.chess.Move>): String {
    val pgn = StringBuilder()
    moves.chunked(2).forEachIndexed { index, pair ->
        pgn.append("${index + 1}. ${formatMove(pair[0])} ")
        if (pair.size > 1) pgn.append("${formatMove(pair[1])} ")
    }
    return pgn.toString()
}

fun formatMove(move: com.example.chess.Move): String {
    val pieceType = when (move.pieceMoved.type) {
        com.example.chess.PieceType.PAWN -> ""
        com.example.chess.PieceType.KNIGHT -> "N"
        com.example.chess.PieceType.BISHOP -> "B"
        com.example.chess.PieceType.ROOK -> "R"
        com.example.chess.PieceType.QUEEN -> "Q"
        com.example.chess.PieceType.KING -> "K"
    }
    val capture = if (move.pieceCaptured != null) "x" else ""
    return "$pieceType${('a' + move.to.col)}${8 - move.to.row}"
}

fun colChar(col: Int): Char {
    return ('a' + col)
}
