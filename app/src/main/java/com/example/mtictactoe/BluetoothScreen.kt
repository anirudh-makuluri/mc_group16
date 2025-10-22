package com.example.mtictactoe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun BluetoothScreen(navController: NavController) {
    val context = LocalContext.current
    val bluetoothManager = remember { BluetoothManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var isWaitingForConnection by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf(emptySet<android.bluetooth.BluetoothDevice>()) }
    var showConnectionAlert by remember { mutableStateOf(false) }
    var connectionMessage by remember { mutableStateOf("") }
    var isConnected by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }
    var showWhoGoesFirst by remember { mutableStateOf(false) }
    var myMacAddress by remember { mutableStateOf("") }
    var opponentMacAddress by remember { mutableStateOf("") }
    var gameDecisionMade by remember { mutableStateOf(false) }
    var whoGoesFirstResult by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) {
            pairedDevices = bluetoothManager.getPairedDevices()
        }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        
        hasPermissions = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        if (hasPermissions) {
            pairedDevices = bluetoothManager.getPairedDevices()
            myMacAddress = bluetoothManager.getMacAddress()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    LaunchedEffect(bluetoothManager) {
        bluetoothManager.setJsonCallback { jsonObject ->

            val gameState = jsonObject.optJSONObject("gameState")
            val metadata = jsonObject.optJSONObject("metadata")
            val miniGame = metadata?.optJSONObject("miniGame")
            
            if (gameState?.optBoolean("connectionEstablished") == true && miniGame != null) {
                val player1Choice = miniGame.optString("player1Choice", "")
                val player2Choice = miniGame.optString("player2Choice", "")
                
                
                if (player1Choice.isNotEmpty() || player2Choice.isNotEmpty()) {
                    showWhoGoesFirst = false
                    gameDecisionMade = true

                    val choiceMade = player1Choice.ifEmpty { player2Choice }

                    // 0: opponent, 1: me
                    whoGoesFirstResult = if (choiceMade == "1") {
                        "You go first!"
                    } else {
                        "Opponent goes first!"
                    }
                }
            }
        }
    }
    

    LaunchedEffect(isWaitingForConnection) {
        if (isWaitingForConnection) {
            val success = bluetoothManager.startServer()
            if (success) {
                val socket = bluetoothManager.waitForConnection()
                if (socket != null) {
                    isConnected = true
                    isWaitingForConnection = false
                    showWhoGoesFirst = true
                }
            } else {
                isWaitingForConnection = false
                connectionMessage = "Failed to start server"
                showConnectionAlert = true
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bluetooth Multiplayer",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Choose your role in the game"
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        if (isWaitingForConnection) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Waiting for player to join...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        isWaitingForConnection = false
                        bluetoothManager.closeConnection()
                    }
                ) {
                    Text("Cancel")
                }
            }
        } else if (isScanning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Scanning for devices...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        isScanning = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        } else if (isConnected) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Connected!", style = MaterialTheme.typography.headlineSmall)

                if (gameDecisionMade && whoGoesFirstResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Game Decision",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = whoGoesFirstResult,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Game is ready to start!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        isConnected = false
                        bluetoothManager.closeConnection()
                        gameDecisionMade = false
                        whoGoesFirstResult = ""
                    }
                ) {
                    Text("Disconnect")
                }
            }
        } else if (!hasPermissions) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Bluetooth permissions required")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        permissionLauncher.launch(arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ))
                    }
                ) {
                    Text("Grant Permissions")
                }
            }
        } else {
            Button(
                onClick = { 
                    isWaitingForConnection = true
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Create Game")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    isScanning = true
                    pairedDevices = bluetoothManager.getPairedDevices()
                },
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text("Join Game")
            }
        }

        if (isScanning && pairedDevices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Select a device to connect:")
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(pairedDevices.toList()) { device ->
                    Card(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxSize(),
                        onClick = {
                            coroutineScope.launch {
                                val success = bluetoothManager.connectToDevice(device)
                                isScanning = false
                                if (success) {
                                    isConnected = true
                                    opponentMacAddress = device.address
                                    showWhoGoesFirst = true
                                } else {
                                    connectionMessage = "Failed to connect to ${device.name ?: "Unknown Device"}"
                                    showConnectionAlert = true
                                }
                            }
                        }
                    ) {
                        Text(
                            text = device.name ?: "Unknown Device",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { navController.popBackStack() }
        ) {
            Text("Back to Home")
        }
    }

    if (showConnectionAlert) {
        AlertDialog(
            onDismissRequest = { showConnectionAlert = false },
            title = { Text("Connection Status") },
            text = { Text(connectionMessage) },
            confirmButton = {
                TextButton(
                    onClick = { showConnectionAlert = false }
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showWhoGoesFirst && !gameDecisionMade) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismissing */ },
            title = { Text("Who Goes First?") },
            text = { 
                Column {
                    Text("Choose who will go first in the game:")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showWhoGoesFirst = false
                        gameDecisionMade = true
                        // 0: me, 1: opponent
                        bluetoothManager.sendGameState(myMacAddress, opponentMacAddress, "0")
                        whoGoesFirstResult = "You go first!"
                    }
                ) {
                    Text("ME")
                }
            },
            dismissButton = {
                Button(
                    onClick = { 
                        showWhoGoesFirst = false
                        gameDecisionMade = true
                        // 0: me, 1: opponent
                        bluetoothManager.sendGameState(myMacAddress, opponentMacAddress, "1")
                        whoGoesFirstResult = "Opponent goes first!"
                    }
                ) {
                    Text("OPPONENT")
                }
            }
        )
    }
    
}
