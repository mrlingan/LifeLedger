package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 卡片色调。
 *
 * 默认只靠「表面色差 + 1dp 描边」建立层级，**不使用阴影**。
 */
enum class AppCardTone {
    /** 默认：白底 + 描边，用在背景稍灰的页面上 */
    Surface,

    /** 比 Surface 再抬一层，用在已经是 Surface 的容器内部 */
    Elevated,

    /** 内嵌区域：输入框、缩略图底、进度轨道 */
    Soft,

    /** 强调区域：使用强调色的浅色调，克制使用 */
    Accent
}

/**
 * 通用容器。
 *
 * ```
 * AppCard(onClick = { ... }) {
 *     Text("...", style = AppTheme.type.h3)
 * }
 * ```
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: AppCardTone = AppCardTone.Surface,
    shape: Shape = RoundedCornerShape(Radius.lg),
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors

    val background = when (tone) {
        AppCardTone.Surface -> colors.surface
        AppCardTone.Elevated -> colors.surfaceElevated
        AppCardTone.Soft -> colors.surfaceSunken
        AppCardTone.Accent -> colors.accentSoft
    }
    val borderColor = when (tone) {
        AppCardTone.Surface -> colors.border
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(Sizes.hairline, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/** 横向分隔线 */
@Composable
fun AppDivider(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.divider,
    thickness: Dp = Sizes.hairline
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

/** 纵向分隔线，用在并排的数据块之间 */
@Composable
fun AppVerticalDivider(
    height: Dp,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.divider,
    thickness: Dp = Sizes.hairline
) {
    Box(
        modifier = modifier
            .width(thickness)
            .height(height)
            .background(color)
    )
}
