package com.Anchored.mylife.ui.components

import com.Anchored.mylife.R

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Text
import com.Anchored.mylife.ui.theme.AppTheme

/**
 * 「标记为已完成」前的二次确认。
 *
 * 只在设置里打开「完成前二次确认」时才会出现，用在列表的一键勾选上——
 * 那是最容易误触的地方。详情页的「标记完成」是有意为之的动作，不重复问。
 */
@Composable
fun CompletionConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = stringResource(R.string.complete_confirm_title),
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
        confirmText = stringResource(R.string.complete_confirm_action),
        dismissText = stringResource(R.string.common_cancel),
        content = {
            Text(
                text = stringResource(R.string.complete_confirm_message),
                style = AppTheme.type.body,
                color = AppTheme.colors.textSecondary
            )
        }
    )
}
