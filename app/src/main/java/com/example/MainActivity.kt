package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.narrator.NarrationText
import com.example.narrator.NarratorScreen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FarmViewModel

enum class AppScreen(val label: String, val icon: ImageVector) {
    HOME("Telemetry", Icons.Default.Dashboard),
    MAPPING("Matrix", Icons.Default.GridOn),
    LAND_GIS("Field Map", Icons.Default.Landscape),
    PROFILES("Profiles", Icons.Default.Eco),
    RECOMMENDATIONS("Analysis", Icons.Default.Lightbulb),
    LOGS("CSV Logs", Icons.Default.FolderOpen)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request version-appropriate permissions dynamically
        val permissionsToRequest = mutableListOf<String>()
        permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungrantedPermissions = permissionsToRequest.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            requestPermissions(ungrantedPermissions.toTypedArray(), 101)
        }

        setContent {
            MyApplicationTheme(darkTheme = true) { // Explicit dark mode as requested
                val viewModel: FarmViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                var showSplash by remember { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Main App Content
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar(
                                containerColor = Color(0xFF0F0F0F),
                                modifier = Modifier.navigationBarsPadding(),
                                tonalElevation = 8.dp
                            ) {
                                AppScreen.values().forEach { screen ->
                                    val isSelected = currentScreen == screen
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentScreen = screen },
                                        icon = {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                tint = if (isSelected) Color.Black else Color.Gray
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = screen.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else Color.Gray
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF090A0A))
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    bottom = innerPadding.calculateBottomPadding()
                                )
                        ) {
                            // key() gives each tab its own narrator session: switching tabs stops speech,
                            // clears the old screen's elements, and lets auto-narrate run again for the new tab.
                            key(currentScreen) {
                                NarratorScreen(screenTitle = NarrationText.Literal(currentScreen.label)) {
                                    when (currentScreen) {
                                        AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                                        AppScreen.MAPPING -> FarmMappingScreen(viewModel = viewModel)
                                        AppScreen.LAND_GIS -> GisFarmingLandScreen(viewModel = viewModel)
                                        AppScreen.PROFILES -> PlantProfileScreen(viewModel = viewModel)
                                        AppScreen.RECOMMENDATIONS -> LiveRecommendationScreen(viewModel = viewModel)
                                        AppScreen.LOGS -> LogsScreen(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }

                    // Splash Overlay
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF090A0A)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subdued green ambient radial gradient background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0x1F4CAF50), Color.Transparent),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.7f
                                    ),
                                    radius = size.width * 0.7f,
                                    center = Offset(size.width / 2f, size.height / 2f)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Custom App Logo from drawable resource
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                        contentDescription = "App Logo",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFF4CAF50), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "farmSense Pro",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = "Precision GIS & Agronomic Intelligence",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(48.dp))

                                // Professional attributions card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "POWERED BY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50),
                                                letterSpacing = 1.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "robogarage Labs",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .height(1.dp)
                                                .fillMaxWidth(0.5f)
                                                .background(Color(0x11FFFFFF))
                                        )

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "MADE BY",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50),
                                                letterSpacing = 1.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Team Robotics Sir",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(48.dp))

                                // Custom Material progress spinner
                                CircularProgressIndicator(
                                    color = Color(0xFF4CAF50),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(28.dp)
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = { showSplash = false },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4CAF50),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Launch Application",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Auto transition delay of 3 seconds
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3500)
                            showSplash = false
                        }
                    }
                }
            }
        }
    }
}
