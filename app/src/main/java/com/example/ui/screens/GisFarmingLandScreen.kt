package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.bluetooth.ConnectionState
import com.example.bluetooth.SensorData
import com.example.data.model.SensorLog
import com.example.ui.viewmodel.FarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GisFarmingLandScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val matrixState by viewModel.matrixCellsState.collectAsState()
    val activeData by viewModel.currentSensorData.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()
    val connectionState by viewModel.bluetoothManager.connectionState.collectAsState()
    val customMapImageUri by viewModel.customMapImageUri.collectAsState()

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateCustomMapImageUri(uri.toString())
        }
    }

    var selectedGridCell by remember { mutableStateOf<String?>("A1") }
    var activeSubTab by remember { mutableStateOf(0) } // 0 = Matrix Land Map, 1 = Matrix State List

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Title block
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Field GIS Grid Map",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "Co-registered 16-point agricultural mapping",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Quick display of active profile being evaluated
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        selectedProfile?.name ?: "Tomato",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = activeSubTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = { HorizontalDivider(color = Color(0x11FFFFFF)) }
        ) {
            Tab(selected = activeSubTab == 0, onClick = { activeSubTab = 0 }) {
                Text("Interactive Land Grid", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = activeSubTab == 1, onClick = { activeSubTab = 1 }) {
                val loggedCount = matrixState.size
                Text("Grid Status List ($loggedCount/16)", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == 0) {
            // Interactive Land Map view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Map Background Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AGRICULTURAL LAND COVER MAP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { imageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change Map Image (PNG)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        if (customMapImageUri != null) {
                            IconButton(
                                onClick = { viewModel.updateCustomMapImageUri(null) },
                                modifier = Modifier.size(28.dp)
                                    .background(Color(0x22FF5252), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0x44FF5252), RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset to default aerial map", tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Map Background Grid Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                        .background(Color(0xFF101010))
                ) {
                    // 1. Aerial Satellite Land Background Image
                    if (customMapImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = java.io.File(customMapImageUri!!)),
                            contentDescription = "Custom farming land map background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.85f
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_farming_land_1784237603550),
                            contentDescription = "Farming land map background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.85f
                        )
                    }

                    // 2. Translucent Matrix Grid Lines Overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw 3 vertical grid split lines
                        for (i in 1..3) {
                            val x = w * (i / 4f)
                            drawLine(
                                color = Color(0x33FFFFFF),
                                start = Offset(x, 0f),
                                end = Offset(x, h),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw 3 horizontal grid split lines
                        for (i in 1..3) {
                            val y = h * (i / 4f)
                            drawLine(
                                color = Color(0x33FFFFFF),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // 3. Coordinate Grid Interactive Markers
                    val rows = listOf("A", "B", "C", "D")
                    val cols = listOf(1, 2, 3, 4)

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val w = maxWidth
                        val h = maxHeight

                        for (rIndex in 0..3) {
                            for (cIndex in 0..3) {
                                val cellId = "${rows[rIndex]}${cols[cIndex]}"
                                val latestLog = matrixState[cellId]

                                // Center position coordinates for each grid coordinate sector
                                val px = w * ((cIndex + 0.5f) / 4f)
                                val py = h * ((rIndex + 0.5f) / 4f)

                                val isSelected = selectedGridCell == cellId

                                // Evaluate deficiencies for this coordinate
                                val parameterStatuses = latestLog?.let { getParameterStatuses(it, selectedProfile) } ?: emptyList()
                                val deficienciesCount = parameterStatuses.count { !it.isOk }

                                val markerColor = when {
                                    latestLog == null -> Color(0xFF6B6B6B) // Unlogged / Gray
                                    deficienciesCount >= 3 -> Color(0xFFF44336) // Critical deficiencies (Red)
                                    deficienciesCount >= 1 -> Color(0xFFFF9800) // Moderate deficiencies (Orange)
                                    else -> Color(0xFF4CAF50) // Optimal (Green)
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = px - 22.dp, y = py - 22.dp)
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color.White 
                                            else markerColor.copy(alpha = 0.3f)
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) markerColor else markerColor.copy(alpha = 0.8f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedGridCell = cellId
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = cellId,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                        if (latestLog != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(markerColor)
                                            )
                                        } else {
                                            Text(
                                                "no log",
                                                fontSize = 7.sp,
                                                color = if (isSelected) Color.DarkGray else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Grid Point Deficiency & Evaluation Analysis card
                val cellId = selectedGridCell
                if (cellId != null) {
                    val latestLog = matrixState[cellId]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxHeight()
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GridOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Grid Point $cellId Analysis",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = selectedProfile?.name ?: "No Profile Selected",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { selectedGridCell = null },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0x11FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                            if (latestLog != null) {
                                val parameterStatuses = getParameterStatuses(latestLog, selectedProfile)
                                val deficiencies = parameterStatuses.filter { !it.isOk }

                                val healthScore = (100 - (deficiencies.size * 10)).coerceIn(0, 100)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Overall Soil Health Index", fontSize = 11.sp, color = Color.Gray)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$healthScore%",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = when {
                                                    healthScore >= 85 -> Color(0xFF4CAF50)
                                                    healthScore >= 60 -> Color(0xFFFF9800)
                                                    else -> Color(0xFFF44336)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when {
                                                    healthScore >= 85 -> "Optimal Condition"
                                                    healthScore >= 60 -> "Deficient Warning"
                                                    else -> "Critical Deficiency"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    healthScore >= 85 -> Color(0xFF4CAF50)
                                                    healthScore >= 60 -> Color(0xFFFF9800)
                                                    else -> Color(0xFFF44336)
                                                }
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.deleteCellHistory(cellId)
                                            android.widget.Toast.makeText(context, "Cleared logs for grid point $cellId", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear Logs", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    "PARAMETER ANALYSIS COMPARED TO TARGET VALUES:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(parameterStatuses) { param ->
                                        val statusColor = when {
                                            param.isOk -> Color(0xFF4CAF50)
                                            param.isLow -> Color(0xFFFF9800)
                                            else -> Color(0xFF2196F3) // High / Wet (Blue) or Red/Orange
                                        }

                                        val itemBgColor = when {
                                            param.isOk -> Color(0x0A4CAF50)
                                            else -> Color(0x0AF44336)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(itemBgColor)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (param.isOk) Color(0x114CAF50) else Color(0x1BF44336),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (param.isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = statusColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        param.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (param.isOk) "Optimal" else if (param.isLow) "Too Low" else "Too High",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Recorded: ${String.format("%.1f", param.currentValue)}${param.unit}",
                                                    fontSize = 11.sp,
                                                    color = Color.LightGray
                                                )
                                                Text(
                                                    text = "Target: ${String.format("%.1f", param.minValue)} - ${String.format("%.1f", param.maxValue)}${param.unit}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            if (!param.isOk) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Problem: " + param.problemText,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFEF9A9A),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Action: " + param.recommendation,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFFFCC80),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Cell is unlogged
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "No logged telemetry at Grid Sector $cellId",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "The currently selected Plant Profile (${selectedProfile?.name ?: "Tomato"}) defines thresholds. Click below to log the current live sensor values to this sector on the map immediately.",
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                if (connectionState == ConnectionState.CONNECTED || viewModel.bluetoothManager.isSimulatorMode.value) {
                                                    viewModel.saveCellReading(cellId, isAppend = true)
                                                    android.widget.Toast.makeText(context, "Logged current sensor telemetry into grid sector $cellId!", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    android.widget.Toast.makeText(context, "Bluetooth is offline. Connect or turn on Simulator to log telemetry.", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Log Current Sensor Data Here", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 96.dp)
                            .background(Color(0xFF161616), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Tap on any coordinate circle inside the agricultural map above to evaluate and show what is deficient in that zone.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        } else {
            // Recorded status list of all 16 coordinates
            val rows = listOf("A", "B", "C", "D")
            val cols = listOf(1, 2, 3, 4)
            val matrixList = mutableListOf<String>()
            for (r in rows) {
                for (c in cols) {
                    matrixList.add("$r$c")
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(matrixList) { cellId ->
                    val latestLog = matrixState[cellId]
                    val parameterStatuses = latestLog?.let { getParameterStatuses(it, selectedProfile) } ?: emptyList()
                    val deficiencies = parameterStatuses.filter { !it.isOk }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGridCell = cellId
                                activeSubTab = 0
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x11FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val indicatorColor = when {
                                    latestLog == null -> Color(0xFF6B6B6B)
                                    deficiencies.size >= 3 -> Color(0xFFF44336)
                                    deficiencies.isNotEmpty() -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(indicatorColor.copy(alpha = 0.2f))
                                        .border(1.dp, indicatorColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cellId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = indicatorColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Grid point $cellId",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (latestLog != null) "Logged: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(latestLog.timestamp)) else "No telemetry saved yet",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (latestLog != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (deficiencies.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFF9800),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${deficiencies.size} Issues",
                                            color = Color(0xFFFF9800),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Optimal",
                                            color = Color(0xFF4CAF50),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (connectionState == ConnectionState.CONNECTED || viewModel.bluetoothManager.isSimulatorMode.value) {
                                            viewModel.saveCellReading(cellId, isAppend = true)
                                            android.widget.Toast.makeText(context, "Logged telemetry at grid point $cellId!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Bluetooth is offline. Connect or turn on Simulator to log telemetry.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E2E2E)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Log", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data class to represent deficiencies
data class Deficiency(
    val paramName: String,
    val description: String,
    val recommendation: String
)

data class ParameterStatus(
    val name: String,
    val currentValue: Double,
    val minValue: Double,
    val maxValue: Double,
    val unit: String,
    val isOk: Boolean,
    val isLow: Boolean,
    val isHigh: Boolean,
    val problemText: String = "",
    val recommendation: String = ""
)

fun getParameterStatuses(spot: SensorLog, profile: com.example.data.model.PlantProfile?): List<ParameterStatus> {
    if (profile == null) return emptyList()

    val list = mutableListOf<ParameterStatus>()

    // 1. Soil Moisture
    val smVal = spot.soilMoisture
    val smMin = profile.minSoilMoisture
    val smMax = profile.maxSoilSoilMoisture
    val smOk = smVal in smMin..smMax
    list.add(
        ParameterStatus(
            name = "Soil Moisture",
            currentValue = smVal,
            minValue = smMin,
            maxValue = smMax,
            unit = "%",
            isOk = smOk,
            isLow = smVal < smMin,
            isHigh = smVal > smMax,
            problemText = if (smVal < smMin) "Soil moisture level is dangerously dry." else if (smVal > smMax) "Soil is waterlogged/excessively wet." else "Optimal hydration.",
            recommendation = if (smVal < smMin) "Activate drip irrigation immediately in this sector." else if (smVal > smMax) "Pause irrigation and verify field soil drainage outlets." else "Maintain current irrigation schedule."
        )
    )

    // 2. Nitrogen
    val nVal = spot.nitrogen
    val nMin = profile.minNitrogen
    val nMax = profile.maxNitrogen
    val nOk = nVal in nMin..nMax
    list.add(
        ParameterStatus(
            name = "Nitrogen (N)",
            currentValue = nVal,
            minValue = nMin,
            maxValue = nMax,
            unit = " ppm",
            isOk = nOk,
            isLow = nVal < nMin,
            isHigh = nVal > nMax,
            problemText = if (nVal < nMin) "Nitrogen level is low (stunted vegetative growth)." else if (nVal > nMax) "Nitrogen is excessive (salt buildup risk)." else "Healthy nitrogen balance.",
            recommendation = if (nVal < nMin) "Incorporate urea, blood meal, or composted manure." else if (nVal > nMax) "Flush soil with clean water to dilute mineral build-ups." else "No fertilizer adjustment needed."
        )
    )

    // 3. Phosphorus
    val pVal = spot.phosphorus
    val pMin = profile.minPhosphorus
    val pMax = profile.maxPhosphorus
    val pOk = pVal in pMin..pMax
    list.add(
        ParameterStatus(
            name = "Phosphorus (P)",
            currentValue = pVal,
            minValue = pMin,
            maxValue = pMax,
            unit = " ppm",
            isOk = pOk,
            isLow = pVal < pMin,
            isHigh = pVal > pMax,
            problemText = if (pVal < pMin) "Phosphorus level is deficient (poor root/flower development)." else if (pVal > pMax) "Phosphorus level is high (may block zinc absorption)." else "Perfect phosphorus availability.",
            recommendation = if (pVal < pMin) "Apply rock phosphate or bone meal in this zone." else if (pVal > pMax) "Avoid phosphate fertilizers; introduce mycorrhizal fungi." else "Adequate phosphorus levels."
        )
    )

    // 4. Potassium
    val kVal = spot.potassium
    val kMin = profile.minPotassium
    val kMax = profile.maxPotassium
    val kOk = kVal in kMin..kMax
    list.add(
        ParameterStatus(
            name = "Potassium (K)",
            currentValue = kVal,
            minValue = kMin,
            maxValue = kMax,
            unit = " ppm",
            isOk = kOk,
            isLow = kVal < kMin,
            isHigh = kVal > kMax,
            problemText = if (kVal < kMin) "Potassium level is deficient (weak stems, disease vulnerability)." else if (kVal > kMax) "Potassium is excessive (antagonizes magnesium)." else "Optimal potassium nutrition.",
            recommendation = if (kVal < kMin) "Apply potash, kelp meal, or wood ash." else if (kVal > kMax) "Flush soil and avoid high-potash nutrition blends." else "Maintain current soil composition."
        )
    )

    // 5. pH Level
    val phVal = spot.pH
    val phMin = profile.minPh
    val phMax = profile.maxPh
    val phOk = phVal in phMin..phMax
    list.add(
        ParameterStatus(
            name = "Soil pH",
            currentValue = phVal,
            minValue = phMin,
            maxValue = phMax,
            unit = "",
            isOk = phOk,
            isLow = phVal < phMin,
            isHigh = phVal > phMax,
            problemText = if (phVal < phMin) "Soil pH is too acidic (restricts nutrient uptake)." else if (phVal > phMax) "Soil pH is too alkaline (causes iron/zinc chlorosis)." else "Optimal neutral/profile pH range.",
            recommendation = if (phVal < phMin) "Add agricultural lime (calcium carbonate) to raise pH." else if (phVal > phMax) "Apply agricultural sulfur or peat moss to reduce pH." else "Soil pH is excellent for this crop profile."
        )
    )

    // 6. Electrical Conductivity
    val ecVal = spot.ec
    val ecMin = profile.minEc
    val ecMax = profile.maxEc
    val ecOk = ecVal in ecMin..ecMax
    list.add(
        ParameterStatus(
            name = "Soil EC",
            currentValue = ecVal,
            minValue = ecMin,
            maxValue = ecMax,
            unit = " dS/m",
            isOk = ecOk,
            isLow = ecVal < ecMin,
            isHigh = ecVal > ecMax,
            problemText = if (ecVal < ecMin) "Electrical conductivity is low (low active soil minerals)." else if (ecVal > ecMax) "EC is high (dangerous soil salinity stress)." else "Healthy dissolved salt ratio.",
            recommendation = if (ecVal < ecMin) "Foliar feed or apply balanced liquid organic fertilizer." else if (ecVal > ecMax) "Heavily irrigate with clean fresh water to leach out salt." else "Electrical conductivity is in the green zone."
        )
    )

    // 7. Soil Temperature
    val stVal = spot.soilTemp
    val stMin = profile.minSoilTemp
    val stMax = profile.maxSoilTemp
    val stOk = stVal in stMin..stMax
    list.add(
        ParameterStatus(
            name = "Soil Temp",
            currentValue = stVal,
            minValue = stMin,
            maxValue = stMax,
            unit = "°C",
            isOk = stOk,
            isLow = stVal < stMin,
            isHigh = stVal > stMax,
            problemText = if (stVal < stMin) "Soil temperature is cold (slows down germination)." else if (stVal > stMax) "Soil temperature is too hot (harms beneficial microbes)." else "Comfortable root temperature.",
            recommendation = if (stVal < stMin) "Apply dark mulch sheets to absorb solar warmth." else if (stVal > stMax) "Utilize white reflective covers or increase canopy shading." else "No action required."
        )
    )

    // 8. Air Temperature
    val atVal = spot.airTemp
    val atMin = profile.minAirTemp
    val atMax = profile.maxAirTemp
    val atOk = atVal in atMin..atMax
    list.add(
        ParameterStatus(
            name = "Air Temp",
            currentValue = atVal,
            minValue = atMin,
            maxValue = atMax,
            unit = "°C",
            isOk = atOk,
            isLow = atVal < atMin,
            isHigh = atVal > atMax,
            problemText = if (atVal < atMin) "Air is too cold (frost/freeze damage hazard)." else if (atVal > atMax) "Air heat stress detected (transpiration limit)." else "Favorable air climate.",
            recommendation = if (atVal < atMin) "Close field windbreaks or activate warm greenhouse heaters." else if (atVal > atMax) "Deploy shading sails, misting nozzles, or fans." else "Atmospheric temperature is optimal."
        )
    )

    // 9. Humidity
    val hVal = spot.humidity
    val hMin = profile.minHumidity
    val hMax = profile.maxHumidity
    val hOk = hVal in hMin..hMax
    list.add(
        ParameterStatus(
            name = "Humidity",
            currentValue = hVal,
            minValue = hMin,
            maxValue = hMax,
            unit = "%",
            isOk = hOk,
            isLow = hVal < hMin,
            isHigh = hVal > hMax,
            problemText = if (hVal < hMin) "Atmosphere is extremely dry (high moisture loss)." else if (hVal > hMax) "Humidity is very high (elevated fungal/mildew risk)." else "Favorable moisture-vapor levels.",
            recommendation = if (hVal < hMin) "Operate overhead fogging lines or mist emitters." else if (hVal > hMax) "Increase crop row spacing or run exhaust ventilators." else "Adequate relative humidity."
        )
    )

    // 10. Light Intensity
    val lVal = spot.light
    val lMin = profile.minLight
    val lMax = profile.maxLight
    val lOk = lVal in lMin..lMax
    list.add(
        ParameterStatus(
            name = "Light",
            currentValue = lVal,
            minValue = lMin,
            maxValue = lMax,
            unit = " Lux",
            isOk = lOk,
            isLow = lVal < lMin,
            isHigh = lVal > lMax,
            problemText = if (lVal < lMin) "Low light exposure (reduced photosynthesis rate)." else if (lVal > lMax) "Scorching light intensity (leads to leaf burn)." else "Satisfactory crop lighting.",
            recommendation = if (lVal < lMin) "Remove overhead shading, clear obstructions, or add supplemental light." else if (lVal > lMax) "Extend shading screens or diffuse incoming sunlight." else "No lighting adjustment needed."
        )
    )

    // 11. Atmospheric Pressure
    val prVal = spot.pressure
    val prMin = profile.minPressure
    val prMax = profile.maxPressure
    val prOk = prVal in prMin..prMax
    list.add(
        ParameterStatus(
            name = "Pressure",
            currentValue = prVal,
            minValue = prMin,
            maxValue = prMax,
            unit = " hPa",
            isOk = prOk,
            isLow = prVal < prMin,
            isHigh = prVal > prMax,
            problemText = if (prVal < prMin) "Low atmospheric pressure (unstable storm front incoming)." else if (prVal > prMax) "High atmospheric pressure (stable but dense air)." else "Standard barometric pressure.",
            recommendation = if (prVal < prMin) "Ensure protective row covers are secured against wind/hail." else if (prVal > prMax) "Clear normal field operations; standard growth cycle." else "Pressure levels are normal."
        )
    )

    return list
}
