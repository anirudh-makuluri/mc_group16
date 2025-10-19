package com.example.mtictactoe

import kotlin.random.Random

fun getMediumMove(board: Board): Cell {
    return if (Random.nextBoolean()) {
        getBestMove(board)
    } else {
        getEasyMove(board)
    }
}
