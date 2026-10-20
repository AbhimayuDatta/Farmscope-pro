package com.example.ui.screens

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.SensorData
import com.example.bluetooth.StatusCalculator
import com.example.bluetooth.StatusLevel
import com.example.ui.viewmodel.FarmViewModel

@Composable
fun LiveRecommendationScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val liveData by viewModel.currentSensorData.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    val healthPercent = liveData?.let { StatusCalculator.getHealthPercentage(it, selectedProfile) } ?: 0
    val animatedHealth by animateIntAsState(
        targetValue = healthPercent,
        animationSpec = tween(durationMillis = 1000),
        label = "health_score"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        item {
            Column {
                Text(
                    "Agronomic Recommendations",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "Dynamic analytics & feedback for crop care",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        if (liveData != null) {
            val data = liveData!!
            val healthLevel = viewModel.getOverallHealthText(data, selectedProfile)
            val healthColor = when (healthLevel) {
                "Excellent" -> Color(0xFF4CAF50)
                "Good" -> Color(0xFF2196F3)
                "Moderate" -> Color(0xFFFFEB3B)
                else -> Color(0xFFF44336)
            }

            // 1. Health Score Circular Dashboard
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("OVERALL CROP HEALTH INDEX", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(healthColor.copy(alpha = 0.15f), Color.Transparent))),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = animatedHealth / 100f,
                                modifier = Modifier.size(130.dp),
                                color = healthColor,
                                strokeWidth = 10.dp,
                                trackColor = Color(0xFF2A2A2A)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$animatedHealth%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = healthLevel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = healthColor
                                )
                            }
                        }
                    }
                }
            }

            // Recommendations Cards Group
            // Water Requirement Recommendation
            item {
                val moistStatus = StatusCalculator.getSoilMoistureStatus(data.soilMoisture, selectedProfile)
                val (level, desc, recColor) = when (moistStatus.level) {
                    StatusLevel.CRITICAL -> Triple("Critical", "Soil moisture is critically low. Active irrigation requested immediately.", Color(0xFFF44336))
                    StatusLevel.LOW_WARNING -> Triple("Moderate", "Soil is drying up. Scheduled watering is highly recommended.", Color(0xFFFFEB3B))
                    StatusLevel.HIGH_WARNING -> Triple("Attention Needed", "Soil is fully waterlogged. Cease watering to prevent root asphyxia.", Color(0xFF2196F3))
                    else -> Triple("Excellent", "Moisture levels are perfect. No irrigation needed at this moment.", Color(0xFF4CAF50))
                }

                RecommendationItemCard(
                    title = "Water Requirement",
                    level = level,
                    description = desc,
                    color = recColor,
                    icon = Icons.Default.WaterDrop
                )
            }

            // Fertilizer Recommendation (EC & NPK)
            item {
                val ecStatus = StatusCalculator.getEcStatus(data.ec, selectedProfile)
                val nStatus = StatusCalculator.getNpkStatus(data.nitrogen, selectedProfile, "N")
                val (level, desc, recColor) = when {
                    ecStatus.level == StatusLevel.CRITICAL || nStatus.level == StatusLevel.CRITICAL ->
                        Triple("Critical", "Critical Nitrogen or general salt deficiency detected in soil. Apply NPK (20-20-20) organic fertilizer.", Color(0xFFF44336))
                    ecStatus.level == StatusLevel.LOW_WARNING ->
                        Triple("Moderate", "Salt/nutrient conductivity is slightly low. Mild nutrient application is advised.", Color(0xFFFFEB3B))
                    ecStatus.level == StatusLevel.HIGH_WARNING ->
                        Triple("Moderate", "Electrical conductivity is high. Avoid additional salt application, irrigate to flush soil.", Color(0xFFFF9800))
                    else ->
                        Triple("Excellent", "NPK and Electrical Conductivity (EC) salts are completely balanced and optimal.", Color(0xFF4CAF50))
                }

                RecommendationItemCard(
                    title = "Fertilizer / Nutrient Soil load",
                    level = level,
                    description = desc,
                    color = recColor,
                    icon = Icons.Default.Science
                )
            }

            // Sunlight Recommendation
            item {
                val lightStatus = StatusCalculator.getLightStatus(data.light, selectedProfile)
                val (level, desc, recColor) = when (lightStatus.level) {
                    StatusLevel.CRITICAL -> Triple("Critical", "Crops are currently in deep shadow or complete darkness. Expose to solar energy.", Color(0xFFF44336))
                    StatusLevel.LOW_WARNING -> Triple("Moderate", "Suboptimal solar radiation. Consider moving shading structures if active.", Color(0xFFFFEB3B))
                    StatusLevel.HIGH_WARNING -> Triple("Moderate", "Excessive direct solar radiation. Shading nets or mist sprays are advised to prevent leaf scorch.", Color(0xFFFF9800))
                    else -> Triple("Excellent", "Photosynthetically active radiation (PAR) levels are optimal for cell division.", Color(0xFF4CAF50))
                }

                RecommendationItemCard(
                    title = "Sunlight Exposure",
                    level = level,
                    description = desc,
                    color = recColor,
                    icon = Icons.Default.WbSunny
                )
            }

            // Humidity Recommendation
            item {
                val humidityStatus = StatusCalculator.getHumidityStatus(data.humidity, selectedProfile)
                val (level, desc, recColor) = when (humidityStatus.level) {
                    StatusLevel.CRITICAL -> Triple("Critical", "Extremely dry or saturated air. Crop is vulnerable to stomatal closure or fungal decay.", Color(0xFFF44336))
                    StatusLevel.LOW_WARNING -> Triple("Moderate", "Dry ambient air. Evapotranspiration is high; crop might dry out quickly.", Color(0xFFFFEB3B))
                    StatusLevel.HIGH_WARNING -> Triple("Moderate", "Slightly high moisture load. Ensure proper canopy spacing and ventilation.", Color(0xFFFF9800))
                    else -> Triple("Excellent", "Vapor Pressure Deficit (VPD) and atmospheric humidity are perfectly optimal.", Color(0xFF4CAF50))
                }

                RecommendationItemCard(
                    title = "Atmospheric Humidity",
                    level = level,
                    description = desc,
                    color = recColor,
                    icon = Icons.Default.Air
                )
            }

            // Temperature Recommendation
            item {
                val tempStatus = StatusCalculator.getTemperatureStatus(data.airTemp, selectedProfile, isSoil = false)
                val (level, desc, recColor) = when (tempStatus.level) {
                    StatusLevel.CRITICAL -> Triple("Critical", "Hazardous crop stress. Heatwave or frost danger. Deploy climate controls.", Color(0xFFF44336))
                    StatusLevel.LOW_WARNING -> Triple("Moderate", "Slightly cool ambient conditions. Metabolic processes are slow.", Color(0xFFFFEB3B))
                    StatusLevel.HIGH_WARNING -> Triple("Moderate", "Slightly warm. Ensure ample water supply to support transpirational cooling.", Color(0xFFFF9800))
                    else -> Triple("Excellent", "Atmospheric heat indices are in the prime metabolic band.", Color(0xFF4CAF50))
                }

                RecommendationItemCard(
                    title = "Microclimate Temperature",
                    level = level,
                    description = desc,
                    color = recColor,
                    icon = Icons.Default.Thermostat
                )
            }

        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = "Waiting", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Waiting for live crop telemetry to evaluate recommendations...",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationItemCard(
    title: String,
    level: String,
    description: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x0FFFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)

                    // Colored tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(level, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(description, color = Color.LightGray, fontSize = 13.sp)
            }
        }
    }
}
