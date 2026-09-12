package com.Anchored.mylife.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.data.settings.MotionChoice

private const val STAGGER_STEPS = 8
private const val STAGGER_STEP_MILLIS = 40

/**
 * 列表项入场：淡入 + 轻微上移。
 *
 * 只做 8dp 的位移、300ms 内完成，同屏条目按 40ms 错峰，
 * 目的是让列表"落下来"而不是"弹出来"。
 *
 * ```
 * itemsIndexed(items) { index, item ->
 *     Row(modifier = Modifier.appearAnimation(index)) { ... }
 * }
 * ```
 */
@Composable
fun Modifier.appearAnimation(index: Int = 0): Modifier {
    // 动效关掉时连淡入都不做，直接是"就在那里"
    val motion = AppTheme.display.motion
    if (motion == MotionChoice.OFF) return this

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val progress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = AppMotion.duration(AppMotion.Slow),
            // 精简模式下不再错峰，列表是一起落下来
            delayMillis = if (motion == MotionChoice.FULL) {
                (index % STAGGER_STEPS) * STAGGER_STEP_MILLIS
            } else {
                0
            },
            easing = AppMotion.Decelerate
        ),
        label = "appearAnimation"
    )

    return this
        .alpha(progress)
        .offset(y = Spacing.sm * (1f - progress))
}
