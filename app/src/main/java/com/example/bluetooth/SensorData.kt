package com.example.bluetooth

import com.example.data.model.PlantProfile
import java.util.Date

data class SensorData(
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
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun empty(): SensorData {
            return SensorData(
                latitude = 0.0,
                longitude = 0.0,
                altitude = 0.0,
                satellites = 0,
                soilMoisture = 0.0,
                soilTemp = 0.0,
                airQuality = 0.0,
                light = 0.0,
                airTemp = 0.0,
                humidity = 0.0,
                pressure = 0.0,
                humanDetected = 0,
                nitrogen = 0.0,
                phosphorus = 0.0,
                potassium = 0.0,
                pH = 0.0,
                ec = 0.0,
                timestamp = System.currentTimeMillis()
            )
        }

        fun parse(csv: String): SensorData? {
            try {
                val parts = csv.trim().split(",")
                if (parts.size < 17) return null // Ignore corrupted packets
                return SensorData(
                    latitude = parts[0].toDouble(),
                    longitude = parts[1].toDouble(),
                    altitude = parts[2].toDouble(),
                    satellites = parts[3].toInt(),
                    soilMoisture = parts[4].toDouble(),
                    soilTemp = parts[5].toDouble(),
                    airQuality = parts[6].toDouble(),
                    light = parts[7].toDouble(),
                    airTemp = parts[8].toDouble(),
                    humidity = parts[9].toDouble(),
                    pressure = parts[10].toDouble(),
                    humanDetected = parts[11].toInt(),
                    nitrogen = parts[12].toDouble(),
                    phosphorus = parts[13].toDouble(),
                    potassium = parts[14].toDouble(),
                    pH = parts[15].toDouble(),
                    ec = parts[16].toDouble()
                )
            } catch (e: Exception) {
                return null // Ignore corrupted packets
            }
        }

        fun generateMock(lastData: SensorData? = null): SensorData {
            val baseLat = 28.613939
            val baseLon = 77.209023
            // Add slight random walks to GPS and sensor values for realism
            val r = java.util.Random()
            val offsetLat = if (lastData != null) lastData.latitude + (r.nextDouble() - 0.5) * 0.0001 else baseLat
            val offsetLon = if (lastData != null) lastData.longitude + (r.nextDouble() - 0.5) * 0.0001 else baseLon
            val baseSoilMoist = if (lastData != null) {
                (lastData.soilMoisture + (r.nextDouble() - 0.5) * 2).coerceIn(10.0, 95.0)
            } else 62.0
            val baseAirTemp = if (lastData != null) {
                (lastData.airTemp + (r.nextDouble() - 0.5) * 0.4).coerceIn(10.0, 45.0)
            } else 31.5
            val baseHum = if (lastData != null) {
                (lastData.humidity + (r.nextDouble() - 0.5) * 1.5).coerceIn(20.0, 90.0)
            } else 64.7
            val basePress = if (lastData != null) {
                (lastData.pressure + (r.nextDouble() - 0.5) * 0.5).coerceIn(950.0, 1050.0)
            } else 1008.3
            val baseLight = if (lastData != null) {
                (lastData.light + (r.nextDouble() - 0.5) * 10).coerceIn(50.0, 1200.0)
            } else 458.2
            val basePh = if (lastData != null) {
                (lastData.pH + (r.nextDouble() - 0.5) * 0.1).coerceIn(4.0, 9.0)
            } else 6.5
            val baseEc = if (lastData != null) {
                (lastData.ec + (r.nextDouble() - 0.5) * 0.05).coerceIn(0.2, 3.5)
            } else 1.4
            val baseAirQuality = if (lastData != null) {
                (lastData.airQuality + (r.nextDouble() - 0.5) * 1).coerceIn(5.0, 95.0)
            } else 15.0 // Fresh

            // Random walk NPK
            val baseN = if (lastData != null) (lastData.nitrogen + (r.nextDouble() - 0.5) * 1).coerceIn(5.0, 100.0) else 25.0
            val baseP = if (lastData != null) (lastData.phosphorus + (r.nextDouble() - 0.5) * 1).coerceIn(5.0, 100.0) else 32.0
            val baseK = if (lastData != null) (lastData.potassium + (r.nextDouble() - 0.5) * 1).coerceIn(5.0, 100.0) else 38.0

            return SensorData(
                latitude = offsetLat,
                longitude = offsetLon,
                altitude = 215.4 + (r.nextDouble() - 0.5) * 2,
                satellites = 8 + r.nextInt(3),
                soilMoisture = baseSoilMoist,
                soilTemp = 28.8 + (r.nextDouble() - 0.5) * 0.3,
                airQuality = baseAirQuality,
                light = baseLight,
                airTemp = baseAirTemp,
                humidity = baseHum,
                pressure = basePress,
                humanDetected = if (r.nextDouble() < 0.02) 1 else 0, // 2% chance of human detection
                nitrogen = baseN,
                phosphorus = baseP,
                potassium = baseK,
                pH = basePh,
                ec = baseEc,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
