package com.Anchored.mylife.ui.components.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.theme.AppTheme

/**
 * 「被选中」的那块玻璃：一层薄薄的任务色玻璃。
 *
 * 给可选项（语言列表里选中的一行、选中的分类标签）用。选中的东西要一眼看得出来，
 * 所以它的底色是任务色而不是中性色；`frosted` 是同样观感的不透明等效色，
 * 采不到画面时（对话框里）颜色也不会跟着变。
 *
 * 放在这里而不是各组件里各写一份：这几处的厚度、色散得是同一个值，
 * 否则同一个界面里的"选中"会长得不一样。
 */
@Composable
fun rememberSelectionGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.accent, colors.accentSoft) {
        LiquidGlassStyle(
            refractionHeight = 9.dp,
            refractionAmount = -9.dp,
            dispersion = 0.4f,
            depth = 0.45f,
            tint = colors.accent.copy(alpha = 0.22f),
            sheen = glass.dropletSheen,
            rimTop = glass.dropletRimTop,
            rimBottom = glass.dropletRimBottom,
            frosted = lerp(colors.accentSoft, colors.accent, 0.12f)
        )
    }
}
