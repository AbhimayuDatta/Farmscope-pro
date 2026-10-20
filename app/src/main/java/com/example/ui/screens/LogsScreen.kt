package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FarmViewModel
import com.example.util.LogFileHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(
    viewModel: FarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLogging by viewModel.isContinuousLogging.collectAsState()
    val recordsCount by viewModel.loggingRecordsCount.collectAsState()
    val elapsedTime by viewModel.elapsedTimeStr.collectAsState()
    val loggingFile by viewModel.loggingFile.collectAsState()
    val savedFiles by viewModel.savedLogsList.collectAsState()

    var showRenameDialog by remember { mutableStateOf<File?>(null) }
    var renameInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshLogsList()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        item {
            Column {
                Text(
                    "Sensor Data Logging",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    "CSV data management & export controls",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // 1. Continuous Logging Control Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLogging) Color(0x33F44336) else Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                tint = if (isLogging) Color.Red else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isLogging) "Continuous Logging Running" else "Continuous Logging Stopped",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                        }

                        Button(
                            onClick = {
                                if (isLogging) {
                                    viewModel.stopContinuousLogging()
                                } else {
                                    viewModel.startContinuousLogging()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLogging) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (isLogging) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isLogging) "Stop" else "Start")
                        }
                    }

                    if (isLogging) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Records Logged", fontSize = 11.sp, color = Color.Gray)
                                Text("$recordsCount pkts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Elapsed Duration", fontSize = 11.sp, color = Color.Gray)
                                Text(elapsedTime, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Target file: ${loggingFile?.name ?: "initializing..."}",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 2. Saved Logs List Title
        item {
            Text(
                "Exportable CSV Records Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.LightGray
            )
        }

        if (savedFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No saved CSV log files available yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(savedFiles) { file ->
                val sizeKb = String.format("%.2f", file.length() / 1024f)
                val lastModifiedStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF009688))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = file.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.widthIn(max = 200.dp)
                                )
                            }
                            Text(
                                text = "$sizeKb KB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Modified: $lastModifiedStr", fontSize = 11.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0x11FFFFFF))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions row: Share, Open, Rename, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { context.startActivity(LogFileHelper.getShareIntent(context, file)) }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(LogFileHelper.getOpenIntent(context, file))
                                    } catch (e: Exception) {
                                        // Fail gracefully if no Excel reader app is found
                                    }
                                }
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = {
                                    renameInput = file.nameWithoutExtension
                                    showRenameDialog = file
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rename", fontSize = 12.sp)
                            }

                            TextButton(
                                onClick = { viewModel.deleteLogFile(file) }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = Color.Red.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog != null) {
        val file = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename CSV Log") },
            text = {
                Column {
                    Text("Enter new file name:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameLogFile(file, renameInput.trim())
                            showRenameDialog = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
