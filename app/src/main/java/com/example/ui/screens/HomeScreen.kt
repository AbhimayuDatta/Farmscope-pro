package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import java.io.File
import com.example.bluetooth.ConnectionState
import com.example.bluetooth.ParameterStatus
import com.example.bluetooth.SensorData
import com.example.bluetooth.StatusCalculator
import com.example.bluetooth.StatusLevel
import com.example.ui.viewmodel.FarmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionState by viewModel.bluetoothManager.connectionState.collectAsState()
    val isSimulator by viewModel.bluetoothManager.isSimulatorMode.collectAsState()
    val autoReconnect by viewModel.bluetoothManager.autoReconnect.collectAsState()
    val liveData by viewModel.currentSensorData.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. App Branding Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101010)),
                border = BorderStroke(1.dp, Color(0x1F4CAF50)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Display custom app logo statically from drawable
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0x114CAF50), CircleShape)
                                .border(1.5.dp, Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop

                            )
                        }

                        Column {
                            Text(
                                text = "farmSense Pro",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Agronomic Companion",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 1. Connection Manager Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val connIcon = when (connectionState) {
                                ConnectionState.CONNECTED -> Icons.Outlined.BluetoothConnected
                                ConnectionState.CONNECTING -> Icons.Filled.BluetoothSearching
                                else -> Icons.Outlined.Bluetooth
                            }
                            val iconColor = when (connectionState) {
                                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                ConnectionState.CONNECTING -> Color(0xFFFFEB3B)
                                else -> Color.Gray
                            }
                            Icon(connIcon, contentDescription = "BT Status", tint = iconColor, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isSimulator) "BT Simulator Mode" else "Device Connection",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = when (connectionState) {
                                        ConnectionState.CONNECTED -> "Connected"
                                        ConnectionState.CONNECTING -> "Connecting..."
                                        else -> "Disconnected"
                                    },
                                    color = iconColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (connectionState == ConnectionState.CONNECTED) {
                                    viewModel.bluetoothManager.disconnect()
                                } else {
                                    showConnectionDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (connectionState == ConnectionState.CONNECTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (connectionState == ConnectionState.CONNECTED) "Disconnect" else "Connect")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggles & Actions
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Simulator Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isSimulator,
                                onCheckedChange = { viewModel.bluetoothManager.toggleSimulator(it) }
                            )
                            Text("Simulation", fontSize = 14.sp)
                        }

                        // Auto Reconnect Toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoReconnect,
                                onCheckedChange = { viewModel.bluetoothManager.toggleAutoReconnect(it) }
                            )
                            Text("Auto Reconnect", fontSize = 14.sp)
                        }

                        // Quick Log Reading Button
                        Button(
                            onClick = { viewModel.saveCurrentReadingManual() },
                            enabled = liveData != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Reading", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Active profile summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16251C)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Eco, contentDescription = "Profile", tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Active Plant Limit Profile: ",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = selectedProfile?.name ?: "Tomato (Default)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        // Live Telemetry Cards
        if (liveData != null) {
            val data = liveData!!
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))

            // 1. GPS Card
            item {
                TelemetryCategoryCard(
                    title = "GPS DATA",
                    icon = Icons.Default.GpsFixed,
                    timestamp = timeString
                ) {
                    TelemetryItem(label = "Latitude", value = String.format("%.6f", data.latitude), unit = "°")
                    TelemetryItem(label = "Longitude", value = String.format("%.6f", data.longitude), unit = "°")
                    TelemetryItem(label = "Altitude", value = String.format("%.1f", data.altitude), unit = "m")
                    TelemetryItem(label = "Satellites", value = "${data.satellites}", unit = " fixed")
                }
            }

            // 2. Environment Card
            item {
                val airTempStatus = StatusCalculator.getTemperatureStatus(data.airTemp, selectedProfile, isSoil = false)
                val humStatus = StatusCalculator.getHumidityStatus(data.humidity, selectedProfile)
                val pressStatus = StatusCalculator.getPressureStatus(data.pressure, selectedProfile)
                val aqStatus = StatusCalculator.getAirQualityStatus(data.airQuality)
                val lightStatus = StatusCalculator.getLightStatus(data.light, selectedProfile)

                TelemetryCategoryCard(
                    title = "ATMOSPHERE & LIGHT",
                    icon = Icons.Default.Cloud,
                    timestamp = timeString
                ) {
                    TelemetryItem(label = "Air Temperature", value = String.format("%.1f", data.airTemp), unit = "°C", status = airTempStatus)
                    TelemetryItem(label = "Humidity", value = String.format("%.1f", data.humidity), unit = "%", status = humStatus)
                    TelemetryItem(label = "Atmospheric Pressure", value = String.format("%.1f", data.pressure), unit = " hPa", status = pressStatus)
                    TelemetryItem(label = "Air Quality (VOC)", value = String.format("%.1f", data.airQuality), unit = "%", status = aqStatus)
                    TelemetryItem(label = "Light Intensity", value = String.format("%.1f", data.light), unit = " Lux", status = lightStatus)
                }
            }

            // 3. Soil Metrics Card
            item {
                val moistStatus = StatusCalculator.getSoilMoistureStatus(data.soilMoisture, selectedProfile)
                val soilTempStatus = StatusCalculator.getTemperatureStatus(data.soilTemp, selectedProfile, isSoil = true)
                val phStatus = StatusCalculator.getPhStatus(data.pH, selectedProfile)
                val ecStatus = StatusCalculator.getEcStatus(data.ec, selectedProfile)

                TelemetryCategoryCard(
                    title = "SOIL METRICS",
                    icon = Icons.Default.Grass,
                    timestamp = timeString
                ) {
                    TelemetryItem(label = "Soil Moisture", value = String.format("%.1f", data.soilMoisture), unit = "%", status = moistStatus)
                    TelemetryItem(label = "Soil Temperature", value = String.format("%.1f", data.soilTemp), unit = "°C", status = soilTempStatus)
                    TelemetryItem(label = "Soil pH Level", value = String.format("%.2f", data.pH), unit = "", status = phStatus)
                    TelemetryItem(label = "Electrical Conductivity", value = String.format("%.2f", data.ec), unit = " mS/cm", status = ecStatus)
                }
            }

            // 4. Soil NPK Card
            item {
                val nStatus = StatusCalculator.getNpkStatus(data.nitrogen, selectedProfile, "N")
                val pStatus = StatusCalculator.getNpkStatus(data.phosphorus, selectedProfile, "P")
                val kStatus = StatusCalculator.getNpkStatus(data.potassium, selectedProfile, "K")

                TelemetryCategoryCard(
                    title = "SOIL NPK NUTRIENTS",
                    icon = Icons.Default.Science,
                    timestamp = timeString
                ) {
                    TelemetryItem(label = "Nitrogen (N)", value = String.format("%.1f", data.nitrogen), unit = " mg/kg", status = nStatus)
                    TelemetryItem(label = "Phosphorus (P)", value = String.format("%.1f", data.phosphorus), unit = " mg/kg", status = pStatus)
                    TelemetryItem(label = "Potassium (K)", value = String.format("%.1f", data.potassium), unit = " mg/kg", status = kStatus)
                }
            }

            // 5. Crop Security Card
            item {
                val humanDetected = data.humanDetected == 1
                val secColor = if (humanDetected) Color(0xFFF44336) else Color(0xFF4CAF50)
                val secStatus = ParameterStatus(
                    text = if (humanDetected) "INTRUDER DETECTED" else "Secure",
                    level = if (humanDetected) StatusLevel.CRITICAL else StatusLevel.GOOD,
                    color = secColor
                )

                // Flash animation for Security card if human detected
                val secInfinite = rememberInfiniteTransition(label = "sec_flash")
                val secBgColor by secInfinite.animateColor(
                    initialValue = Color(0xFF1E1E1E),
                    targetValue = if (humanDetected) Color(0x44F44336) else Color(0xFF1E1E1E),
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "sec_color"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = secBgColor),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (humanDetected) Color.Red else Color(0x33FFFFFF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Security",
                                    tint = secColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "CROP SECURITY ZONE",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                            Text(
                                text = "Live  |  $timeString",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Motion Sensors", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(secColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = secStatus.text,
                                    color = secStatus.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // Empty State while waiting
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Waiting for live Bluetooth packets...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Bluetooth Connection Dialog
    if (showConnectionDialog) {
        val pairedDevices = viewModel.bluetoothManager.getBondedDevices()

        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("Select AgroTech Bluetooth Unit") },
            text = {
                Column {
                    if (pairedDevices.isEmpty()) {
                        Text("No paired Classic Bluetooth devices found on your system. Please pair your ESP32 in System Settings first, or use Simulator Mode.")
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(pairedDevices.size) { index ->
                                val device = pairedDevices[index]
                                val name = try { device.name ?: "Unknown ESP32" } catch (e: SecurityException) { "Permission Error" }
                                val address = device.address
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                                    onClick = {
                                        viewModel.bluetoothManager.connectDevice(address)
                                        showConnectionDialog = false
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(name, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(address, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun TelemetryCategoryCard(
    title: String,
    icon: ImageVector,
    timestamp: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0x1AFFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
                Text(
                    text = "Live  |  $timestamp",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun TelemetryItem(
    label: String,
    value: String,
    unit: String,
    status: ParameterStatus? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.LightGray, fontSize = 14.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "$value$unit",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            if (status != null) {
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(status.color.copy(alpha = 0.15f))
                        .border(1.dp, status.color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        status.text,
                        color = status.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
