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
import java.io.IOException
import java.util.*

class BluetoothManager(private val context: Context) {
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var isServer = false
    private var isConnected = false
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val APP_NAME = "MTicTacToe"
        private val UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB" // Standard UUID for SPP
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
                isConnected = true
                Log.d(TAG, "Client connected!")
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
                isConnected = true
                isServer = false
                Log.d(TAG, "Connected to ${device.name}")
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
            
            val outputStream = if (isServer) {
                serverSocket?.accept()?.outputStream
            } else {
                clientSocket?.outputStream
            }
            outputStream?.write(message.toByteArray())
            Log.d(TAG, "Message sent: $message")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException sending message: ${e.message}")
        } catch (e: IOException) {
            Log.e(TAG, "Error sending message: ${e.message}")
        }
    }
    
    fun closeConnection() {
        try {
            serverSocket?.close()
            clientSocket?.close()
            isConnected = false
            isServer = false
            Log.d(TAG, "Connection closed")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = isConnected
    fun isServer(): Boolean = isServer
}
