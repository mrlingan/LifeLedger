package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.Anchored.mylife.R
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.launch

/** 只收数字，并且不超过上限——粘贴一大串也不会把框撑爆 */
private fun digitsOnly(input: String): String =
    input.filter { it.isDigit() }.take(AppPin.MAX_LENGTH)

private val pinKeyboard = KeyboardOptions(
    keyboardType = KeyboardType.NumberPassword,
    imeAction = ImeAction.Done
)

/**
 * 设置 / 修改应用密码（4–9 位数字）。
 *
 * 格式、两次输入是否一致这类判断就地给反馈；"当前密码对不对"要跑一次
 * PBKDF2（约 100ms），所以交给 [verifyCurrent] —— 由调用方切到后台线程，
 * 界面这边只用管结果。
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

    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val formatError = stringResource(R.string.pin_error_format)
    val mismatchError = stringResource(R.string.pin_error_mismatch)
    val wrongCurrentError = stringResource(R.string.pin_error_current)

    fun save() {
        when {
            !AppPin.isWellFormed(newPin) -> error = formatError
            newPin != confirm -> error = mismatchError
            pinSet && current.isBlank() -> error = wrongCurrentError
            else -> {
                error = null
                busy = true
                scope.launch {
                    // 已经设过密码时，先确认当前密码：手机短暂离手也改不了密码
                    val ok = !pinSet || verifyCurrent(current)
                    busy = false
                    if (ok) {
                        onSave(newPin)
                        onDismiss()
                    } else {
                        error = wrongCurrentError
                    }
                }
            }
        }
    }

    AppDialog(
        title = stringResource(R.string.pin_title),
        onDismissRequest = onDismiss,
        onConfirm = { save() },
        confirmText = stringResource(R.string.common_save),
        dismissText = stringResource(R.string.common_cancel),
        content = {
            Column {
                if (pinSet) {
                    AppTextField(
                        value = current,
                        onValueChange = { current = digitsOnly(it) },
                        label = stringResource(R.string.pin_current),
                        singleLine = true,
                        password = true,
                        keyboardOptions = pinKeyboard,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }

                AppTextField(
                    value = newPin,
                    onValueChange = { newPin = digitsOnly(it) },
                    label = stringResource(R.string.pin_new),
                    supportingText = stringResource(R.string.pin_rule),
                    singleLine = true,
                    password = true,
                    keyboardOptions = pinKeyboard,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AppTextField(
                    value = confirm,
                    onValueChange = { confirm = digitsOnly(it) },
                    label = stringResource(R.string.pin_confirm),
                    singleLine = true,
                    password = true,
                    keyboardOptions = pinKeyboard,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = error.orEmpty(),
                        style = AppTheme.type.bodySmall,
                        color = colors.error
                    )
                }

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
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    fun remove() {
        if (busy) return
        if (current.isBlank()) {
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
        dismissText = stringResource(R.string.common_cancel),
        destructive = true,
        content = {
            Column {
                AppDialogText(stringResource(R.string.pin_remove_message))

                Spacer(modifier = Modifier.height(Spacing.lg))

                AppTextField(
                    value = current,
                    onValueChange = {
                        current = digitsOnly(it)
                        wrong = false
                    },
                    label = stringResource(R.string.pin_current),
                    singleLine = true,
                    password = true,
                    isError = wrong,
                    supportingText = if (wrong) {
                        stringResource(R.string.pin_error_current)
                    } else {
                        null
                    },
                    keyboardOptions = pinKeyboard,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
