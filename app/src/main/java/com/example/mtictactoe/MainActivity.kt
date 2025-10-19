package com.example.mtictactoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mtictactoe.ui.theme.MTicTacToeTheme

/**
 * The main entry point of the application.
 * This activity hosts the Jetpack Compose UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MTicTacToeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination=Routes.homeScreen, builder={
                        composable(Routes.homeScreen){
                            HomeScreen(navController)
                        }
                        composable(Routes.settingsScreen){
                            SettingsScreen(navController)
                        }
                        composable(Routes.gameScreen+"/{difficulty}"){
                            val difficulty = it.arguments?.getString("difficulty")
                            GameScreen(difficulty)
                        }

                    }
                    )

                }
            }
        }
    }
}

