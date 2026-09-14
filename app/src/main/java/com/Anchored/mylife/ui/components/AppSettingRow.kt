package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 设置项行：标题 + 说明 + 右侧的补充文案或箭头。
 *
 * 不可用时整体降级为次要文字色，并且不响应点击。
 *
 * @param destructive 不可逆操作（移除密码这类）：标题用危险色，提醒这一项不是普通开关
 * @param leadingIcon 行首的图标（Material 图标）
 * @param leadingGlyph 行首的图标字形，和 [leadingIcon] 二选一。
 *   设置页用的是这一档：一小撮符号（`★ ✦ ✾ ◍ ♢` 这种）比线性图标更像"人生账本"的
 *   账目符号，也省掉了为每一个概念手画一条矢量路径。两者都会落在同一枚软色方块里，
 *   所以同一屏里混用也不会看起来是两套东西。
 */
@Composable
fun AppSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingGlyph: String? = null,
    trailingText: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    // 行首那枚方块的三档：普通行跟着强调色，危险行跟着危险色，不可用的行整块退成灰
    val markTint = when {
        !enabled -> colors.textTertiary
        destructive -> colors.error
        else -> colors.accent
    }
    val markBackground = when {
        !enabled -> colors.surfaceSunken
        destructive -> colors.error.copy(alpha = 0.12f)
        else -> colors.accentSoft
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null || leadingGlyph != null) {
            SettingMark(background = markBackground) {
                if (leadingGlyph != null) {
                    Text(
                        text = leadingGlyph,
                        style = AppTheme.type.glyph,
                        color = markTint
                    )
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = markTint,
                        modifier = Modifier.size(Sizes.iconLg)
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = when {
                    !enabled -> colors.textTertiary
                    destructive -> colors.error
                    else -> colors.textPrimary
                }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        if (trailingText != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = trailingText,
                // 右侧那列数值是"这一项现在是什么"，比正文轻一档但不该淡到看不见，
                // 所以用 bodySmall + 次要色（说明文字用的也是这一档）
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }

        if (showChevron) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/**
 * 设置项行首那枚图标方块：一层软色底 + 一个图标，42dp 见方、圆角 14dp。
 *
 * 底色比卡片浅、比页面深一点，所以它在卡片里是"浮起来的一枚牌子"，
 * 而不是又一块卡片。颜色由调用方给：普通行是强调色的浅底，危险行是危险色的浅底。
 *
 * 分区标题（[AppSectionHeader]）里的那枚也是同一个方块：同一张卡里，
 * "这一段叫什么"和"这一行是什么"该用同一种牌子，差一个尺寸反而像是两套东西。
 */
@Composable
internal fun SettingMark(
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(Sizes.settingMarkRadius)

    Box(
        modifier = modifier
            .size(Sizes.settingMark)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
