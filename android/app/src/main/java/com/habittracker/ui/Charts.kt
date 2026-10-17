package com.habittracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = HabitPalette,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        if (values.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val barW = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val x = i * (barW + gap)
            drawRoundRect(
                color = track,
                topLeft = Offset(x, 0f),
                size = Size(barW, size.height),
                cornerRadius = CornerRadius(barW * 0.38f),
            )
            val h = (size.height * v.coerceIn(0f, 1f))
            if (h > 0f) {
                val base = colors[i % colors.size]
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(base.copy(alpha = 0.55f), base)),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW * 0.38f),
                )
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    stroke: Dp = 5.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val d = size.toPx() - s
            drawArc(track, 0f, 360f, false, topLeft = Offset(s / 2, s / 2), size = Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
            if (progress > 0f) {
                drawArc(
                    color, -90f, 360f * progress.coerceIn(0f, 1f), false,
                    topLeft = Offset(s / 2, s / 2), size = Size(d, d), style = Stroke(s, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
