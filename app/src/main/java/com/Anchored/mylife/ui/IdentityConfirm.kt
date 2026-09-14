package com.Anchored.mylife.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.Anchored.mylife.R
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppPinInput
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 敏感操作（删除笔记这类）的二次确认 + 身份核对。
 *
 * 分两步，缺一不可：
 * 1. 先弹一个说清后果的确认框；
 * 2. 点确认之后再核对身份 ——
 *    - 设过应用密码：输密码
 *    - 没设密码但设备有指纹 / 面容 / 锁屏密码：走系统验证
 *    - 两个都没有：直接执行（否则这个功能等于用不了）
 *
 * 验证通过才会回调 [onConfirmed]；取消、关掉、验错都走 [onDismiss]。
 */
@Composable
fun IdentityConfirmDialog(
    visible: Boolean,
    title: String,
    message: String?,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    confirmText: String = stringResource(R.string.common_delete)
) {
    if (!visible) return

    val context = LocalContext.current
    val settings = remember(context) {
        RepositoryProvider.get(context.applicationContext).settings
    }
    val pinHash by settings.appPinHash.collectAsStateWithLifecycle()
    val biometrics = remember(context) { biometricAvailable(context) }
    var verifying by remember(visible) { mutableStateOf(false) }

    // 没设密码就走系统验证；两个都没有就直接放行
    LaunchedEffect(verifying, pinHash, biometrics) {
        if (!verifying || pinHash != null) return@LaunchedEffect
        if (!biometrics) {
            onConfirmed()
            return@LaunchedEffect
        }
        val host = context.findFragmentActivity()
        if (host == null) {
            onConfirmed()
            return@LaunchedEffect
        }
        if (authenticateBiometric(host, title)) onConfirmed() else onDismiss()
    }

    when {
        !verifying -> AppDialog(
            title = title,
            onDismissRequest = onDismiss,
            onConfirm = { verifying = true },
            confirmText = confirmText,
            destructive = true,
            content = { if (message != null) AppDialogText(message) }
        )

        pinHash != null -> PinVerifyDialog(
            title = stringResource(R.string.identity_verify),
            hashedPin = pinHash.orEmpty(),
            biometricsAvailable = biometrics,
            onDismiss = onDismiss,
            onVerified = onConfirmed
        )
    }
}

/** 输应用密码这一段。派生 PBKDF2 要 100ms 上下，所以放后台线程 */
@Composable
private fun PinVerifyDialog(
    title: String,
    hashedPin: String,
    biometricsAvailable: Boolean,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    fun submit() {
        if (checking) return
        if (pin.length < AppPin.MIN_LENGTH) {
            failed = true
            return
        }
        checking = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { AppPin.verify(pin, hashedPin) }
            checking = false
            if (ok) {
                onVerified()
            } else {
                failed = true
                pin = ""
            }
        }
    }

    // 设了密码也能改用指纹 / 面容 —— 要求就是"密码或者生物识别"二者其一
    fun verifyWithBiometric() {
        val host = context.findFragmentActivity() ?: return
        scope.launch {
            if (authenticateBiometric(host, title)) onVerified()
        }
    }

    AppDialog(
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = { submit() },
        confirmText = stringResource(R.string.common_ok),
        confirmEnabled = !checking && pin.length >= AppPin.MIN_LENGTH,
        content = {
            Column {
                AppPinInput(
                    value = pin,
                    onValueChange = { value ->
                        pin = value
                        failed = false
                    },
                    isError = failed,
                    supportingText = if (failed) {
                        stringResource(R.string.lock_pin_wrong)
                    } else {
                        stringResource(R.string.identity_pin_hint)
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth()
                )

                if (biometricsAvailable) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    AppButton(
                        text = stringResource(R.string.lock_pin_biometric),
                        onClick = { verifyWithBiometric() },
                        variant = AppButtonVariant.Text,
                        enabled = !checking
                    )
                }
            }
        }
    )
}

/** 系统验证（指纹 / 面容 / 锁屏密码），结果转成挂起函数的返回值 */
private suspend fun authenticateBiometric(host: FragmentActivity, title: String): Boolean =
    suspendCancellableCoroutine { continuation ->
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(host.getString(R.string.identity_prompt_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val prompt = BiometricPrompt(
            host,
            androidx.core.content.ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onAuthenticationFailed() {
                    // 单次没对上：系统自己会让用户重试，这里不做处理
                }
            }
        )

        continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        prompt.authenticate(promptInfo)
    }
