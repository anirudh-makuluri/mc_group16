package com.example.mtictactoe

import kotlin.random.Random

/**
 * Provides the move logic for the Easy difficulty AI.
 */

/**
 * Gets a move for the AI by selecting a random available cell.
 * This AI does not use any strategy and just picks a valid empty spot.
 * @param board The current game board.
 * @return A [Cell] representing the AI's chosen move.
 */
fun getEasyMove(board: Board): Cell {
    // Get all cells that are currently empty.
    val availableCells = board.getAvailableCells()
    // Return a random cell from the list of available cells.
    return availableCells[Random.nextInt(availableCells.size)]
}
