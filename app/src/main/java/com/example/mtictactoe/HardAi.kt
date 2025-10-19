package com.example.mtictactoe

fun minimax(board: Board, player: Player, alpha: Int, beta: Int): Int {
    val winner = checkWinner(board)
    if (winner == Player.O) return 1
    if (winner == Player.X) return -1
    if (board.isFull()) return 0

    var currentAlpha = alpha
    var currentBeta = beta

    if (player == Player.O) {
        var bestScore = Int.MIN_VALUE
        for (cell in board.getAvailableCells()) {
            val newBoard = board.copy()
            newBoard.setCell(cell, player)
            val score = minimax(newBoard, Player.X, currentAlpha, currentBeta)
            bestScore = maxOf(bestScore, score)
            currentAlpha = maxOf(currentAlpha, bestScore)
            if (currentBeta <= currentAlpha) {
                break
            }
        }
        return bestScore
    } else {
        var bestScore = Int.MAX_VALUE
        for (cell in board.getAvailableCells()) {
            val newBoard = board.copy()
            newBoard.setCell(cell, player)
            val score = minimax(newBoard, Player.O, currentAlpha, currentBeta)
            bestScore = minOf(bestScore, score)
            currentBeta = minOf(currentBeta, bestScore)
            if (currentBeta <= currentAlpha) {
                break
            }
        }
        return bestScore
    }
}

fun getBestMove(board: Board): Cell {
    var bestScore = Int.MIN_VALUE
    var bestMove: Cell? = null

    for (cell in board.getAvailableCells()) {
        val newBoard = board.copy()
        newBoard.setCell(cell, Player.O)
        val score = minimax(newBoard, Player.X, Int.MIN_VALUE, Int.MAX_VALUE)
        if (score > bestScore) {
            bestScore = score
            bestMove = cell
        }
    }
    return bestMove!!
}
