package com.example.mtictactoe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Misère Tic-Tac-Toe",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Pick Gameplay Type"
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.navigate(Routes.settingsScreen) }) {
            Text("Single Player (with AI)")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.navigate("") }) {
            Text("Multiplayer (same device)")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { navController.navigate(Routes.bluetoothScreen) }) {
            Text("Multiplayer (with bluetooth)")
        }
    }
}
