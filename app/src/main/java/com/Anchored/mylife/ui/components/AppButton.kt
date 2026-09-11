package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
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

    val background = when (variant) {
        AppButtonVariant.Primary -> if (enabled) colors.accent else colors.accent.copy(alpha = 0.35f)
        else -> Color.Transparent
    }
    val contentColor = when (variant) {
        AppButtonVariant.Primary -> colors.onAccent
        AppButtonVariant.Secondary -> if (enabled) colors.textPrimary else colors.textTertiary
        AppButtonVariant.Text -> if (enabled) colors.accent else colors.textTertiary
        AppButtonVariant.Destructive -> if (enabled) colors.error else colors.textTertiary
    }
    val showBorder = variant == AppButtonVariant.Secondary

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (showBorder) Modifier.border(Sizes.hairline, colors.border, shape) else Modifier
            )
            .clickable(enabled = enabled, onClick = onClick)
            .then(
                if (variant == AppButtonVariant.Text) Modifier
                else Modifier.heightIn(min = Sizes.touchTarget)
            )
            .padding(
                horizontal = if (variant == AppButtonVariant.Text) Spacing.sm else Spacing.lg,
                vertical = if (variant == AppButtonVariant.Text) Spacing.sm else Spacing.md
            ),
        contentAlignment = Alignment.Center
    ) {
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
                style = AppTheme.type.body,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
