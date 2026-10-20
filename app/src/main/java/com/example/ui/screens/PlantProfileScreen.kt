package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlantProfile
import com.example.ui.viewmodel.FarmViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantProfileScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.plantProfiles.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    // Form fields for new profile
    var profileName by remember { mutableStateOf("") }
    var minMoisture by remember { mutableStateOf("40") }
    var maxMoisture by remember { mutableStateOf("80") }
    var minSoilTemp by remember { mutableStateOf("15") }
    var maxSoilTemp by remember { mutableStateOf("30") }
    var minAirTemp by remember { mutableStateOf("15") }
    var maxAirTemp by remember { mutableStateOf("32") }
    var minHumidity by remember { mutableStateOf("45") }
    var maxHumidity by remember { mutableStateOf("75") }
    var minLight by remember { mutableStateOf("200") }
    var maxLight by remember { mutableStateOf("900") }
    var minPressure by remember { mutableStateOf("990") }
    var maxPressure by remember { mutableStateOf("1025") }
    var minNitrogen by remember { mutableStateOf("20") }
    var maxNitrogen by remember { mutableStateOf("60") }
    var minPhosphorus by remember { mutableStateOf("20") }
    var maxPhosphorus by remember { mutableStateOf("60") }
    var minPotassium by remember { mutableStateOf("20") }
    var maxPotassium by remember { mutableStateOf("60") }
    var minPh by remember { mutableStateOf("6.0") }
    var maxPh by remember { mutableStateOf("7.5") }
    var minEc by remember { mutableStateOf("1.0") }
    var maxEc by remember { mutableStateOf("2.5") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Plant Limit Profiles",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Text(
                        "Controls status & health computations",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Profile")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Profile")
                }
            }
        }

        // Active profile indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ACTIVE COMPILATION TARGET", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedProfile?.name ?: "No Profile Selected",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "The application currently evaluates real-time farm sensor thresholds using custom limits set in this profile.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Profiles list
        items(profiles) { profile ->
            val isSelected = selectedProfile?.name == profile.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectProfile(profile) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1A271F) else Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (isSelected) Color(0xFF4CAF50) else Color(0x11FFFFFF)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isSelected) Color(0xFF4CAF50) else Color.White
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Active", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show quick limits overview
                        Text(
                            text = "Moisture: ${profile.minSoilMoisture.toInt()}-${profile.maxSoilSoilMoisture.toInt()}%  |  Soil Temp: ${profile.minSoilTemp.toInt()}-${profile.maxSoilTemp.toInt()}°C",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        Text(
                            text = "N:${profile.minNitrogen.toInt()}-${profile.maxNitrogen.toInt()} P:${profile.minPhosphorus.toInt()}-${profile.maxPhosphorus.toInt()} K:${profile.minPotassium.toInt()}-${profile.maxPotassium.toInt()}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    // Delete button (Prevent deleting the only remaining profiles or pre-defined defaults if desired, but allow custom deletion)
                    IconButton(
                        onClick = { viewModel.deleteProfile(profile) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Profile",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // New Profile Dialog with scrolling limits list
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Plant Profile") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .background(Color.Transparent),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Plant Name
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Plant Name (e.g. Tomato)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            LimitRow("Soil Moisture (%)", minMoisture, { minMoisture = it }, maxMoisture, { maxMoisture = it })
                            LimitRow("Soil Temp (°C)", minSoilTemp, { minSoilTemp = it }, maxSoilTemp, { maxSoilTemp = it })
                            LimitRow("Air Temp (°C)", minAirTemp, { minAirTemp = it }, maxAirTemp, { maxAirTemp = it })
                            LimitRow("Humidity (%)", minHumidity, { minHumidity = it }, maxHumidity, { maxHumidity = it })
                            LimitRow("Light (Lux)", minLight, { minLight = it }, maxLight, { maxLight = it })
                            LimitRow("Pressure (hPa)", minPressure, { minPressure = it }, maxPressure, { maxPressure = it })
                            LimitRow("Nitrogen (N)", minNitrogen, { minNitrogen = it }, maxNitrogen, { maxNitrogen = it })
                            LimitRow("Phosphorus (P)", minPhosphorus, { minPhosphorus = it }, maxPhosphorus, { maxPhosphorus = it })
                            LimitRow("Potassium (K)", minPotassium, { minPotassium = it }, maxPotassium, { maxPotassium = it })
                            LimitRow("Soil pH Level", minPh, { minPh = it }, maxPh, { maxPh = it })
                            LimitRow("Electrical Cond (EC)", minEc, { minEc = it }, maxEc, { maxEc = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (profileName.isNotBlank()) {
                            val newProfile = PlantProfile(
                                name = profileName.trim(),
                                minSoilMoisture = minMoisture.toDoubleOrNull() ?: 40.0,
                                maxSoilSoilMoisture = maxMoisture.toDoubleOrNull() ?: 80.0,
                                minSoilTemp = minSoilTemp.toDoubleOrNull() ?: 15.0,
                                maxSoilTemp = maxSoilTemp.toDoubleOrNull() ?: 30.0,
                                minAirTemp = minAirTemp.toDoubleOrNull() ?: 15.0,
                                maxAirTemp = maxAirTemp.toDoubleOrNull() ?: 32.0,
                                minHumidity = minHumidity.toDoubleOrNull() ?: 45.0,
                                maxHumidity = maxHumidity.toDoubleOrNull() ?: 75.0,
                                minLight = minLight.toDoubleOrNull() ?: 200.0,
                                maxLight = maxLight.toDoubleOrNull() ?: 900.0,
                                minPressure = minPressure.toDoubleOrNull() ?: 990.0,
                                maxPressure = maxPressure.toDoubleOrNull() ?: 1025.0,
                                minNitrogen = minNitrogen.toDoubleOrNull() ?: 20.0,
                                maxNitrogen = maxNitrogen.toDoubleOrNull() ?: 60.0,
                                minPhosphorus = minPhosphorus.toDoubleOrNull() ?: 20.0,
                                maxPhosphorus = maxPhosphorus.toDoubleOrNull() ?: 60.0,
                                minPotassium = minPotassium.toDoubleOrNull() ?: 20.0,
                                maxPotassium = maxPotassium.toDoubleOrNull() ?: 60.0,
                                minPh = minPh.toDoubleOrNull() ?: 6.0,
                                maxPh = maxPh.toDoubleOrNull() ?: 7.5,
                                minEc = minEc.toDoubleOrNull() ?: 1.0,
                                maxEc = maxEc.toDoubleOrNull() ?: 2.5
                            )
                            viewModel.createProfile(newProfile)
                            showCreateDialog = false
                            profileName = ""
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LimitRow(
    label: String,
    minVal: String,
    onMinChange: (String) -> Unit,
    maxVal: String,
    onMaxChange: (String) -> Unit
) {
    Column {
        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minVal,
                onValueChange = onMinChange,
                label = { Text("Min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = maxVal,
                onValueChange = onMaxChange,
                label = { Text("Max") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
