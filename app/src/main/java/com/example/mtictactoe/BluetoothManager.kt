package com.example.mtictactoe

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.IOException
import java.util.*

class BluetoothManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private var isServer = false
    private var isConnected = false
    private var dataCallback: ((String) -> Unit)? = null
    private var jsonCallback: ((JSONObject) -> Unit)? = null
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val APP_NAME = "MTicTacToe"
        private val UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"
    }
    
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getPairedDevices(): Set<BluetoothDevice> {
        return try {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                bluetoothAdapter?.bondedDevices ?: emptySet()
            } else {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                emptySet()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting paired devices: ${e.message}")
            emptySet()
        }
    }

    suspend fun startServer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                    return@withContext false
                }
                
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    APP_NAME, 
                    UUID.fromString(UUID_STRING)
                )
                isServer = true
                Log.d(TAG, "Server started, waiting for connections...")
                true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting server: ${e.message}")
                false
            } catch (e: IOException) {
                Log.e(TAG, "Error starting server: ${e.message}")
                false
            }
        }
    }
    
    suspend fun waitForConnection(): BluetoothSocket? {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                    return@withContext null
                }
                
                val socket = serverSocket?.accept()
                connectedSocket = socket
                isConnected = true
                Log.d(TAG, "Client connected!")
                
                startDataListener()
                
                socket
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException accepting connection: ${e.message}")
                null
            } catch (e: IOException) {
                Log.e(TAG, "Error accepting connection: ${e.message}")
                null
            }
        }
    }

    suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                    return@withContext false
                }
                
                if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
                    Log.w(TAG, "BLUETOOTH_SCAN permission not granted")
                    return@withContext false
                }
                
                clientSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING))
                bluetoothAdapter?.cancelDiscovery()
                clientSocket?.connect()
                connectedSocket = clientSocket
                isConnected = true
                isServer = false
                Log.d(TAG, "Connected to ${device.name}")
                
                startDataListener()
                
                true
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException connecting to device: ${e.message}")
                try {
                    clientSocket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Error closing socket: ${closeException.message}")
                }
                false
            } catch (e: IOException) {
                Log.e(TAG, "Error connecting to device: ${e.message}")
                try {
                    clientSocket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Error closing socket: ${closeException.message}")
                }
                false
            }
        }
    }
    
    fun sendMessage(message: String) {
        try {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
            
            if (!isConnected || connectedSocket == null) {
                Log.w(TAG, "Not connected to any device")
                return
            }
            
            val outputStream = connectedSocket?.outputStream
            outputStream?.write(message.toByteArray())
            Log.d(TAG, "Message sent: $message")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending message: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "Error sending message: ${e.message}")
        }
    }
    
    fun sendJsonMessage(jsonData: JSONObject) {
        try {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                return
            }
            
            if (!isConnected || connectedSocket == null) {
                Log.w(TAG, "Not connected to any device")
                return
            }
            
            val jsonString = jsonData.toString()
            val outputStream = connectedSocket?.outputStream
            outputStream?.write(jsonString.toByteArray())
            Log.d(TAG, "JSON message sent: $jsonString")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending JSON message: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "Error sending JSON message: ${e.message}")
        }
    }
    
    fun sendGameState(player1Mac: String, player2Mac: String, player1Choice: String, player2Choice: String = "") {
        val gameState = JSONObject().apply {
            put("gameState", JSONObject().apply {
                put("board", JSONObject().apply {
                    put("0", JSONObject().apply {
                        put("0", " ")
                        put("1", " ")
                        put("2", " ")
                    })
                    put("1", JSONObject().apply {
                        put("0", " ")
                        put("1", " ")
                        put("2", " ")
                    })
                    put("2", JSONObject().apply {
                        put("0", " ")
                        put("1", " ")
                        put("2", " ")
                    })
                })
                put("turn", "0")
                put("winner", " ")
                put("draw", false)
                put("connectionEstablished", true)
                put("reset", false)
            })
            put("metadata", JSONObject().apply {
                put("choices", JSONObject().apply {
                    put("0", JSONObject().apply {
                        put("id", "player1")
                        put("name", player1Mac)
                    })
                    put("1", JSONObject().apply {
                        put("id", "player2")
                        put("name", player2Mac)
                    })
                })
                put("miniGame", JSONObject().apply {
                    put("player1Choice", player1Choice)
                    put("player2Choice", player2Choice)
                })
            })
        }
        sendJsonMessage(gameState)
    }
    
    fun getMacAddress(): String {
        return try {
            if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                bluetoothAdapter?.address ?: "Unknown"
            } else {
                "Unknown"
            }
        } catch (e: SecurityException) {
            "Unknown"
        }
    }
    
    fun setDataCallback(callback: (String) -> Unit) {
        dataCallback = callback
    }
    
    fun setJsonCallback(callback: (JSONObject) -> Unit) {
        jsonCallback = callback
    }
    
    private fun startDataListener() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = connectedSocket?.inputStream
                val buffer = ByteArray(1024)
                
                while (isConnected && connectedSocket != null) {
                    val bytes = inputStream?.read(buffer)
                    if (bytes != null && bytes > 0) {
                        val message = String(buffer, 0, bytes)
                        Log.d(TAG, "Message received: $message")

                        withContext(Dispatchers.Main) {
                            dataCallback?.invoke(message)

                            try {
                                val jsonObject = JSONObject(message)
                                jsonCallback?.invoke(jsonObject)
                            } catch (e: Exception) {

                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error reading data: ${e.message}")
                isConnected = false
            }
        }
    }
    
    fun closeConnection() {
        try {
            serverSocket?.close()
            clientSocket?.close()
            connectedSocket?.close()
            isConnected = false
            isServer = false
            connectedSocket = null
            Log.d(TAG, "Connection closed")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = isConnected
    fun isServer(): Boolean = isServer
}
