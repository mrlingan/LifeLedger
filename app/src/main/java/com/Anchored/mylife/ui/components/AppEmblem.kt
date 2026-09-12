package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes

/**
 * 成就徽记：一个圆角方块 + 标题首字。
 *
 * 为什么不用图标字体也不用 emoji：徽记要跟着人用很多年，
 * 首字在任何字体、任何语言下都稳定；稀有度只通过描边和字色各露一点，
 * 不做发光、不做金属质感堆叠。
 *
 * - 已解锁：描边和字色取稀有度色（低饱和），一眼能看出"这是我的收藏"
 * - 未解锁：中性描边 + 次要字色，保留轮廓但不抢注意力
 */
@Composable
fun AppEmblem(
    text: String,
    modifier: Modifier = Modifier,
    tier: RarityTier? = null,
    unlocked: Boolean = false,
    size: Dp = Sizes.avatarLg
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.lg)
    val rarityColor = tier?.let { colors.rarityColor(it) }

    val borderColor = when {
        unlocked && rarityColor != null -> rarityColor.copy(alpha = 0.40f)
        else -> colors.border
    }
    val glyphColor = when {
        unlocked && rarityColor != null -> rarityColor
        unlocked -> colors.textSecondary
        else -> colors.textTertiary
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.surfaceSunken)
            .border(Sizes.hairline, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.firstGlyph(),
            style = if (size >= Sizes.avatarLg) AppTheme.type.h2 else AppTheme.type.h3,
            color = glyphColor
        )
    }
}
