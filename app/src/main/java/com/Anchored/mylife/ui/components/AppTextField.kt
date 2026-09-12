package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 统一输入框。
 *
 * 刻意不用 Material 的 OutlinedTextField：它自带的下划线、轮廓和浮动标签
 * 是「Android 表单」观感的主要来源。这里用 BasicTextField 自己画容器。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    /** 密码模式：内容用圆点遮起来 */
    password: Boolean = false,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val borderColor = if (isError) colors.error else colors.border

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = AppTheme.type.caption,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (enabled) colors.surfaceSunken else colors.surface)
                .border(Sizes.hairline, borderColor, shape)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(Sizes.iconMd)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
            }

            BasicTextField(
        value = value,
        // 密码模式：只改显示，值本身不变
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = AppTheme.type.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = AppTheme.type.bodyLarge,
                                color = colors.textTertiary
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        if (supportingText != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = supportingText,
                style = AppTheme.type.caption,
                color = if (isError) colors.error else colors.textTertiary
            )
        }
    }
}
