package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassStyle
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 卡片色调。
 *
 * iOS 的分组列表靠背景层级而不是硬描边区分内容；卡片默认没有轮廓线。
 */
enum class AppCardTone {
    /** 默认：白底 + 描边，用在背景稍灰的页面上 */
    Surface,

    /** 比 Surface 再抬一层，用在已经是 Surface 的容器内部 */
    Elevated,

    /** 内嵌区域：输入框、缩略图底、进度轨道 */
    Soft,

    /** 强调区域：使用强调色的浅色调，克制使用 */
    Accent,

    /**
     * 液态玻璃：和底部导航同一条折射管线，折射的是背景图与遮罩那一层。
     *
     * 有背景图时边缘能看见画面被掰弯、带一点色散；没有背景图时折射看不出来，
     * 卡片仍是一块带高光与描边的奶玻璃。
     *
     * 关掉液态玻璃（设置里的开关）或者跑在布局预览里时退回普通卡片表面，
     * 也就是这个功能出现之前的样子。
     */
    Glass
}

/**
 * 通用容器。
 *
 * ```
 * AppCard(onClick = { ... }) {
 *     Text("...", style = AppTheme.type.h3)
 * }
 * ```
 *
 * 要一块液态玻璃卡片就传 `tone = AppCardTone.Glass`：玻璃的圆角由 SDF 算，
 * 所以自定义 [shape] 时记得把 [cornerRadius] 一起改掉，否则折射带会画错位置。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    tone: AppCardTone = AppCardTone.Surface,
    shape: Shape = RoundedCornerShape(Radius.lg),
    cornerRadius: Dp = Radius.lg,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = AppTheme.colors
    val backdrop = LocalLiquidGlassBackdrop.current
    val glassEnabled = LocalLiquidGlassEnabled.current

    if (tone == AppCardTone.Glass && glassEnabled) {
        GlassCard(
            backdrop = backdrop,
            modifier = modifier,
            shape = shape,
            cornerRadius = cornerRadius,
            contentPadding = contentPadding,
            onClick = onClick,
            content = content
        )
        return
    }

    val background = when (tone) {
        AppCardTone.Surface -> colors.surface
        AppCardTone.Elevated -> colors.surfaceElevated
        AppCardTone.Soft -> colors.surfaceSunken
        AppCardTone.Accent -> colors.accentSoft
        // 玻璃卡片由上面的分支处理；走到这里说明没有采样源，退回普通卡片表面
        AppCardTone.Glass -> colors.surfaceElevated
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
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

/**
 * 玻璃卡片的实现：外壳交给 [GlassSurface]（折射 + 高光 + 描边），
 * 内容照常画在玻璃之上，保持清晰。
 *
 * 内容必须先撑出尺寸：玻璃本体是 `matchParentSize` 铺的，卡片自己的大小
 * 由 [content] 决定。
 *
 * [backdrop] 为 null 表示采不到背后的画面（对话框、预览）：折射那一步退化成
 * [LiquidGlassStyle.frosted] 的磨砂底，高光与描边照旧——仍然是玻璃，只是薄一点。
 */
@Composable
private fun GlassCard(
    backdrop: LiquidGlassBackdrop?,
    modifier: Modifier,
    shape: Shape,
    cornerRadius: Dp,
    contentPadding: PaddingValues,
    onClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable BoxScope.() -> Unit = {
        Column(
            // clip 是为了把按下时的水波纹收在圆角里，玻璃本体的裁剪不影响内容
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                )
                .padding(contentPadding),
            content = content
        )
    }

    GlassSurface(
        backdrop = backdrop,
        style = rememberCardGlassStyle(),
        shape = shape,
        cornerRadius = cornerRadius,
        modifier = modifier,
        content = body
    )
}

/**
 * 卡片玻璃参数。
 *
 * 比底栏薄一档：折射带更窄、位移更小。卡片面积比底栏大得多，
 * 位移给大了边缘那一圈会把背后的画面扯得不像玻璃。
 */
@Composable
private fun rememberCardGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surfaceElevated) {
        LiquidGlassStyle(
            refractionHeight = 16.dp,
            refractionAmount = -18.dp,
            dispersion = 0.28f,
            depth = 0.35f,
            tint = glass.tint,
            sheen = glass.sheen,
            rimTop = glass.rimTop,
            rimBottom = glass.rimBottom,
            // 没开玻璃 / 设备不支持采样时的兜底填充：普通卡片表面，
            // 也就是和 [AppCardTone.Glass] 取不到采样源时同一个观感
            frosted = colors.surfaceElevated
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
