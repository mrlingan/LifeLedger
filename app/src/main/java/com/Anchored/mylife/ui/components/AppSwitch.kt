package com.Anchored.mylife.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 开关。替代 Material 的 Switch：没有描边、没有波纹光晕，
 * 只有一条轨道和一个会滑动的圆点。
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.surfaceSunken
            checked -> colors.accent
            else -> colors.border
        },
        animationSpec = tween(AppMotion.Base, easing = AppMotion.Standard),
        label = "switchTrack"
    )

    val knobOffset by animateDpAsState(
        targetValue = if (checked) {
            Sizes.switchWidth - Sizes.switchKnob - Spacing.xxs
        } else {
            Spacing.xxs
        },
        animationSpec = tween(AppMotion.Base, easing = AppMotion.Standard),
        label = "switchKnob"
    )

    Box(
        modifier = modifier
            .size(width = Sizes.switchWidth, height = Sizes.switchHeight)
            .clip(RoundedCornerShape(Radius.pill))
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(Sizes.switchKnob)
                .clip(CircleShape)
                .background(if (enabled) Color.White else colors.textTertiary)
        )
    }
}
