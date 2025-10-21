package com.example.mtictactoe

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) {
            pairedDevices = bluetoothManager.getPairedDevices()
        }
    }
    
    // Check permissions on startup
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
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    
    // Handle connection waiting
    LaunchedEffect(isWaitingForConnection) {
        if (isWaitingForConnection) {
            val success = bluetoothManager.startServer()
            if (success) {
                val socket = bluetoothManager.waitForConnection()
                if (socket != null) {
                    isConnected = true
                    connectionMessage = "Player joined your game!"
                    showConnectionAlert = true
                    isWaitingForConnection = false
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
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        isConnected = false
                        bluetoothManager.closeConnection()
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
        
        // Show paired devices when scanning
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
                                    connectionMessage = "Connected to ${device.name ?: "Unknown Device"}!"
                                    showConnectionAlert = true
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
    
    // Connection alert dialog
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
}
