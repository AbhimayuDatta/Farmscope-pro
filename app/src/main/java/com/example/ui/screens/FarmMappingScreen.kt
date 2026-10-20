package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.bluetooth.SensorData
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.bluetooth.StatusCalculator
import com.example.data.model.SensorLog
import com.example.ui.components.CustomInteractiveChart
import com.example.ui.components.GisMapCanvas
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FarmMappingScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val matrixState by viewModel.matrixCellsState.collectAsState()
    val selectedCellId by viewModel.selectedCellId.collectAsState()
    val cellHistory by viewModel.selectedCellHistory.collectAsState()
    val activeData by viewModel.currentSensorData.collectAsState()
    val heatmapParam by viewModel.heatmapParameter.collectAsState()

    var showCellActionsDialog by remember { mutableStateOf(false) }
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Heatmap Grid, 1 = Cell History & Charts, 2 = GIS Tactical Radar Map

    // Parameters available for Heatmap
    val heatmapParamsList = listOf(
        "Soil Moisture", "Temperature", "Humidity", "Light",
        "Air Quality", "Nitrogen", "Phosphorus", "Potassium", "pH", "EC", "Health Score"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Title Bar
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Farm Mapping & GIS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "4x4 Matrix coordinates telemetry",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sub Tabs switcher
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = Color(0x11FFFFFF)) }
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("Grid Heatmap", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                Text("Cell History & Charts", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = activeSubTab == 2, onClick = { activeSubTab = 2 }) {
                Text("GIS Radar Map", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Animated Content based on selected Sub Tab
        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTab) {
                0 -> {
                    // Heatmap & Grid
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        // 1. Selector for Heatmap Parameter
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("HEATMAP ATTRIBUTE FILTER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        heatmapParamsList.forEach { param ->
                                            val isSelected = heatmapParam == param
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2C))
                                                    .clickable { viewModel.heatmapParameter.value = param }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = param,
                                                    color = if (isSelected) Color.Black else Color.LightGray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. 4x4 Grid Matrix
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("FARM 16-POINT MATRIX GRID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    val rows = listOf("A", "B", "C", "D")
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        for (r in rows) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                for (c in 1..4) {
                                                    val cellId = "$r$c"
                                                    val latestLog = matrixState[cellId]
                                                    val isSelected = selectedCellId == cellId

                                                    // Calculate heatmap color for this cell
                                                    val cellColor = getHeatmapColorForCell(latestLog, heatmapParam)

                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .aspectRatio(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(cellColor.copy(alpha = 0.25f))
                                                            .border(
                                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else cellColor.copy(alpha = 0.7f),
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            .clickable {
                                                                viewModel.selectedCellId.value = cellId
                                                                viewModel.saveCellReading(cellId, isAppend = true)
                                                                viewModel.loadCellHistory(cellId)
                                                                android.widget.Toast.makeText(context, "Logged telemetry at grid point $cellId", android.widget.Toast.LENGTH_SHORT).show()
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = cellId,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 15.sp,
                                                                color = if (latestLog != null) Color.White else Color.Gray
                                                            )
                                                            if (latestLog != null) {
                                                                val score = getHeatmapValueForCell(latestLog, "Health Score").toInt()
                                                                Text(
                                                                    text = "$score%",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = cellColor
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = "empty",
                                                                    fontSize = 9.sp,
                                                                    color = Color.DarkGray
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    // Heatmap Legend
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LegendChip("Critical/Dry", Color(0xFFF44336))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        LegendChip("Low/Deficient", Color(0xFFFF9800))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        LegendChip("Optimal", Color(0xFF4CAF50))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        LegendChip("Wet/Excessive", Color(0xFF2196F3))
                                    }
                                }
                            }
                        }

                        // Cell quick details block if selected
                        item {
                            selectedCellId?.let { cellId ->
                                val cellLog = matrixState[cellId]
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "SELECTED CELL: COORDINATE $cellId",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (cellLog != null) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Plant: ${cellLog.plantName}", fontWeight = FontWeight.Bold, color = Color.White)
                                                    Text("Status: ${cellLog.status}", color = Color.LightGray, fontSize = 13.sp)
                                                }
                                                Text(
                                                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(cellLog.timestamp)),
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.deleteCellHistory(cellId)
                                                        android.widget.Toast.makeText(context, "Cleared logs for grid point $cellId", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Clear Grid Point Logs", fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Text(
                                                "No readings saved for cell $cellId yet. Tap the cell to log your current sensor telemetry data here.",
                                                color = Color.Gray,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Cell History & Graphs
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        item {
                            Text(
                                text = "History: Coordinate Grid ${selectedCellId ?: "A1"}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (cellHistory.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color(0xFF1E1E1E), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No previous logs saved for this coordinate point.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            // Render canvas charts for key metrics
                            item {
                                Text("Soil Moisture Progress (%)", fontSize = 13.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                CustomInteractiveChart(
                                    data = cellHistory.map { it.soilMoisture }.reversed(),
                                    labels = cellHistory.take(4).map { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp)) }.reversed(),
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    lineColor = Color(0xFF00BCD4),
                                    gradientColors = listOf(Color(0x3300BCD4), Color(0x0000BCD4)),
                                    unit = "%"
                                )
                            }

                            item {
                                Text("Soil Temperature (°C)", fontSize = 13.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                CustomInteractiveChart(
                                    data = cellHistory.map { it.soilTemp }.reversed(),
                                    labels = cellHistory.take(4).map { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it.timestamp)) }.reversed(),
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    lineColor = Color(0xFFFF9800),
                                    gradientColors = listOf(Color(0x33FF9800), Color(0x00FF9800)),
                                    unit = "°C"
                                )
                            }

                            // Render History logs list
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Recorded Logs History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }

                            items(cellHistory) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Saved for: ${log.plantName}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Moisture: ${String.format("%.1f", log.soilMoisture)}%  |  Temp: ${String.format("%.1f", log.soilTemp)}°C  |  pH: ${String.format("%.2f", log.pH)}",
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )
                                        Text(
                                            text = "NPK: ${log.nitrogen.toInt()}-${log.phosphorus.toInt()}-${log.potassium.toInt()}  |  EC: ${String.format("%.2f", log.ec)}  |  Health: ${log.status}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // GIS Canvas Radar Map
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "GIS Survey Location Tracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Get all coordinate logs from Room
                        var allRoomLogs by remember { mutableStateOf<List<SensorLog>>(emptyList()) }
                        LaunchedEffect(matrixState) {
                            allRoomLogs = viewModel.repository.getAllLogsList()
                        }

                        GisMapCanvas(
                            currentData = activeData,
                            loggedPositions = allRoomLogs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                        )
                        Spacer(modifier = Modifier.height(96.dp))
                    }
                }
            }
        }
    }

    // Cell action sheet / dialog
    if (showCellActionsDialog && selectedCellId != null) {
        val cellId = selectedCellId!!
        val isOccupied = matrixState.containsKey(cellId)

        AlertDialog(
            onDismissRequest = { showCellActionsDialog = false },
            title = { Text("Grid Point $cellId options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isOccupied) "This cell currently holds active logged telemetry metrics. Choose an operation:"
                        else "No current telemetry saved here. Log current live receiver stats to $cellId:"
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (activeData == null) {
                        Text("Please connect to a live telemetry stream or enable Bluetooth Simulation before saving coordinates.", color = Color.Gray, fontSize = 12.sp)
                    }

                    // Append Reading Button
                    Button(
                        onClick = {
                            viewModel.saveCellReading(cellId, isAppend = true)
                            showCellActionsDialog = false
                        },
                        enabled = activeData != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Current Reading (Append)")
                    }

                    // Overwrite Reading Button
                    if (isOccupied) {
                        Button(
                            onClick = {
                                viewModel.saveCellReading(cellId, isAppend = false)
                                showCellActionsDialog = false
                            },
                            enabled = activeData != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Overwrite Cell Reading")
                        }
                    }

                    // Delete cell history button
                    if (isOccupied) {
                        Button(
                            onClick = {
                                viewModel.deleteCellHistory(cellId)
                                showCellActionsDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Cell logs")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCellActionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LegendChip(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.Gray, fontSize = 9.sp)
    }
}

// Map helper to calculate heatmap value
fun getHeatmapValueForCell(log: SensorLog?, param: String): Double {
    if (log == null) return 0.0
    return when (param) {
        "Soil Moisture" -> log.soilMoisture
        "Temperature" -> log.soilTemp
        "Humidity" -> log.humidity
        "Light" -> log.light
        "Air Quality" -> log.airQuality
        "Nitrogen" -> log.nitrogen
        "Phosphorus" -> log.phosphorus
        "Potassium" -> log.potassium
        "pH" -> log.pH
        "EC" -> log.ec
        else -> { // Health Score
            // Recompute health percentage
            val parsed = SensorData(
                latitude = log.latitude, longitude = log.longitude, altitude = log.altitude, satellites = log.satellites,
                soilMoisture = log.soilMoisture, soilTemp = log.soilTemp, airQuality = log.airQuality, light = log.light,
                airTemp = log.airTemp, humidity = log.humidity, pressure = log.pressure, humanDetected = log.humanDetected,
                nitrogen = log.nitrogen, phosphorus = log.phosphorus, potassium = log.potassium, pH = log.pH, ec = log.ec
            )
            StatusCalculator.getHealthPercentage(parsed, null).toDouble()
        }
    }
}

// Map helper to calculate heatmap color for grid cells
fun getHeatmapColorForCell(log: SensorLog?, param: String): Color {
    if (log == null) return Color.DarkGray
    val value = getHeatmapValueForCell(log, param)
    return when (param) {
        "Soil Moisture" -> {
            when {
                value <= 20.0 -> Color(0xFFF44336) // Dry (Red)
                value <= 40.0 -> Color(0xFFFF9800) // Low (Orange)
                value <= 80.0 -> Color(0xFF4CAF50) // Optimal (Green)
                else -> Color(0xFF2196F3)          // Wet (Blue)
            }
        }
        "Temperature" -> {
            when {
                value < 15.0 -> Color(0xFF2196F3)  // Cold (Blue)
                value < 22.0 -> Color(0xFF4CAF50)  // Cool/Normal (Green)
                value < 35.0 -> Color(0xFFFF9800)  // Warm (Orange)
                else -> Color(0xFFF44336)          // Hot (Red)
            }
        }
        "pH" -> {
            when {
                value < 6.0 -> Color(0xFFFF9800)   // Acidic (Orange)
                value <= 7.5 -> Color(0xFF4CAF50)  // Neutral (Green)
                else -> Color(0xFF2196F3)          // Alkaline (Blue)
            }
        }
        "Health Score" -> {
            when {
                value >= 80 -> Color(0xFF4CAF50)   // Healthy (Green)
                value >= 60 -> Color(0xFFFFEB3B)   // Moderate (Yellow)
                value >= 40 -> Color(0xFFFF9800)   // Low (Orange)
                else -> Color(0xFFF44336)          // Critical (Red)
            }
        }
        else -> { // Default standard gradient mapping
            when {
                value < 20.0 -> Color(0xFFF44336)
                value < 50.0 -> Color(0xFFFF9800)
                value <= 85.0 -> Color(0xFF4CAF50)
                else -> Color(0xFF2196F3)
            }
        }
    }
}
