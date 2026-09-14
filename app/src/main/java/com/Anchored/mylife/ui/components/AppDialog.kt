package com.Anchored.mylife.ui.components

import com.Anchored.mylife.R

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius

/**
 * 统一对话框。
 *
 * 宿主仍是 AlertDialog（它负责遮罩与窗口），但形状、配色、字体、按钮
 * 全部走设计系统，所以看起来不是系统默认弹窗。
 *
 * 对话框活在自己的窗口里，采不到它背后那个窗口的页面——玻璃的采样层是主窗口里
 * 录的，硬要采样只会画到错误的位置。所以这里把采样源显式置空：对话框内部的分段
 * 控件、按钮仍然是玻璃材质（见 [com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled]），
 * 只是折射退化成一层磨砂——也就是"没设背景图时"那种观感。
 *
 * @param destructive 删除这类不可逆操作，确认按钮会变成危险色
 * @param confirmEnabled 确认按钮是否可用（多步流程用它卡住"还没输完"的那一步）
 * @param content 正文。注意宿主 AlertDialog 的正文槽是个 Box：
 *   要放多段内容时自己包一层 Column，否则会叠在一起
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
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLiquidGlassBackdrop provides null) {
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
                    variant = if (destructive) {
                        AppButtonVariant.Destructive
                    } else {
                        AppButtonVariant.Primary
                    },
                    enabled = confirmEnabled
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
