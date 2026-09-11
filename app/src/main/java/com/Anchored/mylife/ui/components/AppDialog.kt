package com.Anchored.mylife.ui.components

import com.Anchored.mylife.R

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius

/**
 * 统一对话框。
 *
 * 宿主仍是 AlertDialog（它负责遮罩与窗口），但形状、配色、字体、按钮
 * 全部走设计系统，所以看起来不是系统默认弹窗。
 *
 * @param destructive 删除这类不可逆操作，确认按钮会变成危险色
 */
@Composable
fun AppDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = stringResource(R.string.common_ok),
    dismissText: String? = stringResource(R.string.common_cancel),
    destructive: Boolean = false,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(Radius.hero),
        containerColor = AppTheme.colors.surfaceElevated,
        titleContentColor = AppTheme.colors.textPrimary,
        textContentColor = AppTheme.colors.textSecondary,
        title = {
            Text(text = title, style = AppTheme.type.h3)
        },
        text = { content() },
        confirmButton = {
            AppButton(
                text = confirmText,
                onClick = onConfirm,
                variant = if (destructive) AppButtonVariant.Destructive else AppButtonVariant.Primary
            )
        },
        dismissButton = if (dismissText != null) {
            {
                AppButton(
                    text = dismissText,
                    onClick = onDismissRequest,
                    variant = AppButtonVariant.Text
                )
            }
        } else {
            null
        }
    )
}

/** 对话框正文的标准段落样式 */
@Composable
fun AppDialogText(text: String) {
    Text(
        text = text,
        style = AppTheme.type.body,
        color = AppTheme.colors.textSecondary
    )
}
