package com.example.bluetooth

import androidx.compose.ui.graphics.Color
import com.example.data.model.PlantProfile

enum class StatusLevel {
    GOOD,      // Green
    OPTIMAL,   // Blue (or Green)
    LOW_WARNING, // Yellow
    HIGH_WARNING, // Orange
    CRITICAL   // Red
}

data class ParameterStatus(
    val text: String,
    val level: StatusLevel,
    val color: Color
)

object StatusCalculator {
    // Standard color palette
    val Green = Color(0xFF4CAF50)
    val Yellow = Color(0xFFFFEB3B)
    val Orange = Color(0xFFFF9800)
    val Red = Color(0xFFF44336)
    val Blue = Color(0xFF2196F3)

    fun getSoilMoistureStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minSoilMoisture
            val max = profile.maxSoilSoilMoisture
            return when {
                value < min * 0.5 -> ParameterStatus("Critically Dry", StatusLevel.CRITICAL, Red)
                value < min -> ParameterStatus("Low Moisture", StatusLevel.LOW_WARNING, Yellow)
                value > max * 1.2 -> ParameterStatus("Waterlogged", StatusLevel.CRITICAL, Red)
                value > max -> ParameterStatus("Wet", StatusLevel.HIGH_WARNING, Blue)
                else -> ParameterStatus("Optimal", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value <= 20.0 -> ParameterStatus("Dry", StatusLevel.CRITICAL, Red)
            value <= 40.0 -> ParameterStatus("Low", StatusLevel.LOW_WARNING, Yellow)
            value <= 60.0 -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            value <= 80.0 -> ParameterStatus("Good", StatusLevel.OPTIMAL, Blue)
            else -> ParameterStatus("Wet", StatusLevel.HIGH_WARNING, Orange)
        }
    }

