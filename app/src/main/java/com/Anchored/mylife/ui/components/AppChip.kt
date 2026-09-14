package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.components.liquidglass.rememberSelectionGlassStyle
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 轻量标签，替代 Material 的 FilterChip。
 *
 * 只有一层底色，没有描边、没有选中对勾、没有抬升。
 *
 * 开着液态玻璃时，选中那一个是一小块任务色玻璃（没选中的保持一层平底色）：
 * 一屏里选中的通常只有一个，玻璃只用在这一处，选中态才突出。
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    val glassEnabled = LocalLiquidGlassEnabled.current

    if (glassEnabled && selected) {
        GlassSurface(
            backdrop = LocalLiquidGlassBackdrop.current,
            style = rememberSelectionGlassStyle(),
            shape = shape,
            cornerRadius = Radius.sm,
            modifier = modifier.clickable(onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(
                    horizontal = Spacing.md,
                    vertical = Spacing.sm
                ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTheme.type.bodySmall,
                    color = colors.accentStrong
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.surfaceSunken)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            text = label,
            style = AppTheme.type.bodySmall,
            color = if (selected) colors.accentStrong else colors.textSecondary
        )
    }
}
