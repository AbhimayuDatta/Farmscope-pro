package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothManager
import com.example.bluetooth.ConnectionState
import com.example.bluetooth.SensorData
import com.example.bluetooth.StatusCalculator
import com.example.bluetooth.StatusLevel
import com.example.data.db.AppDatabase
import com.example.data.model.PlantProfile
import com.example.data.model.SensorLog
import com.example.data.repository.FarmRepository
import com.example.util.LogFileHelper
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val repository = FarmRepository(database.plantProfileDao(), database.sensorLogDao())

    val bluetoothManager = BluetoothManager(context)

    // UI States
    val plantProfiles: StateFlow<List<PlantProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfile = MutableStateFlow<PlantProfile?>(null)
    val selectedProfile: StateFlow<PlantProfile?> = _selectedProfile.asStateFlow()

    val currentSensorData: StateFlow<SensorData> = bluetoothManager.liveSensorData

    // Continuous Logging States
    private val _isContinuousLogging = MutableStateFlow(false)
    val isContinuousLogging: StateFlow<Boolean> = _isContinuousLogging.asStateFlow()

    private val _loggingFile = MutableStateFlow<File?>(null)
    val loggingFile: StateFlow<File?> = _loggingFile.asStateFlow()

    private val _loggingStartTime = MutableStateFlow(0L)
    val loggingStartTime: StateFlow<Long> = _loggingStartTime.asStateFlow()

    private val _loggingRecordsCount = MutableStateFlow(0)
    val loggingRecordsCount: StateFlow<Int> = _loggingRecordsCount.asStateFlow()

    private val _elapsedTimeStr = MutableStateFlow("00:00:00")
    val elapsedTimeStr: StateFlow<String> = _elapsedTimeStr.asStateFlow()

    // Saved CSV Logs list
    private val _savedLogsList = MutableStateFlow<List<File>>(emptyList())
    val savedLogsList: StateFlow<List<File>> = _savedLogsList.asStateFlow()

    // Notification Cooldown Maps (to avoid spamming notifications every second)
    private val notificationCooldowns = mutableMapOf<String, Long>()
    private val COOLDOWN_MS = 30000L // 30 seconds cooldown

    // Matrix / Mapping states
    // A mapping of cellId ("A1".."D4") to its latest SensorLog
    private val _matrixCellsState = MutableStateFlow<Map<String, SensorLog>>(emptyMap())
    val matrixCellsState: StateFlow<Map<String, SensorLog>> = _matrixCellsState.asStateFlow()

    // Selected cell for viewing details / history
    val selectedCellId = MutableStateFlow<String?>("A1")

    // Logs for the selected cell
    private val _selectedCellHistory = MutableStateFlow<List<SensorLog>>(emptyList())
    val selectedCellHistory: StateFlow<List<SensorLog>> = _selectedCellHistory.asStateFlow()

    // Heatmap mode parameter ("Soil Moisture", "Temperature", "Humidity", "Light", "Air Quality", "Nitrogen", "Phosphorus", "Potassium", "pH", "EC", "Health Score")
    val heatmapParameter = MutableStateFlow("Soil Moisture")

    // GPS Field Logs (for mapping onto the aerial land image)
    private val _gpsFieldLogs = MutableStateFlow<List<SensorLog>>(emptyList())
    val gpsFieldLogs: StateFlow<List<SensorLog>> = _gpsFieldLogs.asStateFlow()

    private var loggingTimerJob: Job? = null
    private var telemetryCollectionJob: Job? = null

    init {
        NotificationHelper.initNotificationChannel(context)
        viewModelScope.launch {
            repository.checkAndPrepopulateDefaults()
            // Default select Tomato as profile
            val profilesList = repository.allProfiles.first()
            if (profilesList.isNotEmpty()) {
                _selectedProfile.value = profilesList.find { it.name == "Tomato" } ?: profilesList.first()
            }

            refreshLogsList()
            loadMatrixCellsState()
            loadGpsFieldLogs()
        }

        // Handle incoming live telemetry and continuous logs / notifications
        telemetryCollectionJob = viewModelScope.launch {
            currentSensorData.collect { data ->
                handleTelemetryTick(data)
            }
        }
    }

    private fun handleTelemetryTick(data: SensorData) {
        val profile = _selectedProfile.value

        // Continuous logging write
        if (_isContinuousLogging.value) {
            val file = _loggingFile.value
            if (file != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val statusText = getOverallHealthText(data, profile)
                    val sensorLog = SensorLog(
                        plantName = profile?.name ?: "Default",
                        latitude = data.latitude,
                        longitude = data.longitude,
                        altitude = data.altitude,
                        satellites = data.satellites,
                        soilMoisture = data.soilMoisture,
                        soilTemp = data.soilTemp,
                        airQuality = data.airQuality,
                        light = data.light,
                        airTemp = data.airTemp,
                        humidity = data.humidity,
                        pressure = data.pressure,
                        humanDetected = data.humanDetected,
                        nitrogen = data.nitrogen,
                        phosphorus = data.phosphorus,
                        potassium = data.potassium,
                        pH = data.pH,
                        ec = data.ec,
                        status = statusText,
                        isManual = false
                    )
                    LogFileHelper.appendLogToCsv(file, sensorLog)
                    _loggingRecordsCount.value = _loggingRecordsCount.value + 1
                }
            }
        }
    }

    fun selectProfile(profile: PlantProfile) {
        _selectedProfile.value = profile
    }

    fun createProfile(profile: PlantProfile) {
        viewModelScope.launch {
            repository.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: PlantProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            // Fallback selection if active got deleted
            val list = repository.allProfiles.first()
            if (_selectedProfile.value?.name == profile.name) {
                _selectedProfile.value = list.firstOrNull { it.name != profile.name }
            }
        }
    }

    // Manual Data Logging
    fun saveCurrentReadingManual() {
        val data = currentSensorData.value
        val profile = _selectedProfile.value
        viewModelScope.launch {
            val statusText = getOverallHealthText(data, profile)
            val log = SensorLog(
                plantName = profile?.name ?: "Default",
                latitude = data.latitude,
                longitude = data.longitude,
                altitude = data.altitude,
                satellites = data.satellites,
                soilMoisture = data.soilMoisture,
                soilTemp = data.soilTemp,
                airQuality = data.airQuality,
                light = data.light,
                airTemp = data.airTemp,
                humidity = data.humidity,
                pressure = data.pressure,
                humanDetected = data.humanDetected,
                nitrogen = data.nitrogen,
                phosphorus = data.phosphorus,
                potassium = data.potassium,
                pH = data.pH,
                ec = data.ec,
                status = statusText,
                isManual = true
            )
            // Insert in Room
            repository.insertLog(log)

            // Save to manual CSV file
            viewModelScope.launch(Dispatchers.IO) {
                val csvFile = LogFileHelper.createNewSessionFile(context, isManual = true)
                LogFileHelper.appendLogToCsv(csvFile, log)
                refreshLogsList()
            }
        }
    }

    // Continuous Data Logging Control
    fun startContinuousLogging() {
        if (_isContinuousLogging.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val newFile = LogFileHelper.createNewSessionFile(context, isManual = false)
            _loggingFile.value = newFile
            _loggingStartTime.value = System.currentTimeMillis()
            _loggingRecordsCount.value = 0
            _isContinuousLogging.value = true

            // Launch timer job to update elapsed time
            loggingTimerJob?.cancel()
            loggingTimerJob = viewModelScope.launch {
                while (_isContinuousLogging.value) {
                    val elapsedMs = System.currentTimeMillis() - _loggingStartTime.value
                    val seconds = (elapsedMs / 1000) % 60
                    val minutes = (elapsedMs / (1000 * 60)) % 60
                    val hours = (elapsedMs / (1000 * 60 * 60)) % 24
                    _elapsedTimeStr.value = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    delay(1000)
                }
            }
            refreshLogsList()
        }
    }

    fun stopContinuousLogging() {
        if (!_isContinuousLogging.value) return
        _isContinuousLogging.value = false
        loggingTimerJob?.cancel()
        _loggingFile.value = null
        refreshLogsList()
    }

    // CSV File Directory refresh
    fun refreshLogsList() {
        viewModelScope.launch(Dispatchers.IO) {
            _savedLogsList.value = LogFileHelper.getAllLogFiles(context)
        }
    }

    fun renameLogFile(file: File, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            LogFileHelper.renameFile(file, newName)
            refreshLogsList()
        }
    }

    fun deleteLogFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            LogFileHelper.deleteFile(file)
            refreshLogsList()
        }
    }

    // Matrix Grid Actions
    private fun loadMatrixCellsState() {
        viewModelScope.launch {
            // Retrieve latest log for each coordinate cell A1 to D4
            val cells = listOf("A", "B", "C", "D")
            val latestState = mutableMapOf<String, SensorLog>()
            for (r in cells) {
                for (c in 1..4) {
                    val cellId = "$r$c"
                    val latestLog = repository.getLatestLogForCell(cellId)
                    if (latestLog != null) {
                        latestState[cellId] = latestLog
                    }
                }
            }
            _matrixCellsState.value = latestState

            // Also reload current selection's history
            selectedCellId.value?.let { cellId ->
                loadCellHistory(cellId)
            }
        }
    }

    fun loadCellHistory(cellId: String) {
        viewModelScope.launch {
            _selectedCellHistory.value = repository.getLogsForCell(cellId)
        }
    }

    fun saveCellReading(cellId: String, isAppend: Boolean) {
        val data = currentSensorData.value
        val profile = _selectedProfile.value
        viewModelScope.launch {
            val statusText = getOverallHealthText(data, profile)
            val log = SensorLog(
                plantName = profile?.name ?: "Default",
                latitude = data.latitude,
                longitude = data.longitude,
                altitude = data.altitude,
                satellites = data.satellites,
                soilMoisture = data.soilMoisture,
                soilTemp = data.soilTemp,
                airQuality = data.airQuality,
                light = data.light,
                airTemp = data.airTemp,
                humidity = data.humidity,
                pressure = data.pressure,
                humanDetected = data.humanDetected,
                nitrogen = data.nitrogen,
                phosphorus = data.phosphorus,
                potassium = data.potassium,
                pH = data.pH,
                ec = data.ec,
                status = statusText,
                isManual = true,
                matrixCellId = cellId
            )

            if (!isAppend) {
                // Overwrite means delete previous cell logs before inserting new one
                repository.deleteLogsForCell(cellId)
            }

            repository.insertLog(log)
            loadMatrixCellsState()
        }
    }

    fun deleteCellHistory(cellId: String) {
        viewModelScope.launch {
            repository.deleteLogsForCell(cellId)
            loadMatrixCellsState()
        }
    }

    // Helper functions for overall health percent and text
    fun getOverallHealthText(data: SensorData, profile: PlantProfile?): String {
        val score = StatusCalculator.getHealthPercentage(data, profile)
        return when {
            score >= 85 -> "Excellent"
            score >= 70 -> "Good"
            score >= 50 -> "Moderate"
            else -> "Critical"
        }
    }

    // GPS Field Map Operations
    fun loadGpsFieldLogs() {
        viewModelScope.launch {
            _gpsFieldLogs.value = repository.getAllLogsList().filter { it.matrixCellId == "GPS_FIELD_MAP" }
        }
    }

    fun saveGpsFieldLog() {
        val data = currentSensorData.value
        val profile = _selectedProfile.value
        viewModelScope.launch {
            val statusText = getOverallHealthText(data, profile)
            val log = SensorLog(
                plantName = profile?.name ?: "Default",
                latitude = data.latitude,
                longitude = data.longitude,
                altitude = data.altitude,
                satellites = data.satellites,
                soilMoisture = data.soilMoisture,
                soilTemp = data.soilTemp,
                airQuality = data.airQuality,
                light = data.light,
                airTemp = data.airTemp,
                humidity = data.humidity,
                pressure = data.pressure,
                humanDetected = data.humanDetected,
                nitrogen = data.nitrogen,
                phosphorus = data.phosphorus,
                potassium = data.potassium,
                pH = data.pH,
                ec = data.ec,
                status = statusText,
                isManual = true,
                matrixCellId = "GPS_FIELD_MAP"
            )
            repository.insertLog(log)
            loadGpsFieldLogs()
        }
    }

    fun deleteGpsFieldLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLogById(logId)
            loadGpsFieldLogs()
        }
    }

    // Custom Background Image Support
    private val prefs = context.getSharedPreferences("farm_sense_prefs", android.content.Context.MODE_PRIVATE)

    private fun saveUriToPrivateFile(uriString: String, filename: String): String? {
        try {
            val uri = android.net.Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val outputFile = java.io.File(context.filesDir, filename)
            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private val _customMapImageUri = MutableStateFlow<String?>(
        prefs.getString("custom_map_image_uri", null)?.let { path ->
            if (java.io.File(path).exists()) path else null
        }
    )
    val customMapImageUri: StateFlow<String?> = _customMapImageUri.asStateFlow()

    fun updateCustomMapImageUri(uriString: String?) {
        if (uriString == null) {
            _customMapImageUri.value = null
            prefs.edit().remove("custom_map_image_uri").apply()
            java.io.File(context.filesDir, "custom_map_image.png").delete()
        } else {
            val localPath = saveUriToPrivateFile(uriString, "custom_map_image.png")
            if (localPath != null) {
                _customMapImageUri.value = localPath
                prefs.edit().putString("custom_map_image_uri", localPath).apply()
            }
        }
    }

    private val _customLogoImageUri = MutableStateFlow<String?>(
        prefs.getString("custom_logo_image_uri", null)?.let { path ->
            if (java.io.File(path).exists()) path else null
        }
    )
    val customLogoImageUri: StateFlow<String?> = _customLogoImageUri.asStateFlow()

    fun updateCustomLogoImageUri(uriString: String?) {
        if (uriString == null) {
            _customLogoImageUri.value = null
            prefs.edit().remove("custom_logo_image_uri").apply()
            java.io.File(context.filesDir, "custom_logo_image.png").delete()
        } else {
            val localPath = saveUriToPrivateFile(uriString, "custom_logo_image.png")
            if (localPath != null) {
                _customLogoImageUri.value = localPath
                prefs.edit().putString("custom_logo_image_uri", localPath).apply()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
        loggingTimerJob?.cancel()
        telemetryCollectionJob?.cancel()
    }
}
