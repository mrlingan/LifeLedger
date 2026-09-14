package com.Anchored.mylife.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.R
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassStyle
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 应用内密码输入：一排圆点 + 一块数字键盘。
 *
 * 刻意不用系统输入法：密码框一旦拉起输入法，键盘高度、候选栏、第三方输入法的
 * 联想词都会挤进版面，锁屏和解锁都要多一步"弹键盘 / 收键盘"；换成应用内的数字
 * 键盘之后，输密码这件事完全在应用里闭环——按几下就是几位，不存在输入法状态。
 *
 * 数字是**直接按进来的**，不走文本输入：所以没有"粘贴进来一长串""输入法自动补全"
 * 这类需要过滤的输入，位数也只可能落在 [AppPin.MIN_LENGTH]..[AppPin.MAX_LENGTH] 之间。
 */
@Composable
fun AppPinInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    minLength: Int = AppPin.MIN_LENGTH,
    maxLength: Int = AppPin.MAX_LENGTH
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PinDots(
            value = value,
            isError = isError,
            minLength = minLength,
            maxLength = maxLength
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        PinKeypad(
            onDigit = { digit -> onValueChange((value + digit).take(maxLength)) },
            onBackspace = { onValueChange(value.dropLast(1)) },
            onClear = { onValueChange("") },
            enabled = enabled,
            // 键盘比整屏窄：三列按键铺到 400dp 就成了三个大方块，按起来反而不好找
            // （widthIn 必须在 fillMaxWidth 之前：父容器一旦把宽度定死，后面的上限就不起作用了）
            modifier = Modifier
                .widthIn(max = Sizes.pinPad)
                .fillMaxWidth()
        )

        if (supportingText != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = supportingText,
                style = AppTheme.type.bodySmall,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.textTertiary
            )
        }
    }
}

/**
 * 已输入的位数。
 *
 * 显示 `max(已输入, 最少位数)` 个点：一开始是 4 个空位，输满 4 位之后用户如果
 * 还想接着输，点会跟着长出来，一直到上限。这样"最少 4 位"这条规则不用写文字说明，
 * 空位本身就在说这件事。
 */
@Composable
fun PinDots(
    value: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    minLength: Int = AppPin.MIN_LENGTH,
    maxLength: Int = AppPin.MAX_LENGTH
) {
    val colors = AppTheme.colors
    val slots = value.length.coerceAtLeast(minLength).coerceAtMost(maxLength)
    val description = stringResource(R.string.pin_input_dots, value.length)

    Row(
        // 读屏只念"已输入几位"，不去念那几个圆点
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(slots) { index ->
            val filled = index < value.length
            val dotColor by animateColorAsState(
                targetValue = when {
                    !filled -> Color.Transparent
                    isError -> colors.error
                    else -> colors.accent
                },
                animationSpec = tween(AppMotion.Base, easing = AppMotion.Standard),
                label = "pinDot"
            )

            Box(
                modifier = Modifier
                    .size(Sizes.pinDot)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = if (filled) 0.dp else Sizes.hairline,
                        color = if (filled) Color.Transparent else colors.textTertiary,
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * 应用内数字键盘：1–9 / 清空 / 0 / 退格。
 *
 * 键面本身就是玻璃（和按钮同一种材质），所以键盘在页面上、对话框里、锁屏上
 * 都是一个观感，不会因为宿主换了就变回平面。
 */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val digitRows = remember {
        listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9')
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        digitRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                row.forEach { digit ->
                    PinKey(
                        onClick = { onDigit(digit) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = digit.toString(),
                            style = AppTheme.type.h3,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            PinKey(
                onClick = { onClear?.invoke() },
                // 没输东西时清空没有意义，但位置要留着，不然三个键会左右乱跳
                enabled = enabled && onClear != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.pin_key_clear),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }

            PinKey(
                onClick = { onDigit('0') },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "0",
                    style = AppTheme.type.h3,
                    color = colors.textPrimary
                )
            }

            PinKey(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                val deleteDescription = stringResource(R.string.pin_key_delete)
                BackspaceGlyph(
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(Sizes.iconLg)
                        .clearAndSetSemantics {
                            contentDescription = deleteDescription
                        }
                )
            }
        }
    }
}

/** 一个键。开着液态玻璃时键面是玻璃，关掉就是一块普通底 */
@Composable
private fun PinKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val glassEnabled = LocalLiquidGlassEnabled.current

    val body: @Composable BoxScope.() -> Unit = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            label()
        }
    }

    if (glassEnabled && enabled) {
        GlassSurface(
            backdrop = LocalLiquidGlassBackdrop.current,
            style = rememberKeyGlassStyle(),
            shape = shape,
            cornerRadius = Radius.md,
            modifier = modifier
                .height(Sizes.pinKey)
                .clickable(onClick = onClick),
            content = body
        )
        return
    }

    Box(
        modifier = modifier
            .height(Sizes.pinKey)
            .clip(shape)
            .background(if (enabled) colors.surfaceSunken else colors.surface)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        label()
    }
}

/**
 * 键面玻璃：比按钮再薄一点，一屏里有十几个键，厚了会显得吵。
 *
 * 兜底用 [com.Anchored.mylife.ui.theme.AppColors.surface] 而不是 surfaceSunken：
 * 键盘只会出现在弹窗里和锁屏上，这两处的底色一头是纯白、一头是纯黑，
 * 只有"比页面抬一档的表面色"在四种明暗组合里都还看得见。
 */
@Composable
private fun rememberKeyGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surface) {
        LiquidGlassStyle(
            refractionHeight = 8.dp,
            refractionAmount = -8.dp,
            dispersion = 0.34f,
            depth = 0.4f,
            tint = glass.tint,
            sheen = glass.sheen,
            rimTop = glass.rimTop,
            rimBottom = glass.rimBottom,
            frosted = colors.surface
        )
    }
}

/**
 * 退格键的图形。
 *
 * 不用图标字体：核心图标集里没有退格（那是 material-icons-extended），
 * 为一个键引一整个图标库不划算，直接画更省。
 */
@Composable
private fun BackspaceGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stroke = width * 0.09f

        val body = Path().apply {
            moveTo(width * 0.94f, height * 0.14f)
            lineTo(width * 0.36f, height * 0.14f)
            lineTo(width * 0.06f, height * 0.5f)
            lineTo(width * 0.36f, height * 0.86f)
            lineTo(width * 0.94f, height * 0.86f)
            close()
        }
        drawPath(
            path = body,
            color = tint,
            style = Stroke(width = stroke, join = StrokeJoin.Round)
        )

        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.34f),
            end = androidx.compose.ui.geometry.Offset(width * 0.76f, height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = androidx.compose.ui.geometry.Offset(width * 0.76f, height * 0.34f),
            end = androidx.compose.ui.geometry.Offset(width * 0.5f, height * 0.66f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
