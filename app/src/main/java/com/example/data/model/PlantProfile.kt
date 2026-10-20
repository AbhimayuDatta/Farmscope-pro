package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_profiles")
data class PlantProfile(
    @PrimaryKey val name: String,
    val minSoilMoisture: Double,
    val maxSoilSoilMoisture: Double, // Matches name "Moisture" in UI
    val minSoilTemp: Double,
    val maxSoilTemp: Double,
    val minAirTemp: Double,
    val maxAirTemp: Double,
    val minHumidity: Double,
    val maxHumidity: Double,
    val minLight: Double,
    val maxLight: Double,
    val minPressure: Double,
    val maxPressure: Double,
    val minNitrogen: Double,
    val maxNitrogen: Double,
    val minPhosphorus: Double,
    val maxPhosphorus: Double,
    val minPotassium: Double,
    val maxPotassium: Double,
    val minPh: Double,
    val maxPh: Double,
    val minEc: Double,
    val maxEc: Double
) {
    companion object {
        fun getDefaults(): List<PlantProfile> = listOf(
            PlantProfile(
                name = "Tomato",
                minSoilMoisture = 60.0, maxSoilSoilMoisture = 80.0,
                minSoilTemp = 20.0, maxSoilTemp = 30.0,
                minAirTemp = 18.0, maxAirTemp = 25.0,
                minHumidity = 50.0, maxHumidity = 70.0,
                minLight = 400.0, maxLight = 800.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 20.0, maxNitrogen = 40.0,
                minPhosphorus = 30.0, maxPhosphorus = 50.0,
                minPotassium = 30.0, maxPotassium = 50.0,
                minPh = 6.0, maxPh = 6.8,
                minEc = 1.5, maxEc = 2.5
            ),
            PlantProfile(
                name = "Potato",
                minSoilMoisture = 50.0, maxSoilSoilMoisture = 70.0,
                minSoilTemp = 15.0, maxSoilTemp = 22.0,
                minAirTemp = 15.0, maxAirTemp = 20.0,
                minHumidity = 60.0, maxHumidity = 80.0,
                minLight = 300.0, maxLight = 600.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 30.0, maxNitrogen = 50.0,
                minPhosphorus = 20.0, maxPhosphorus = 40.0,
                minPotassium = 40.0, maxPotassium = 60.0,
                minPh = 5.5, maxPh = 6.2,
                minEc = 1.2, maxEc = 2.0
            ),
            PlantProfile(
                name = "Wheat",
                minSoilMoisture = 40.0, maxSoilSoilMoisture = 60.0,
                minSoilTemp = 10.0, maxSoilTemp = 20.0,
                minAirTemp = 12.0, maxAirTemp = 22.0,
                minHumidity = 40.0, maxHumidity = 65.0,
                minLight = 500.0, maxLight = 900.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 40.0, maxNitrogen = 60.0,
                minPhosphorus = 25.0, maxPhosphorus = 45.0,
                minPotassium = 25.0, maxPotassium = 45.0,
                minPh = 6.0, maxPh = 7.0,
                minEc = 1.0, maxEc = 1.8
            ),
            PlantProfile(
                name = "Rice",
                minSoilMoisture = 80.0, maxSoilSoilMoisture = 100.0,
                minSoilTemp = 22.0, maxSoilTemp = 35.0,
                minAirTemp = 25.0, maxAirTemp = 33.0,
                minHumidity = 70.0, maxHumidity = 90.0,
                minLight = 600.0, maxLight = 1000.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 50.0, maxNitrogen = 70.0,
                minPhosphorus = 30.0, maxPhosphorus = 50.0,
                minPotassium = 30.0, maxPotassium = 50.0,
                minPh = 5.5, maxPh = 6.5,
                minEc = 1.5, maxEc = 2.2
            ),
            PlantProfile(
                name = "Sugarcane",
                minSoilMoisture = 70.0, maxSoilSoilMoisture = 90.0,
                minSoilTemp = 25.0, maxSoilTemp = 35.0,
                minAirTemp = 24.0, maxAirTemp = 30.0,
                minHumidity = 60.0, maxHumidity = 85.0,
                minLight = 600.0, maxLight = 1100.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 60.0, maxNitrogen = 80.0,
                minPhosphorus = 40.0, maxPhosphorus = 60.0,
                minPotassium = 50.0, maxPotassium = 70.0,
                minPh = 6.0, maxPh = 7.5,
                minEc = 1.8, maxEc = 2.6
            ),
            PlantProfile(
                name = "Cotton",
                minSoilMoisture = 45.0, maxSoilSoilMoisture = 65.0,
                minSoilTemp = 20.0, maxSoilTemp = 30.0,
                minAirTemp = 22.0, maxAirTemp = 32.0,
                minHumidity = 50.0, maxHumidity = 70.0,
                minLight = 700.0, maxLight = 1200.0,
                minPressure = 980.0, maxPressure = 1024.0,
                minNitrogen = 40.0, maxNitrogen = 60.0,
                minPhosphorus = 20.0, maxPhosphorus = 40.0,
                minPotassium = 30.0, maxPotassium = 50.0,
                minPh = 5.8, maxPh = 7.2,
                minEc = 1.2, maxEc = 2.2
            )
        )
    }
}