    fun getTemperatureStatus(value: Double, profile: PlantProfile?, isSoil: Boolean): ParameterStatus {
        val min = if (isSoil) profile?.minSoilTemp else profile?.minAirTemp
        val max = if (isSoil) profile?.maxSoilTemp else profile?.maxAirTemp

        if (min != null && max != null) {
            return when {
                value < min - 5 -> ParameterStatus("Too Cold", StatusLevel.CRITICAL, Red)
                value < min -> ParameterStatus("Cool", StatusLevel.LOW_WARNING, Yellow)
                value > max + 5 -> ParameterStatus("Too Hot", StatusLevel.CRITICAL, Red)
                value > max -> ParameterStatus("Warm", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value < 15.0 -> ParameterStatus("Cold", StatusLevel.CRITICAL, Blue)
            value < 22.0 -> ParameterStatus("Cool", StatusLevel.LOW_WARNING, Yellow)
            value < 30.0 -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            value < 38.0 -> ParameterStatus("Warm", StatusLevel.HIGH_WARNING, Orange)
            else -> ParameterStatus("Hot", StatusLevel.CRITICAL, Red)
        }
    }

    fun getHumidityStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minHumidity
            val max = profile.maxHumidity
            return when {
                value < min -> ParameterStatus("Dry Air", StatusLevel.LOW_WARNING, Yellow)
                value > max -> ParameterStatus("Humid Air", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Comfort", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value <= 30.0 -> ParameterStatus("Dry", StatusLevel.LOW_WARNING, Yellow)
            value <= 60.0 -> ParameterStatus("Comfort", StatusLevel.GOOD, Green)
            value <= 85.0 -> ParameterStatus("Humid", StatusLevel.HIGH_WARNING, Orange)
            else -> ParameterStatus("Very Humid", StatusLevel.CRITICAL, Red)
        }
    }

    fun getPressureStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minPressure
            val max = profile.maxPressure
            return when {
                value < min -> ParameterStatus("Low Pressure", StatusLevel.LOW_WARNING, Yellow)
                value > max -> ParameterStatus("High Pressure", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value < 990.0 -> ParameterStatus("Low", StatusLevel.LOW_WARNING, Yellow)
            value <= 1015.0 -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            value <= 1030.0 -> ParameterStatus("High", StatusLevel.HIGH_WARNING, Orange)
            else -> ParameterStatus("Very High", StatusLevel.CRITICAL, Red)
        }
    }

    fun getLightStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minLight
            val max = profile.maxLight
            return when {
                value < min -> ParameterStatus("Low Light", StatusLevel.LOW_WARNING, Yellow)
                value > max -> ParameterStatus("Too Bright", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Optimal", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value < 100.0 -> ParameterStatus("Dark", StatusLevel.CRITICAL, Red)
            value < 300.0 -> ParameterStatus("Dim", StatusLevel.LOW_WARNING, Yellow)
            value <= 700.0 -> ParameterStatus("Normal", StatusLevel.GOOD, Green)
            value <= 1000.0 -> ParameterStatus("Bright", StatusLevel.OPTIMAL, Blue)
            else -> ParameterStatus("Very Bright", StatusLevel.HIGH_WARNING, Orange)
        }
    }

    fun getAirQualityStatus(value: Double): ParameterStatus {
        // Lower % index is typically cleaner in some definitions, but let's assume standard index: 0-25 Fresh, 26-50 Good, 51-70 Moderate, 71-90 Poor, 91+ Hazardous
        return when {
            value <= 15.0 -> ParameterStatus("Fresh", StatusLevel.GOOD, Green)
            value <= 35.0 -> ParameterStatus("Good", StatusLevel.OPTIMAL, Blue)
            value <= 55.0 -> ParameterStatus("Moderate", StatusLevel.LOW_WARNING, Yellow)
            value <= 75.0 -> ParameterStatus("Poor", StatusLevel.HIGH_WARNING, Orange)
            else -> ParameterStatus("Hazardous", StatusLevel.CRITICAL, Red)
        }
    }

    fun getPhStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minPh
            val max = profile.maxPh
            return when {
                value < min -> ParameterStatus("Too Acidic", StatusLevel.CRITICAL, Red)
                value > max -> ParameterStatus("Too Alkaline", StatusLevel.CRITICAL, Orange)
                else -> ParameterStatus("Optimal pH", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value < 6.0 -> ParameterStatus("Acidic", StatusLevel.LOW_WARNING, Yellow)
            value <= 7.5 -> ParameterStatus("Neutral", StatusLevel.GOOD, Green)
            else -> ParameterStatus("Alkaline", StatusLevel.HIGH_WARNING, Orange)
        }
    }

    fun getEcStatus(value: Double, profile: PlantProfile?): ParameterStatus {
        if (profile != null) {
            val min = profile.minEc
            val max = profile.maxEc
            return when {
                value < min -> ParameterStatus("Low EC", StatusLevel.LOW_WARNING, Yellow)
                value > max -> ParameterStatus("High EC", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Optimal EC", StatusLevel.GOOD, Green)
            }
        }
        return when {
            value < 0.8 -> ParameterStatus("Low", StatusLevel.LOW_WARNING, Yellow)
            value <= 2.0 -> ParameterStatus("Optimal", StatusLevel.GOOD, Green)
            else -> ParameterStatus("High", StatusLevel.HIGH_WARNING, Orange)
        }
    }

    fun getNpkStatus(value: Double, profile: PlantProfile?, paramName: String): ParameterStatus {
        // Param can be "N", "P", "K"
        val min = when (paramName) {
            "N" -> profile?.minNitrogen
            "P" -> profile?.minPhosphorus
            else -> profile?.minPotassium
        }
        val max = when (paramName) {
            "N" -> profile?.maxNitrogen
            "P" -> profile?.maxPhosphorus
            else -> profile?.maxPotassium
        }

        if (min != null && max != null) {
            return when {
                value < min -> ParameterStatus("Deficient", StatusLevel.CRITICAL, Red)
                value > max -> ParameterStatus("Excessive", StatusLevel.HIGH_WARNING, Orange)
                else -> ParameterStatus("Optimal $paramName", StatusLevel.GOOD, Green)
            }
        }

        return when {
            value < 15.0 -> ParameterStatus("Low", StatusLevel.LOW_WARNING, Yellow)
            value <= 40.0 -> ParameterStatus("Medium", StatusLevel.GOOD, Green)
            value <= 75.0 -> ParameterStatus("High", StatusLevel.OPTIMAL, Blue)
            else -> ParameterStatus("Excellent", StatusLevel.GOOD, Green)
        }
    }

    // Recommendation generator logic
    fun getHealthPercentage(data: SensorData, profile: PlantProfile?): Int {
        var scoreSum = 0
        var count = 0

        fun evaluateParam(isOptimal: Boolean, isCritical: Boolean) {
            scoreSum += if (isOptimal) 100 else if (isCritical) 20 else 60
            count++
        }

        val moisture = getSoilMoistureStatus(data.soilMoisture, profile)
        evaluateParam(moisture.level == StatusLevel.GOOD || moisture.level == StatusLevel.OPTIMAL, moisture.level == StatusLevel.CRITICAL)

        val soilTemp = getTemperatureStatus(data.soilTemp, profile, true)
        evaluateParam(soilTemp.level == StatusLevel.GOOD || soilTemp.level == StatusLevel.OPTIMAL, soilTemp.level == StatusLevel.CRITICAL)

        val airTemp = getTemperatureStatus(data.airTemp, profile, false)
        evaluateParam(airTemp.level == StatusLevel.GOOD || airTemp.level == StatusLevel.OPTIMAL, airTemp.level == StatusLevel.CRITICAL)

        val humidity = getHumidityStatus(data.humidity, profile)
        evaluateParam(humidity.level == StatusLevel.GOOD, humidity.level == StatusLevel.CRITICAL)

        val light = getLightStatus(data.light, profile)
        evaluateParam(light.level == StatusLevel.GOOD || light.level == StatusLevel.OPTIMAL, light.level == StatusLevel.CRITICAL)

        val ph = getPhStatus(data.pH, profile)
        evaluateParam(ph.level == StatusLevel.GOOD, ph.level == StatusLevel.CRITICAL)

        val ec = getEcStatus(data.ec, profile)
        evaluateParam(ec.level == StatusLevel.GOOD, ec.level == StatusLevel.CRITICAL)

        return (scoreSum / count.toDouble()).toInt().coerceIn(10, 100)
    }
}
