package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@SuppressLint("MissingPermission")
class BluetoothManager(private val context: Context) {
    private val tag = "BluetoothManager"
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(false) // Defaults to false
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private val _autoReconnect = MutableStateFlow(true)
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _liveSensorData = MutableStateFlow<SensorData>(SensorData.empty())
    val liveSensorData: StateFlow<SensorData> = _liveSensorData.asStateFlow()

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages: SharedFlow<String> = _errorMessages.asSharedFlow()

    private var activeSocket: BluetoothSocket? = null
    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private var simulatorJob: Job? = null
    private var currentDeviceAddress: String? = null

    init {
        // Defaults to disconnected, no auto-simulation
        _liveSensorData.value = SensorData.empty()
    }

    fun toggleSimulator(enabled: Boolean) {
        _isSimulatorMode.value = enabled
        if (enabled) {
            disconnectRealBluetooth()
            startSimulation()
        } else {
            stopSimulation()
        }
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        _autoReconnect.value = enabled
    }

    fun connectDevice(address: String) {
        currentDeviceAddress = address
        _isSimulatorMode.value = false
        stopSimulation()

        connectionJob?.cancel()
        connectionJob = scope.launch {
            var attempt = 1
            while (isActive) {
                _connectionState.value = ConnectionState.CONNECTING
                try {
                    val adapter = BluetoothAdapter.getDefaultAdapter()
                    if (adapter == null) {
                        _errorMessages.emit("Bluetooth not supported on this device.")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        return@launch
                    }

                    if (!adapter.isEnabled) {
                        _errorMessages.emit("Bluetooth is disabled. Enable Bluetooth to connect.")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        return@launch
                    }

                    val device = adapter.getRemoteDevice(address)
                    Log.d(tag, "Connecting to ${device.name ?: "Device"} ($address)")

                    val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                    activeSocket = socket
                    socket.connect()

                    _connectionState.value = ConnectionState.CONNECTED
                    startReading(socket)
                    break // Connected successfully
                } catch (e: Exception) {
                    Log.e(tag, "Connection failed: ${e.message}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _liveSensorData.value = SensorData.empty()
                    activeSocket = null

                    if (_autoReconnect.value) {
                        _errorMessages.emit("Connection failed. Retrying in 5s (Attempt $attempt)...")
                        attempt++
                        delay(5000)
                    } else {
                        _errorMessages.emit("Failed to connect: ${e.localizedMessage}")
                        break
                    }
                }
            }
        }
    }

    fun disconnect() {
        currentDeviceAddress = null
        disconnectRealBluetooth()
    }

    private fun disconnectRealBluetooth() {
        connectionJob?.cancel()
        readJob?.cancel()
        try {
            activeSocket?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing socket: ${e.message}")
        }
        activeSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _liveSensorData.value = SensorData.empty()
    }

    private fun startReading(socket: BluetoothSocket) {
        readJob?.cancel()
        readJob = scope.launch {
            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)
            var bytes: Int
            val stringBuilder = StringBuilder()

            while (isActive) {
                try {
                    bytes = inputStream.read(buffer)
                    val chunk = String(buffer, 0, bytes)
                    stringBuilder.append(chunk)

                    // Process lines
                    var index = stringBuilder.indexOf("\n")
                    while (index != -1) {
                        val line = stringBuilder.substring(0, index).trim()
                        stringBuilder.delete(0, index + 1)

                        val parsed = SensorData.parse(line)
                        if (parsed != null) {
                            _liveSensorData.value = parsed
                        } else {
                            Log.w(tag, "Ignored corrupted packet: $line")
                        }
                        index = stringBuilder.indexOf("\n")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Disconnected from read stream: ${e.message}")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _liveSensorData.value = SensorData.empty()
                    activeSocket = null
                    _errorMessages.emit("Bluetooth device disconnected.")

                    // Trigger Auto Reconnect
                    if (_autoReconnect.value && currentDeviceAddress != null) {
                        connectDevice(currentDeviceAddress!!)
                    }
                    break
                }
            }
        }
    }

    private fun startSimulation() {
        simulatorJob?.cancel()
        _connectionState.value = ConnectionState.CONNECTED
        simulatorJob = scope.launch {
            while (isActive) {
                val mockData = SensorData.generateMock(_liveSensorData.value)
                _liveSensorData.value = mockData
                delay(1000)
            }
        }
    }

    private fun stopSimulation() {
        simulatorJob?.cancel()
        _liveSensorData.value = SensorData.empty()
        if (_isSimulatorMode.value) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
