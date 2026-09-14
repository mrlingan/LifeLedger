package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassStyle
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 按钮只有三档 + 一个危险态。
 *
 * - Primary：强调色实底，一屏最多一个
 * - Secondary：描边
 * - Text：无底色
 * - Destructive：用于删除这类不可逆操作
 */
enum class AppButtonVariant {
    Primary,
    Secondary,
    Text,
    Destructive
}

/**
 * 按钮。
 *
 * 开着液态玻璃的时候，除了 [AppButtonVariant.Text]（它没有面，就是一段可点的文字），
 * 其余三档都是一小块玻璃：底色换成对应语义色的玻璃 / 半透明玻璃，再叠上高光与描边。
 * 圆角与点击区域不变，所以换过来不会动到任何版面。
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val glassEnabled = LocalLiquidGlassEnabled.current

    // 文本按钮没有"面"，铺一层玻璃它就变成按钮了；禁用态也走普通路径，
    // 免得和"变淡"的语义打架
    val useGlass = glassEnabled && enabled && variant != AppButtonVariant.Text

    val background = when (variant) {
        AppButtonVariant.Primary -> if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val contentColor = when (variant) {
        // 玻璃版的强调按钮不是实底蓝：底色只有一层淡蓝，标签用深一档的强调色才压得住
        AppButtonVariant.Primary -> if (useGlass) colors.accentStrong else colors.onAccent
        AppButtonVariant.Secondary -> if (enabled) colors.textPrimary else colors.textTertiary
        AppButtonVariant.Text -> if (enabled) colors.accent else colors.textTertiary
        AppButtonVariant.Destructive -> if (enabled) colors.error else colors.textTertiary
    }
    val showBorder = variant == AppButtonVariant.Secondary

    val contentPadding = PaddingValues(
        horizontal = if (variant == AppButtonVariant.Text) Spacing.sm else Spacing.lg,
        vertical = if (variant == AppButtonVariant.Text) Spacing.sm else Spacing.md
    )

    val label: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(Sizes.iconMd)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
            }
            Text(
                text = text,
                style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    // 文字画在玻璃之上，保持清晰；玻璃只负责它下面那一层
    val body: @Composable BoxScope.() -> Unit = {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            label()
        }
    }

    if (useGlass) {
        GlassSurface(
            backdrop = LocalLiquidGlassBackdrop.current,
            style = rememberButtonGlassStyle(variant),
            shape = shape,
            cornerRadius = Radius.md,
            modifier = modifier
                .heightIn(min = Sizes.touchTarget)
                .clickable(onClick = onClick),
            content = body
        )
        return
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (showBorder) Modifier.border(Sizes.hairline, colors.border, shape) else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            // 每一档都撑到最小点击高度，文字在框里居中：Text 档原来比别的档矮一截，
            // 和描边按钮并排时（「全部显示 / 恢复默认」这种）文字会贴着行的上沿，
            // 看起来像没对齐
            .heightIn(min = Sizes.touchTarget)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        label()
    }
}

/**
 * 按钮的玻璃参数。
 *
 * 三档共用一个"厚度"（比卡片薄、比底栏薄，按钮小，掰狠了就糊），
 * 区别只在底色：Primary 是强调色的厚玻璃，Secondary 是中性的奶玻璃，
 * Destructive 是危险色的淡玻璃——都保留原来的语义色，只把材质换成玻璃。
 *
 * `frosted` 是采不到画面时的兜底（对话框里、API 31 以下）：给一个**不透明**的
 * 等效色，让"有没有折射"不影响按钮本身的颜色。
 */
@Composable
private fun rememberButtonGlassStyle(variant: AppButtonVariant): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(variant, glass, colors.surface, colors.surfaceElevated, colors.accent, colors.error) {
        val tint = when (variant) {
            // 强调色也走玻璃：实底蓝太"重"，和别处的玻璃材质也不搭。
            // 一层淡任务色 + 强调色标签，既看得出这是主操作，又确实是玻璃。
            AppButtonVariant.Primary -> colors.accent.copy(alpha = 0.24f)
            AppButtonVariant.Destructive -> colors.error.copy(alpha = 0.20f)
            else -> glass.droplet
        }
        val frosted = when (variant) {
            // 采不到画面时的等效底色：和 tint 叠在表面色上是一样的观感
            AppButtonVariant.Primary -> lerp(colors.surfaceElevated, colors.accent, 0.24f)
            AppButtonVariant.Destructive -> lerp(colors.surfaceElevated, colors.error, 0.20f)
            else -> colors.surfaceElevated
        }
        LiquidGlassStyle(
            refractionHeight = 10.dp,
            refractionAmount = -10.dp,
            dispersion = 0.36f,
            depth = 0.4f,
            tint = tint,
            sheen = glass.dropletSheen,
            rimTop = glass.dropletRimTop,
            rimBottom = glass.dropletRimBottom,
            frosted = frosted
        )
    }
}

/** 无底色的图标按钮，统一 48dp 点击区域 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.textPrimary,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .size(Sizes.touchTarget)
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else AppTheme.colors.textTertiary,
            modifier = Modifier.size(Sizes.iconLg)
        )
    }
}
