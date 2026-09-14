package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppPinInput
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.launch

/** 设置 / 修改密码时的三步 */
private enum class PinStep {
    /** 已经设过密码：先核对当前这一个 */
    Current,

    /** 输入新密码 */
    New,

    /** 再输一次，确认没按错 */
    Confirm
}

/**
 * 设置 / 修改应用密码（4–9 位数字）。
 *
 * 用**应用内数字键盘**（见 [AppPinInput]），不拉系统输入法：密码这件事不该受
 * 输入法状态影响——装了什么输入法、候选栏多高、会不会自动联想，都不该改变这个
 * 弹窗的样子。数字是直接按进来的，所以也不存在"粘贴进来一长串"要过滤的情况。
 *
 * 三步走，一步只问一件事：当前密码 → 新密码 → 再输一次。原来三格并排的表单
 * 在手机上本来就挤，换成键盘之后一屏只放一个输入，反而更清楚。
 *
 * "当前密码对不对"要跑一次 PBKDF2（约 100ms），所以交给 [verifyCurrent] ——
 * 由调用方切到后台线程，界面这边只用管结果。
 *
 * 移除密码不在这里：它是不可逆操作，走独立的 [AppPinRemoveDialog]，
 * 免得和「保存新密码」挤在同一个弹窗里、点错按钮。
 */
@Composable
fun AppPinDialog(
    pinSet: Boolean,
    verifyCurrent: suspend (String) -> Boolean,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(if (pinSet) PinStep.Current else PinStep.New) }
    var input by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val formatError = stringResource(R.string.pin_error_format)
    val mismatchError = stringResource(R.string.pin_error_mismatch)
    val wrongCurrentError = stringResource(R.string.pin_error_current)

    fun advance() {
        if (busy) return
        when (step) {
            PinStep.Current -> {
                if (input.length < AppPin.MIN_LENGTH) {
                    error = wrongCurrentError
                    return
                }
                busy = true
                scope.launch {
                    // 已经设过密码时，先确认当前密码：手机短暂离手也改不了密码
                    val ok = verifyCurrent(input)
                    busy = false
                    input = ""
                    if (ok) {
                        error = null
                        step = PinStep.New
                    } else {
                        error = wrongCurrentError
                    }
                }
            }

            PinStep.New -> {
                if (!AppPin.isWellFormed(input)) {
                    error = formatError
                    return
                }
                newPin = input
                input = ""
                error = null
                step = PinStep.Confirm
            }

            PinStep.Confirm -> {
                if (input != newPin) {
                    input = ""
                    error = mismatchError
                    return
                }
                onSave(newPin)
                onDismiss()
            }
        }
    }

    val hint = stringResource(
        when (step) {
            PinStep.Current -> R.string.pin_step_current
            PinStep.New -> R.string.pin_step_new
            PinStep.Confirm -> R.string.pin_step_confirm
        }
    )

    AppDialog(
        title = stringResource(R.string.pin_title),
        onDismissRequest = { if (!busy) onDismiss() },
        onConfirm = { advance() },
        confirmText = stringResource(
            if (step == PinStep.Confirm) R.string.common_save else R.string.pin_action_next
        ),
        // 还没输够位数就没有"下一步"可言
        confirmEnabled = !busy && input.length >= AppPin.MIN_LENGTH,
        content = {
            Column {
                Text(
                    text = hint,
                    style = AppTheme.type.body,
                    color = colors.textSecondary
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = error.orEmpty(),
                        style = AppTheme.type.bodySmall,
                        color = colors.error
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                AppPinInput(
                    value = input,
                    onValueChange = { value ->
                        input = value
                        error = null
                    },
                    enabled = !busy,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.pin_note),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    )
}

/**
 * 单独移除应用密码。
 *
 * 设置里「应用密码」那一项管设置与修改；移除是不可逆操作，单独一个入口，
 * 而且要输当前密码确认——手机短暂离手也降不了这道锁。
 */
@Composable
fun AppPinRemoveDialog(
    verifyCurrent: suspend (String) -> Boolean,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun remove() {
        if (busy) return
        if (current.length < AppPin.MIN_LENGTH) {
            wrong = true
            return
        }
        busy = true
        scope.launch {
            val ok = verifyCurrent(current)
            busy = false
            if (ok) {
                onRemove()
                onDismiss()
            } else {
                current = ""
                wrong = true
            }
        }
    }

    AppDialog(
        title = stringResource(R.string.pin_remove_title),
        onDismissRequest = { if (!busy) onDismiss() },
        onConfirm = { remove() },
        confirmText = stringResource(R.string.pin_remove),
        destructive = true,
        confirmEnabled = !busy && current.length >= AppPin.MIN_LENGTH,
        content = {
            Column {
                AppDialogText(stringResource(R.string.pin_remove_message))

                Spacer(modifier = Modifier.height(Spacing.lg))

                if (wrong) {
                    Text(
                        text = stringResource(R.string.pin_error_current),
                        style = AppTheme.type.bodySmall,
                        color = colors.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                AppPinInput(
                    value = current,
                    onValueChange = { value ->
                        current = value
                        wrong = false
                    },
                    enabled = !busy,
                    isError = wrong,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
