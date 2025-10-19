package com.example.mtictactoe

/**
 * Represents the entire state of the game at a given moment.
 * @param board The current state of the game board.
 * @param currentPlayer The player whose turn it is.
 */
data class Game(
    val board: Board = Board(),
    val currentPlayer: Player = Player.X
)

/**
 * Represents a single cell on the board using row and column coordinates.
 * @param row The row index (0-2).
 * @param col The column index (0-2).
 */
data class Cell(val row: Int, val col: Int)

/**
 * Represents a player in the game, either X or O.
 */
enum class Player {
    X, O
}

/**
 * Represents the 3x3 game board.
 * Manages the state of each cell.
 */
class Board {
    // A 2D array to store the state of the board. A null value means the cell is empty.
    private val board = Array(3) { Array<Player?>(3) { null } }
    /**
     * Places a player's mark on a specific cell.
     * Does nothing if the cell is already occupied.
     * @param cell The cell to modify.
     * @param player The player making the move.
     */
    fun setCell(cell: Cell, player: Player) {
        if (board[cell.row][cell.col] == null) {
            board[cell.row][cell.col] = player
        }
    }

    /**
     * Retrieves the player who has marked a specific cell.
     * @param cell The cell to check.
     * @return The player who marked the cell, or null if the cell is empty.
     */
    fun getCell(cell: Cell): Player? {
        return board[cell.row][cell.col]
    }

    /**
     * Gets a list of all currently empty cells on the board.
     * @return A list of [Cell] objects that are available for a move.
     */
    fun getAvailableCells(): List<Cell> {
        val cells = mutableListOf<Cell>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (board[i][j] == null) {
                    cells.add(Cell(i, j))
                }
            }
        }
        return cells
    }

    /**
     * Checks if the board is completely full.
     * @return True if there are no available cells left, false otherwise.
     */
    fun isFull(): Boolean {
        return getAvailableCells().isEmpty()
    }
}

/**
 * Checks the board for a winner.
 * @param board The board to check.
 * @return The winning [Player], or null if there is no winner yet.
 */
fun checkWinner(board: Board): Player? {
    // Check all rows for a win
    for (i in 0..2) {
        if (board.getCell(Cell(i, 0)) != null &&
            board.getCell(Cell(i, 0)) == board.getCell(Cell(i, 1)) &&
            board.getCell(Cell(i, 1)) == board.getCell(Cell(i, 2))) {
            return board.getCell(Cell(i, 0))
        }
    }

    // Check all columns for a win
    for (i in 0..2) {
        if (board.getCell(Cell(0, i)) != null &&
            board.getCell(Cell(0, i)) == board.getCell(Cell(1, i)) &&
            board.getCell(Cell(1, i)) == board.getCell(Cell(2, i))) {
            return board.getCell(Cell(0, i))
        }
    }

    // Check the two diagonals for a win
    if (board.getCell(Cell(0, 0)) != null &&
        board.getCell(Cell(0, 0)) == board.getCell(Cell(1, 1)) &&
        board.getCell(Cell(1, 1)) == board.getCell(Cell(2, 2))) {
        return board.getCell(Cell(0, 0))
    }

    if (board.getCell(Cell(0, 2)) != null &&
        board.getCell(Cell(0, 2)) == board.getCell(Cell(1, 1)) &&
        board.getCell(Cell(1, 1)) == board.getCell(Cell(2, 0))) {
        return board.getCell(Cell(0, 2))
    }

    // If no winner is found, return null
    return null
}

/**
 * Creates a deep copy of the board.
 * This is essential for the AI algorithms to simulate moves without affecting the actual game board.
 * @return A new [Board] instance with the same state as the original.
 */
fun Board.copy(): Board {
    val newBoard = Board()
    for (i in 0..2) {
        for (j in 0..2) {
            val cell = Cell(i, j)
            getCell(cell)?.let { player ->
                newBoard.setCell(cell, player)
            }
        }
    }
    return newBoard
}
