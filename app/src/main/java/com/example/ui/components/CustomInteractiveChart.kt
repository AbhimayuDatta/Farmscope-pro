package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomInteractiveChart(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF4CAF50),
    gradientColors: List<Color> = listOf(Color(0x404CAF50), Color(0x004CAF50)),
    unit: String = ""
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No logs recorded for this parameter.",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        return
    }

    val maxVal = data.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val minVal = data.minOrNull()?.coerceAtMost(maxVal - 0.1) ?: 0.0
    val diff = (maxVal - minVal).coerceAtLeast(0.1)

    // Smooth entry animation
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        animationTriggered = true
    }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "chart_growth"
    )

    Column(
        modifier = modifier
            .background(Color(0xFF121212), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Min: ${String.format("%.1f", minVal)}$unit  |  Max: ${String.format("%.1f", maxVal)}$unit",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 40f
                val paddingRight = 20f
                val paddingTop = 20f
                val paddingBottom = 40f

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                // Draw background grid lines (horizontal)
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = paddingTop + (chartHeight / gridLines) * i
                    drawLine(
                        color = Color(0xFF2C2C2C),
                        start = Offset(paddingLeft, y),
                        end = Offset(width - paddingRight, y),
                        strokeWidth = 1f
                    )
                }

                // Points placement
                val points = mutableListOf<Offset>()
                val xSpacing = chartWidth / (data.size - 1).coerceAtLeast(1)

                for (i in data.indices) {
                    val x = paddingLeft + i * xSpacing
                    // Calculate Y based on value scaled between min and max
                    val normalizedY = (data[i] - minVal) / diff
                    val y = paddingTop + chartHeight - (normalizedY * chartHeight * animatedProgress).toFloat()
                    points.add(Offset(x, y))
                }

                // Draw gradient background under the path
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, paddingTop + chartHeight)
                        for (point in points) {
                            lineTo(point.x, point.y)
                        }
                        lineTo(points.last().x, paddingTop + chartHeight)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = gradientColors,
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )
                }

                // Draw line path with smooth curve (or straight lines for safety)
                if (points.size > 1) {
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            // Spline approximation curves
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cp1 = Offset(prev.x + (curr.x - prev.x) / 2f, prev.y)
                            val cp2 = Offset(prev.x + (curr.x - prev.x) / 2f, curr.y)
                            cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }

                // Draw dots at data points
                for (point in points) {
                    drawCircle(
                        color = lineColor,
                        radius = 6f,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = point
                    )
                }

                // Draw axes
                drawLine(
                    color = Color.Gray,
                    start = Offset(paddingLeft, paddingTop),
                    end = Offset(paddingLeft, paddingTop + chartHeight),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.Gray,
                    start = Offset(paddingLeft, paddingTop + chartHeight),
                    end = Offset(width - paddingRight, paddingTop + chartHeight),
                    strokeWidth = 2f
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // X Axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
