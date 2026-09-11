package com.Anchored.mylife.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes

/**
 * 条形进度。没有发光、没有渐变，只有一条干净的轨道和一条强调色。
 */
@Composable
fun AppProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceSunken,
    height: Dp = Sizes.progressBar
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.value(),
        label = "appProgressBar"
    )
    val shape = RoundedCornerShape(Radius.pill)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(shape)
                .background(color)
        )
    }
}

/**
 * 环形进度。中间可以放任何内容（数字、图标）。
 *
 * ```
 * AppProgressRing(progress = 0.67f) {
 *     AnimatedMetricNumber(value = 67)
 * }
 * ```
 */
@Composable
fun AppProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = Sizes.progressRingSize,
    strokeWidth: Dp = Sizes.progressRing,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceSunken,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = AppMotion.value(),
        label = "appProgressRing"
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(
                width = this.size.width - stroke,
                height = this.size.height - stroke
            )

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        content()
    }
}

/**
 * 不确定进度条：用于"正在处理"这类没有明确进度的场景。
 *
 * 用一小段强调色在轨道上匀速滑过，没有 Material 的旋转菊花。
 */
@Composable
fun AppIndeterminateBar(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.accent,
    trackColor: Color = AppTheme.colors.surfaceSunken,
    height: Dp = Sizes.progressBar
) {
    val transition = rememberInfiniteTransition(label = "indeterminate")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing)
        ),
        label = "indeterminateProgress"
    )

    val shape = RoundedCornerShape(Radius.pill)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(trackColor)
    ) {
        // 一小段强调色从左滑到右；按轨道实际宽度计算，适配任何屏宽
        val segmentWidth = maxWidth * 0.35f
        val travel = maxWidth + segmentWidth

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(segmentWidth)
                .offset(x = travel * progress - segmentWidth)
                .clip(shape)
                .background(color)
        )
    }
}
