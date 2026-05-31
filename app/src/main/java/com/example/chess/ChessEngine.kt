package com.example.chess

import kotlin.math.abs

enum class PieceColor { WHITE, BLACK }
enum class PieceType { PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING }

data class Piece(val type: PieceType, val color: PieceColor)

data class Position(val row: Int, val col: Int)

data class Move(val from: Position, val to: Position, val pieceMoved: Piece, val pieceCaptured: Piece?)

class ChessEngine {
    var board = Array(8) { Array<Piece?>(8) { null } }
        private set
    var currentTurn = PieceColor.WHITE
        private set
    val moveHistory = mutableListOf<Move>()

    init {
        setupBoard()
    }

    private fun setupBoard() {
        for (i in 0..7) {
            board[1][i] = Piece(PieceType.PAWN, PieceColor.BLACK)
            board[6][i] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }
        val order = arrayOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
        for (i in 0..7) {
            board[0][i] = Piece(order[i], PieceColor.BLACK)
            board[7][i] = Piece(order[i], PieceColor.WHITE)
        }
    }

    fun pieceAt(pos: Position): Piece? = board[pos.row][pos.col]
    fun pieceAt(row: Int, col: Int): Piece? = board[row][col]

    fun isValidMove(from: Position, to: Position): Boolean {
        val piece = pieceAt(from) ?: return false
        if (piece.color != currentTurn) return false
        val destPiece = pieceAt(to)
        if (destPiece?.color == piece.color) return false
        
        return isPseudoLegalMove(piece, from, to) && !moveLeavesKingInCheck(from, to)
    }
    
    private fun isPseudoLegalMove(piece: Piece, from: Position, to: Position): Boolean {
        val dr = to.row - from.row
        val dc = to.col - from.col
        val destPiece = pieceAt(to)
        
        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                if (dc == 0) {
                    if (dr == dir && destPiece == null) return true
                    if (dr == 2 * dir && from.row == startRow && destPiece == null && pieceAt(from.row + dir, from.col) == null) return true
                } else if (abs(dc) == 1 && dr == dir) {
                    if (destPiece != null && destPiece.color != piece.color) return true // capture
                }
                return false
            }
            PieceType.KNIGHT -> {
                return (abs(dr) == 2 && abs(dc) == 1) || (abs(dr) == 1 && abs(dc) == 2)
            }
            PieceType.BISHOP -> {
                if (abs(dr) != abs(dc)) return false
                return isPathClear(from, to)
            }
            PieceType.ROOK -> {
                if (dr != 0 && dc != 0) return false
                return isPathClear(from, to)
            }
            PieceType.QUEEN -> {
                if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return false
                return isPathClear(from, to)
            }
            PieceType.KING -> {
                return abs(dr) <= 1 && abs(dc) <= 1
            }
        }
    }
    
    private fun isPathClear(from: Position, to: Position): Boolean {
        val dr = Integer.signum(to.row - from.row)
        val dc = Integer.signum(to.col - from.col)
        var r = from.row + dr
        var c = from.col + dc
        while (r != to.row || c != to.col) {
            if (pieceAt(r, c) != null) return false
            r += dr
            c += dc
        }
        return true
    }
    
    private fun moveLeavesKingInCheck(from: Position, to: Position): Boolean {
        // Temporarily make move
        val p = board[from.row][from.col]
        val captured = board[to.row][to.col]
        board[to.row][to.col] = p
        board[from.row][from.col] = null
        
        val inCheck = isKingInCheck(p!!.color)
        
        // Revert
        board[from.row][from.col] = p
        board[to.row][to.col] = captured
        return inCheck
    }
    
    fun isKingInCheck(color: PieceColor): Boolean {
        var kingPos: Position? = null
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p?.color == color && p.type == PieceType.KING) {
                    kingPos = Position(r, c)
                    break
                }
            }
        }
        if (kingPos == null) return false
        
        for (r in 0..7) {
            for (c in 0..7) {
                val opp = board[r][c]
                if (opp != null && opp.color != color) {
                    if (isPseudoLegalMove(opp, Position(r, c), kingPos)) return true
                }
            }
        }
        return false
    }
    
    fun isCheckmate(): Boolean {
        if (!isKingInCheck(currentTurn)) return false
        return !hasLegalMoves(currentTurn)
    }

    fun hasLegalMoves(color: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color) {
                    for (tr in 0..7) {
                        for (tc in 0..7) {
                            if (isValidMove(Position(r, c), Position(tr, tc))) return true
                        }
                    }
                }
            }
        }
        return false
    }
    
    fun move(from: Position, to: Position): Boolean {
        if (isValidMove(from, to)) {
            val p = pieceAt(from)!!
            val captured = pieceAt(to)
            board[to.row][to.col] = p
            board[from.row][from.col] = null
            moveHistory.add(Move(from, to, p, captured))
            
            // Promotion
            if (p.type == PieceType.PAWN && (to.row == 0 || to.row == 7)) {
                board[to.row][to.col] = Piece(PieceType.QUEEN, p.color) // Auto-promote to Queen for simplicity
            }
            
            currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
            return true
        }
        return false
    }

    fun undo() {
        if (moveHistory.isEmpty()) return
        val lastMove = moveHistory.removeAt(moveHistory.size - 1)
        board[lastMove.from.row][lastMove.from.col] = lastMove.pieceMoved
        board[lastMove.to.row][lastMove.to.col] = lastMove.pieceCaptured
        currentTurn = if (currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
    }
}
