package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 退到后台多久之后重新上锁 */
private const val AUTO_LOCK_DELAY_MILLIS = 30_000L

/**
 * 应用锁。
 *
 * 开启后：
 * - 冷启动直接上锁
 * - 退到后台超过 30 秒再回来会重新上锁（短暂切出去选图片不会被打断）
 * - 设备没有指纹 / 面容 / 锁屏密码时**不上锁**，避免把用户自己关在外面
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val available = remember(context) { biometricAvailable(context) }

    var unlocked by remember { mutableStateOf(!enabled || !available) }
    var backgroundedAt by remember { mutableStateOf(0L) }

    LaunchedEffect(enabled, available) {
        unlocked = !enabled || !available
    }

    DisposableEffect(lifecycleOwner, enabled, available) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAt = System.currentTimeMillis()

                Lifecycle.Event.ON_START -> {
                    val elapsed = System.currentTimeMillis() - backgroundedAt
                    if (enabled && available && backgroundedAt > 0L && elapsed > AUTO_LOCK_DELAY_MILLIS) {
                        unlocked = false
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        if (enabled && available && !unlocked) {
            LockScreen(onUnlock = { unlocked = true })
        }
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val activity = remember(context) { context.findFragmentActivity() }
    val promptTitle = stringResource(R.string.lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)
    val failedText = stringResource(R.string.lock_failed)
    val promptInfo = remember(promptTitle, promptSubtitle) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    val prompt = remember(activity) {
        activity?.let { host ->
            BiometricPrompt(
                host,
                ContextCompat.getMainExecutor(host),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        errorMessage = null
                        onUnlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        errorMessage = errString.toString()
                    }

                    override fun onAuthenticationFailed() {
                        errorMessage = failedText
                    }
                }
            )
        }
    }

    // 进锁屏就弹一次；宿主不是 FragmentActivity 时直接放行，不做无法解除的锁定
    LaunchedEffect(prompt) {
        if (prompt == null) onUnlock() else prompt.authenticate(promptInfo)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.lock_locked),
                style = AppTheme.type.h2,
                color = AppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = errorMessage ?: stringResource(R.string.lock_hint),
                style = AppTheme.type.body,
                color = AppTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            AppButton(
                text = stringResource(R.string.lock_action),
                onClick = { prompt?.authenticate(promptInfo) }
            )
        }
    }
}

/** 设备是否具备可用的验证方式（指纹 / 面容 / 锁屏密码） */
fun biometricAvailable(context: Context): Boolean = runCatching {
    BiometricManager.from(context).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS
}.getOrDefault(false)

/** Compose 的 LocalContext 有时是包装过的 Context，这里向上找到 FragmentActivity */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return null
}

@Preview(showBackground = true, heightDp = 700, name = "锁屏")
@Composable
private fun LockScreenPreview() {
    LifeLedgerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.lock_locked),
                    style = AppTheme.type.h2,
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.lock_hint),
                    style = AppTheme.type.body,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
                AppButton(text = stringResource(R.string.lock_action), onClick = {})
            }
        }
    }
}
