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
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 轻量标签，替代 Material 的 FilterChip。
 *
 * 只有一层底色，没有描边、没有选中对勾、没有抬升。
 */
@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
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
