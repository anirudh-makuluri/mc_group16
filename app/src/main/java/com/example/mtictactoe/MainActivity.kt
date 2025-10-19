package com.example.mtictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mtictactoe.ui.theme.MTicTacToeTheme

/**
 * The main entry point of the application.
 * This activity hosts the Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content of the activity to be our main game screen.
        setContent {
            MTicTacToeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    GameScreen()
                }
            }
        }
    }
}

/**
 * A composable function that represents the main screen of the game.
 * It manages the game state and displays the board, status, and buttons.
 */
@Composable
fun GameScreen() {
    // State for the current game, including the board and current player.
    // `remember` makes sure the state is kept across recompositions.
    var game by remember { mutableStateOf(Game()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Check for a winner or a draw to display the status.
        val winner = checkWinner(game.board)
        val statusText = when {
            winner != null -> "Winner: $winner"
            game.board.isFull() -> "Draw"
            else -> "Player: ${game.currentPlayer}"
        }

        Text(text = statusText)
        Spacer(modifier = Modifier.height(16.dp))

        // Display the Tic-Tac-Toe board.
        BoardView(game.board) { cell ->
            // This block is executed when a cell is clicked.
            // It only allows a move if the game is ongoing and the cell is empty.
            if (winner == null && game.board.getCell(cell) == null && game.currentPlayer == Player.X) {
                // Player's move
                val newPlayerBoard = game.board.copy().apply { setCell(cell, Player.X) }
                var updatedGame = game.copy(board = newPlayerBoard, currentPlayer = Player.O)
                game = updatedGame // Update the game state to trigger recomposition.

                // AI's turn, if the game is not over.
                val newWinner = checkWinner(updatedGame.board)
                if (newWinner == null && !updatedGame.board.isFull()) {
                    val aiMove = getEasyMove(updatedGame.board) // Get the AI's move.
                    val newAiBoard = updatedGame.board.copy().apply { setCell(aiMove, Player.O) }
                    updatedGame = updatedGame.copy(board = newAiBoard, currentPlayer = Player.X)
                    game = updatedGame // Update state again for the AI's move.
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to reset the game to its initial state.
        Button(onClick = { game = Game() }) {
            Text(text = "New Game")
        }
    }
}

/**
 * A composable function that draws the Tic-Tac-Toe board and handles user input.
 * @param board The current state of the board.
 * @param onCellClick A callback function to be invoked when a cell is clicked.
 */
@Composable
fun BoardView(board: Board, onCellClick: (Cell) -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .aspectRatio(1f)
            .pointerInput(Unit) {
                // Detect tap gestures to determine which cell was clicked.
                detectTapGestures {
                    val row = (it.y / (size.height / 3)).toInt().coerceIn(0, 2)
                    val col = (it.x / (size.width / 3)).toInt().coerceIn(0, 2)
                    onCellClick(Cell(row, col))
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / 3
            // Draw the grid lines.
            for (i in 1..2) {
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, cellSize * i),
                    end = Offset(size.width, cellSize * i),
                    strokeWidth = 5f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(cellSize * i, 0f),
                    end = Offset(cellSize * i, size.height),
                    strokeWidth = 5f
                )
            }

            // Draw the X's and O's on the board based on the game state.
            for (i in 0..2) {
                for (j in 0..2) {
                    board.getCell(Cell(i, j))?.let { player ->
                        val center = Offset(
                            (j + 0.5f) * cellSize,
                            (i + 0.5f) * cellSize
                        )
                        if (player == Player.X) {
                            // Draw 'X'
                            val halfCell = cellSize / 4
                            drawLine(
                                color = Color.Black,
                                start = Offset(center.x - halfCell, center.y - halfCell),
                                end = Offset(center.x + halfCell, center.y + halfCell),
                                strokeWidth = 8f
                            )
                            drawLine(
                                color = Color.Black,
                                start = Offset(center.x - halfCell, center.y + halfCell),
                                end = Offset(center.x + halfCell, center.y - halfCell),
                                strokeWidth = 8f
                            )
                        } else {
                            // Draw 'O'
                            drawCircle(
                                color = Color.Black,
                                radius = cellSize / 4,
                                center = center,
                                style = Stroke(width = 8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A preview for the GameScreen, visible in the Android Studio editor.
 */
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MTicTacToeTheme {
        GameScreen()
    }
}
