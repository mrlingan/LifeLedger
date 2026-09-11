package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 轻量分段控件，替代 Material 的 FilterChip / SegmentedButton。
 *
 * 视觉上只有一层浅色底 + 一个白色滑块，没有描边、没有阴影。
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.sm)

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceSunken)
            .padding(Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (selected) colors.surface else colors.surfaceSunken)
                    .clickable { onSelect(index) }
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = AppTheme.type.bodySmall,
                    color = if (selected) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}
