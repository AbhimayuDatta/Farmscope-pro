package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_logs")
data class SensorLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val plantName: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val satellites: Int,
    val soilMoisture: Double,
    val soilTemp: Double,
    val airQuality: Double,
    val light: Double,
    val airTemp: Double,
    val humidity: Double,
    val pressure: Double,
    val humanDetected: Int, // 0 or 1
    val nitrogen: Double,
    val phosphorus: Double,
    val potassium: Double,
    val pH: Double,
    val ec: Double,
    val status: String, // E.g. "Excellent", "Good", "Moderate", "Critical"
    val isManual: Boolean = false,
    val matrixCellId: String? = null // E.g., "A1", "B2", etc.
) {
    fun toCsvRow(): String {
        return "$id,${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(timestamp)}," +
                "${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(timestamp)}," +
                "$latitude,$longitude,$altitude,$satellites,$soilMoisture,$soilTemp,$airQuality,$light,$airTemp," +
                "$humidity,$pressure,$humanDetected,$nitrogen,$phosphorus,$potassium,$pH,$ec," +
                "\"$status\",$isManual,\"${matrixCellId ?: "N/A"}\",\"$plantName\""
    }

    companion object {
        fun csvHeader(): String {
            return "ID,Date,Time,Latitude,Longitude,Altitude,Satellites,SoilMoisture(%),SoilTemp(C),AirQuality(%),Light(Lux)," +
                    "AirTemp(C),Humidity(%),Pressure(hPa),HumanDetected,Nitrogen(N),Phosphorus(P),Potassium(K),pH,EC," +
                    "Status,IsManual,MatrixCell,PlantName"
        }
    }
}
