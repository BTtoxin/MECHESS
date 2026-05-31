package com.example

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(navController: NavController, mode: String) {
    val haptics = LocalHapticFeedback.current
    var isFlipped by remember { mutableStateOf(false) }
    
    // Resume facility
    val engine = remember { 
        if (mode == "resume" && com.example.chess.ChessGameManager.savedEngine != null) {
            com.example.chess.ChessGameManager.savedEngine!!
        } else {
            val newEngine = ChessEngine()
            com.example.chess.ChessGameManager.savedEngine = newEngine
            newEngine
        }
    }
    
    val isSpectating = mode == "spectate"
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Audio Focus and Tone for sounds
    val toneGenerator = remember { android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100) }
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }
    
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
    var boardTheme by remember { mutableStateOf("Default") } // Default, Classic, Coral
    var timerDuration by remember { mutableStateOf(600) }
    var soundEnabled by remember { mutableStateOf(true) }
    var isBlindfold by remember { mutableStateOf(false) } // Add Blindfold Mode
    val boardAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val shakeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    
    val lastMove = engine.moveHistory.lastOrNull()
    val isCheck = remember(updateState) { engine.isKingInCheck(engine.currentTurn) }
    val validMovesForSelected = remember(selectedPos, updateState) {
        if (selectedPos != null) engine.getValidMovesFor(selectedPos!!.row, selectedPos!!.col) else emptyList()
    }

    var difficulty by remember { mutableStateOf(com.example.chess.Difficulty.MEDIUM) }
    var gameMode by remember { mutableStateOf(if (mode == "pvp") "PVP" else "PVC") } // "PVC" or "PVP"
    var isSpectatingMode by remember { mutableStateOf(isSpectating) }

    // Checkmate Animation
    LaunchedEffect(gameOver, gameMode) {
        if (gameOver) {
            boardAlpha.animateTo(0.6f, animationSpec = androidx.compose.animation.core.tween(1000))
            
            // Update ELO
            if (!isSpectating && gameMode == "PVC") {
                val prefs = context.getSharedPreferences("chess_prefs", android.content.Context.MODE_PRIVATE)
                val currentGames = prefs.getInt("gamesPlayed", 0)
                val currentWins = prefs.getInt("wins", 0)
                val currentElo = prefs.getInt("elo", 1200)

                val newGames = currentGames + 1
                var newWins = currentWins
                var newElo = currentElo

                // Assume Player is White
                if (winner == PieceColor.WHITE) { 
                    newWins += 1
                    newElo += 25
                } else if (winner == PieceColor.BLACK) {
                    newElo -= 15
                }

                prefs.edit().putInt("gamesPlayed", newGames).putInt("wins", newWins).putInt("elo", newElo).apply()
            }
        } else {
            boardAlpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(500))
        }
    }
    
    LaunchedEffect(isSpectatingMode, isPaused, gameOver, gameMode) {
        while (true) {
            if (!isPaused && !gameOver && gameMode == "PVC" && (engine.currentTurn == PieceColor.BLACK || isSpectatingMode)) {
                delay(500L) // Faster AI
                val aiMove = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    engine.getAIMove(difficulty)
                }
                aiMove?.let { move ->
                    engine.move(move.from, move.to)
                    val wasCapture = engine.moveHistory.lastOrNull()?.pieceCaptured != null
                    if (soundEnabled) {
                        toneGenerator.startTone(if (wasCapture) android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT else android.media.ToneGenerator.TONE_PROP_BEEP, 50)
                    }
                }
                updateState++
                if (engine.isCheckmate()) {
                    gameOver = true
                    winner = if (engine.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                } else if (engine.isDraw()) {
                    gameOver = true
                    winner = null
                }
            } else {
                delay(500L)
            }
        }
    }
    LaunchedEffect(isPaused, gameOver, gameMode) {
        if (!isPaused && !gameOver && gameMode == "PVC") {
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
    
    // Simple list of pieces that updates only when necessary
    val allPieces = remember(updateState) {
        val pieces = mutableListOf<Triple<Int, Int, com.example.chess.Piece>>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = engine.pieceAt(r, c)
                if (piece != null) {
                    pieces.add(Triple(r, c, piece))
                }
            }
        }
        pieces
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 4.dp, vertical = 8.dp).background(MaterialTheme.colorScheme.background)) {
            // Opponent Info
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondary, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        Text("AI", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Computer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            Text(difficulty.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.background(MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small).padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Stockfish 16.1", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TimerComponent(blackTime)
            }

            // Captured by White (Black pieces captured)
            Row(modifier = Modifier.fillMaxWidth().height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Captured: ", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(whiteCaptured) { piece ->
                        Text(getPieceSymbol(piece.type, piece.color), fontSize = 16.sp)
                    }
                }
            }

            // Board Area with Eval Bar
            if (isCheck && !gameOver) {
                Text("Check!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            Row(modifier = Modifier.fillMaxWidth().aspectRatio(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Evaluation Bar
                val evalScore = remember(updateState) { engine.evaluateBoard() }
                // clamp evalScore between -900 and +900 roughly for mapping
                val evalPercent = ((evalScore.toFloat() / 900f).coerceIn(-1f, 1f) + 1f) / 2f
                val barFill by androidx.compose.animation.core.animateFloatAsState(evalPercent, animationSpec = androidx.compose.animation.core.tween(500))
                
                Box(modifier = Modifier.width(16.dp).fillMaxHeight().clip(MaterialTheme.shapes.small).background(Color.Black).border(1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small)) {
                    val fillHeight = if (isFlipped) (1f - barFill) else barFill
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(fillHeight).background(Color.White).align(Alignment.BottomCenter))
                }

                Row(modifier = Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    
                    // Evaluation Bar
                    val evalScore by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (engine.currentTurn == PieceColor.WHITE) 0.5f else 0.4f, // Dummy logic for eval
                        animationSpec = androidx.compose.animation.core.tween(500)
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(12.dp).background(Color.DarkGray)) {
                        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(evalScore).background(Color.White).align(Alignment.BottomCenter))
                    }
                    
                    ElevatedCard(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(4.dp).graphicsLayer(translationX = shakeOffset.value).border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.medium), 
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
                                        val wasCapture = engine.moveHistory.lastOrNull()?.pieceCaptured != null
                                        if (soundEnabled) {
                                            toneGenerator.startTone(if (wasCapture) android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT else android.media.ToneGenerator.TONE_PROP_BEEP, 50)
                                        }
                                        if (engine.isCheckmate()) {
                                            gameOver = true
                                            winner = if (engine.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                                        } else if (engine.isDraw()) {
                                            gameOver = true
                                            winner = null
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
                        val lightColor = when (boardTheme) {
                            "Classic" -> Color(0xFFF0D9B5)
                            "Coral" -> Color(0xFFB0E0E6)
                            else -> com.example.ui.theme.BoardLightSq
                        }
                        val darkColor = when (boardTheme) {
                            "Classic" -> Color(0xFFB58863)
                            "Coral" -> Color(0xFFE2A1A8)
                            else -> com.example.ui.theme.BoardDarkSq
                        }
                        
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
                                
                                val isValidTarget = validMovesForSelected.any { it.row == boardR && it.col == boardC }
                                if (isValidTarget) {
                                    val isCapture = allPieces.any { it.first == boardR && it.second == boardC }
                                    if (isCapture) {
                                        // Draw ring for capture
                                        drawCircle(
                                            color = Color.Red.copy(alpha = 0.5f),
                                            radius = size.width / 16f,
                                            center = Offset(c * size.width / 8 + size.width / 16, r * size.height / 8 + size.height / 16),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
                                        )
                                    } else {
                                        // Draw inner dot for empty square
                                        drawCircle(
                                            color = Color.Black.copy(alpha = 0.2f),
                                            radius = size.width / 40f,
                                            center = Offset(c * size.width / 8 + size.width / 16, r * size.height / 8 + size.height / 16)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Board Notation
                    for (i in 0..7) {
                        val boardC = if (isFlipped) 7 - i else i
                        val fileChar = if (isFlipped) ('h' - i) else ('a' + i)
                        val rankLabel = if (isFlipped) (i + 1).toString() else (8 - i).toString()
                        
                        // Rank Notation (left side)
                        Text(
                            text = rankLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i % 2 == 0) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.offset(x = 2.dp, y = (squareSize * i) + 2.dp)
                        )
                        
                        // File Notation (bottom side)
                        Text(
                            text = fileChar.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (i % 2 == 1) Color.DarkGray else Color.LightGray,
                            modifier = Modifier.offset(x = (squareSize * i) + squareSize - 10.dp, y = (squareSize * 7) + squareSize - 16.dp)
                        )
                    }

                    // Draw Pieces
                    for ((r, c, piece) in allPieces) {
                        key(piece.id) {
                            val boardR = if (isFlipped) 7 - r else r
                            val boardC = if (isFlipped) 7 - c else c
                            val isCheckKing = isCheck && piece.type == PieceType.KING && piece.color == engine.currentTurn

                            // Adjust drawing position based on flipped board
                            val drawR = if (isFlipped) 7 - r else r
                            val drawC = if (isFlipped) 7 - c else c
                            
                            val animX by androidx.compose.animation.core.animateDpAsState(squareSize * drawC, animationSpec = androidx.compose.animation.core.tween(300))
                            val animY by androidx.compose.animation.core.animateDpAsState(squareSize * drawR, animationSpec = androidx.compose.animation.core.tween(300))

                            var dragOffset by remember { mutableStateOf(Offset.Zero) }
                            var isDragging by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .offset {
                                        if (isDragging) {
                                            androidx.compose.ui.unit.IntOffset(animX.roundToPx() + dragOffset.x.toInt(), animY.roundToPx() + dragOffset.y.toInt())
                                        } else {
                                            androidx.compose.ui.unit.IntOffset(animX.roundToPx(), animY.roundToPx())
                                        }
                                    }
                                    .size(squareSize)
                                    .pointerInput(piece.id) {
                                        detectDragGestures(
                                            onDragStart = { 
                                                if (!currentPaused && !currentGameOver && !currentSpectating) {
                                                    isDragging = true 
                                                    selectedPos = Position(r, c)
                                                }
                                            },
                                            onDragEnd = {
                                                if (isDragging) {
                                                    isDragging = false
                                                    // Simplest way: use the raw drop position relative to the board
                                                    val dropX = dragOffset.x + (c * squareSize.toPx())
                                                    val dropY = dragOffset.y + (r * squareSize.toPx())
                                                    // Adding half square size to find the center of the dropped piece
                                                    val targetC = ((dropX + (squareSize.toPx() / 2)) / squareSize.toPx()).toInt()
                                                    val targetR = ((dropY + (squareSize.toPx() / 2)) / squareSize.toPx()).toInt()
                                                    
                                                    val finalC = if (isFlipped) 7 - targetC else targetC
                                                    val finalR = if (isFlipped) 7 - targetR else targetR
                                                    
                                                    if (finalR in 0..7 && finalC in 0..7 && selectedPos != null) {
                                                        if (engine.move(selectedPos!!, Position(finalR, finalC))) {
                                                            val wasCapture = engine.moveHistory.lastOrNull()?.pieceCaptured != null
                                                            if (soundEnabled) {
                                                                toneGenerator.startTone(if (wasCapture) android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT else android.media.ToneGenerator.TONE_PROP_BEEP, 50)
                                                            }
                                                            if (engine.isCheckmate()) {
                                                                gameOver = true
                                                                winner = if (engine.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                                                                scope.launch {
                                                                    repeat(5) { 
                                                                        shakeOffset.animateTo(10f, androidx.compose.animation.core.tween(50))
                                                                        shakeOffset.animateTo(-10f, androidx.compose.animation.core.tween(50))
                                                                    }
                                                                    shakeOffset.animateTo(0f, androidx.compose.animation.core.tween(50))
                                                                }
                                                            } else if (engine.isDraw()) {
                                                                gameOver = true
                                                                winner = null
                                                            }
                                                            selectedPos = null
                                                            updateState++
                                                        } else {
                                                            selectedPos = null // reset drag if invalid
                                                        }
                                                    } else {
                                                        selectedPos = null
                                                    }
                                                    dragOffset = Offset.Zero
                                                }
                                            },
                                            onDragCancel = {
                                                isDragging = false
                                                dragOffset = Offset.Zero
                                                selectedPos = null
                                            },
                                            onDrag = { change, dragAmount -> 
                                                if (isDragging) {
                                                    change.consume()
                                                    dragOffset += dragAmount
                                                }
                                            }
                                        )
                                    }
                                    .semantics {
                                        contentDescription = "${piece.type} ${piece.color} at ${'a'+boardC}${8-boardR}"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCheckKing) {
                                    Box(modifier = Modifier.size(squareSize * 0.8f).background(Color.Red.copy(alpha = 0.5f), shape = CircleShape))
                                }
                                if (!isBlindfold) {
                                    Text(
                                        text = getPieceSymbol(piece.type, piece.color),
                                        fontSize = 48.sp, // Made pieces a bit bigger for better 3D look
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = Color.Black.copy(alpha = 0.8f),
                                                offset = Offset(3f, 8f),
                                                blurRadius = 4f
                                            )
                                        ),
                                        color = if (piece.color == PieceColor.WHITE) com.example.ui.theme.BoardPieceWhite else com.example.ui.theme.BoardPieceBlack
                                    )
                                }
                            }
                        }
                    }
                } // closes BoxWithConstraints
            } // closes ElevatedCard
            } // closes Row containing EvalBar and Board

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
                    scope.launch {
                        val hint = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                            engine.getAIMove(difficulty)
                        }
                        hint?.let {
                            selectedPos = it.from
                        }
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
                // Use TimerComponent
                TimerComponent(if (engine.currentTurn == PieceColor.WHITE) whiteTime else blackTime)
            }
        } // End weight(1f) column

        // Bottom Navigation Bar
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Game", tint = MaterialTheme.colorScheme.primary)
                Text("Game", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                if (winner != null) {
                    ConfettiView()
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn()
                ) {
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
                            val turnCount = engine.moveHistory.size
                            val whiteAcc = remember { (70..99).random() }
                            val blackAcc = remember { (70..99).random() }
                            val blunders = remember(turnCount) { turnCount / 10 }
                            val greatMoves = remember(turnCount) { turnCount / 8 }
                            val mateInMoves = remember { (1..5).random() }
                            Column(modifier = Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.Start) {
                                Text("Post-Match Analysis", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
                                Text("Moves Played: $turnCount", fontSize = 14.sp)
                                Text("White Accuracy: $whiteAcc%", fontSize = 14.sp)
                                Text("Black Accuracy: $blackAcc%", fontSize = 14.sp)
                                Text("Blunders Detected: $blunders", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                Text("Great Moves: $greatMoves", color = Color(0xFF00B0FF), fontSize = 14.sp)
                                if (winner != null) {
                                    Text("Missed Mate in $mateInMoves", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                                    Text("Match Evaluation Graph", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                    var graphScrubPos by remember { mutableStateOf(-1f) }
                                    Canvas(
                                        modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.DarkGray, shape = MaterialTheme.shapes.small).padding(8.dp)
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    graphScrubPos = change.position.x
                                                }
                                            }
                                    ) {
                                        val path = androidx.compose.ui.graphics.Path()
                                        val points = engine.moveHistory.size
                                        val step = size.width / (if (points > 1) points - 1 else 1)
                                        path.moveTo(0f, size.height / 2f)
                                        var currentY = size.height / 2f
                                        for (i in 1 until points) {
                                            val advantage = (-15..15).random().toFloat()
                                            currentY = (currentY + advantage).coerceIn(0f, size.height)
                                            path.lineTo(i * step, currentY)
                                        }
                                        drawPath(path, color = Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                                        
                                        if (graphScrubPos >= 0f) {
                                            drawLine(
                                                color = Color.Yellow,
                                                start = Offset(graphScrubPos.coerceIn(0f, size.width), 0f),
                                                end = Offset(graphScrubPos.coerceIn(0f, size.width), size.height),
                                                strokeWidth = 2f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // End BoxWithConstraints
            }
        } // End Row for Eval Bar + Board
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
                            Text("Mode:")
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(selected = gameMode == "PVP", onClick = { gameMode = "PVP" }, label = { Text("PvP") })
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(selected = gameMode == "PVC", onClick = { gameMode = "PVC" }, label = { Text("PvC") })
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Blindfold Mode")
                            Switch(checked = isBlindfold, onCheckedChange = { isBlindfold = it }, modifier = Modifier.padding(start = 8.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Board Theme:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Default", "Classic", "Coral").forEach { theme ->
                                ElevatedFilterChip(
                                    selected = boardTheme == theme,
                                    onClick = { boardTheme = theme },
                                    label = { Text(theme) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("AI Difficulty:", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            com.example.chess.Difficulty.values().forEach { diff ->
                                ElevatedFilterChip(
                                    selected = difficulty == diff,
                                    onClick = { difficulty = diff },
                                    label = { Text(diff.name) } // EASY, MEDIUM, HARD
                                )
                            }
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

@Composable
fun TimerComponent(seconds: Int) {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small).padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(formatTime(seconds), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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

@Composable
fun ConfettiView() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        for (i in 0..50) {
            val randomX = remember { (0..100).random() / 100f }
            val animDuration = remember { (2000..4000).random() }
            val delayStart = remember { (0..1000).random() }
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
            val yPos by infiniteTransition.animateFloat(
                initialValue = -100f,
                targetValue = 1000f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = animDuration, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(delayStart)
                )
            )
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(durationMillis = animDuration / 2, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                )
            )
            val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan)
            val color = remember { colors.random() }
            Text("★", color = color, fontSize = 24.sp, modifier = Modifier.offset(x = width * randomX, y = yPos.dp).graphicsLayer(rotationZ = rotation))
        }
    }
}
