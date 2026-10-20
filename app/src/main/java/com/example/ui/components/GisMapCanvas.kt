package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import com.example.bluetooth.SensorData
import com.example.data.model.SensorLog
import java.util.Random

@Composable
fun GisMapCanvas(
    currentData: SensorData?,
    loggedPositions: List<SensorLog>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(100000.0) } // Coordinate magnification scale

    // Pulse animation for the current GPS beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Fallback base lat/lon if currentData is null
    val currentLat = currentData?.latitude ?: 28.613939
    val currentLon = currentData?.longitude ?: 77.209023

    Box(
        modifier = modifier
            .background(Color(0xFF0F1412), RoundedCornerShape(24.dp))
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // 1. Draw radar concentric grid lines
            val rings = 4
            val maxRingRadius = size.width.coerceAtMost(size.height) * 0.45f
            for (i in 1..rings) {
                val radius = (maxRingRadius / rings) * i
                drawCircle(
                    color = Color(0x334CAF50),
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                )
            }

            // Crosshairs
            drawLine(
                color = Color(0x224CAF50),
                start = Offset(cx - maxRingRadius, cy),
                end = Offset(cx + maxRingRadius, cy),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0x224CAF50),
                start = Offset(cx, cy - maxRingRadius),
                end = Offset(cx, cy + maxRingRadius),
                strokeWidth = 2f
            )

            // Convert Lat/Lon offsets to screen coordinates relative to center (Current GPS)
            fun gpsToScreen(lat: Double, lon: Double): Offset {
                // simple equirectangular projection centered on current GPS
                val dx = (lon - currentLon) * scale
                val dy = (currentLat - lat) * scale // Y goes down on screen
                return Offset(cx + dx.toFloat(), cy + dy.toFloat())
            }

            // 2. Draw Polyline paths connecting historical logging locations
            if (loggedPositions.size > 1) {
                val path = Path().apply {
                    val first = loggedPositions.first()
                    val p = gpsToScreen(first.latitude, first.longitude)
                    moveTo(p.x, p.y)
                    for (i in 1 until loggedPositions.size) {
                        val pos = loggedPositions[i]
                        val screenPos = gpsToScreen(pos.latitude, pos.longitude)
                        lineTo(screenPos.x, screenPos.y)
                    }
                }
                drawPath(
                    path = path,
                    color = Color(0x8800BCD4),
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // 3. Draw Historical log markers with distinct colors based on status/cell ID
            for (log in loggedPositions) {
                val screenPos = gpsToScreen(log.latitude, log.longitude)
                // Skip if off screen boundary
                if (screenPos.x < 0 || screenPos.x > size.width || screenPos.y < 0 || screenPos.y > size.height) {
                    continue
                }

                // Color based on status
                val markerColor = when {
                    log.status.contains("Excellent", ignoreCase = true) -> Color(0xFF4CAF50)
                    log.status.contains("Good", ignoreCase = true) -> Color(0xFF2196F3)
                    log.status.contains("Moderate", ignoreCase = true) -> Color(0xFFFFEB3B)
                    else -> Color(0xFFF44336)
                }

                drawCircle(
                    color = markerColor,
                    radius = 8f,
                    center = screenPos
                )
                drawCircle(
                    color = Color.Black,
                    radius = 3f,
                    center = screenPos
                )
            }

            // 4. Draw Current GPS pulsed crop tracker beacon at center
            val centerOffset = Offset(cx, cy)
            drawCircle(
                color = Color(0x662196F3),
                radius = pulseRadius,
                center = centerOffset,
                alpha = pulseAlpha
            )
            drawCircle(
                color = Color(0xFF00BCD4),
                radius = 12f,
                center = centerOffset
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = centerOffset
            )
        }

        // Tactical Overlay Controls (Zoom and telemetry info)
        Card(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE0121212)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CROP SURVEY GPS", color = Color.Gray, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Lat: ${String.format("%.6f", currentLat)}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Lon: ${String.format("%.6f", currentLon)}", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Sats: ${currentData?.satellites ?: 0} (3D Lock)", color = Color(0xFF4CAF50), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Zoom Controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { scale = (scale * 1.5).coerceAtMost(500000.0) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xCC2A2A2A))
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(
                onClick = { scale = (scale / 1.5).coerceAtLeast(10000.0) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xCC2A2A2A))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
            }
        }
    }
}
