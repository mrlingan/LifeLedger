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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withStateAtLeast
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppPinInput
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 退到后台多久之后重新上锁 */
private const val AUTO_LOCK_DELAY_MILLIS = 30_000L

/** 连续输错几次之后进入冷却 */
private const val MAX_PIN_ATTEMPTS = 5

/** 冷却时长 */
private const val PIN_COOLDOWN_MILLIS = 30_000L

/**
 * 应用锁。
 *
 * 两把锁可以同时用，互不替代：
 * - 应用密码（4–9 位数字）：设了就一定上锁
 * - 指纹 / 面容：开关打开且设备支持时，锁屏上多一条解锁途径
 *
 * 什么时候锁：
 * - 冷启动
 * - 离开前台就锁。**不再给 30 秒宽限期**：任务被系统留在后台时（从最近任务
 *   划掉、按 Home 再点图标），Activity 可能没有重建，只靠冷启动判断会漏锁
 * - 例外：应用自己拉起的相册 / 文件选择（见 [ExternalActivity]），回来不用重解锁
 * - 兜底：万一没收到 ON_STOP，退到后台超过 30 秒再回来也会锁
 *
 * @param pinSet 用户是否设过应用密码
 * @param verifyPin 校验密码；内部会切到后台线程跑 PBKDF2，界面线程不受影响
 * @param onLockedChange 锁定状态变化回调。玻璃底栏画在内容之外、锁屏只盖住内容，
 *   所以宿主需要知道「现在锁着」，好在锁屏期间把底栏一起收起来
 */
@Composable
fun AppLockGate(
    enabled: Boolean,
    pinSet: Boolean = false,
    verifyPin: (String) -> Boolean = { false },
    onLockedChange: (Boolean) -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val supported = remember(context) { biometricAvailable(context) }
    // 设备「支持」指纹 / 面容，不等于用户「开启」了它。开关没开时，
    // 锁屏既不能自动弹系统验证，也不能留下一条指纹解锁的入口。
    val biometricUnlock = enabled && supported
    // 设了密码就一定锁；指纹 / 面容是另一把，两个可以同时生效
    val lockActive = pinSet || biometricUnlock

    var unlocked by remember { mutableStateOf(!lockActive) }
    var backgroundedAt by remember { mutableStateOf(0L) }

    LaunchedEffect(lockActive) {
        unlocked = !lockActive
    }

    DisposableEffect(lifecycleOwner, lockActive) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    backgroundedAt = System.currentTimeMillis()
                    // 自己去相册 / 文件选择不算离开；其余情况立刻上锁，
                    // 不然"划掉再打开"看起来就像锁没生效
                    if (lockActive && !ExternalActivity.isFresh()) {
                        unlocked = false
                    }
                }

                Lifecycle.Event.ON_START -> {
                    ExternalActivity.clear()
                    val elapsed = System.currentTimeMillis() - backgroundedAt
                    if (lockActive && backgroundedAt > 0L && elapsed > AUTO_LOCK_DELAY_MILLIS) {
                        unlocked = false
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locked = lockActive && !unlocked

    LaunchedEffect(locked) { onLockedChange(locked) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 锁着的时候连语义树一起摘掉：光盖住画面不够，
        // 否则 TalkBack 之类读屏（以及无障碍自动化）还能念出底下的记录
        Box(
            modifier = if (locked) Modifier.clearAndSetSemantics { } else Modifier
        ) {
            content()
        }

        if (locked) {
            LockScreen(
                biometricUnlock = biometricUnlock,
                pinSet = pinSet,
                verifyPin = verifyPin,
                onUnlock = { unlocked = true }
            )
        }
    }
}

@Composable
private fun LockScreen(
    biometricUnlock: Boolean,
    pinSet: Boolean,
    verifyPin: (String) -> Boolean,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var failedAttempts by remember { mutableStateOf(0) }
    var cooldownUntil by remember { mutableStateOf(0L) }
    var cooldownLeft by remember { mutableStateOf(0) }

    val activity = remember(context) { context.findFragmentActivity() }
    val promptTitle = stringResource(R.string.lock_prompt_title)
    val promptSubtitle = stringResource(R.string.lock_prompt_subtitle)
    val failedText = stringResource(R.string.lock_failed)
    val wrongPinText = stringResource(R.string.lock_pin_wrong)
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

    // 设了密码就等用户输入，不自动弹指纹；只有"仅生物识别"这档才进来自动弹一次。
    // 宿主不是 FragmentActivity 且没有密码时直接放行，不做无法解除的锁定。
    // 用户没开生物识别时，这里连系统验证都不会弹，锁屏只剩输入密码一条路。
    LaunchedEffect(prompt, pinSet, biometricUnlock) {
        if (pinSet || !biometricUnlock) return@LaunchedEffect
        if (prompt == null) {
            onUnlock()
            return@LaunchedEffect
        }
        // 锁屏可能是在 ON_STOP 里挂上的，那时候弹指纹会被系统直接取消：
        // 等回到前台（RESUMED）再弹
        lifecycleOwner.lifecycle.withStateAtLeast(Lifecycle.State.RESUMED) {
            prompt.authenticate(promptInfo)
        }
    }

    // 冷却倒计时
    LaunchedEffect(cooldownUntil) {
        while (true) {
            val left = ((cooldownUntil - System.currentTimeMillis()) / 1000).toInt()
            cooldownLeft = left.coerceAtLeast(0)
            if (left <= 0) break
            delay(1000)
        }
    }

    fun submit() {
        if (checking || cooldownLeft > 0) return
        if (pin.length < AppPin.MIN_LENGTH) {
            errorMessage = wrongPinText
            return
        }
        checking = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { verifyPin(pin) }
            checking = false
            if (ok) {
                onUnlock()
            } else {
                pin = ""
                errorMessage = wrongPinText
                failedAttempts++
                if (failedAttempts >= MAX_PIN_ATTEMPTS) {
                    failedAttempts = 0
                    cooldownUntil = System.currentTimeMillis() + PIN_COOLDOWN_MILLIS
                }
            }
        }
    }

    val hint = when {
        cooldownLeft > 0 -> stringResource(R.string.lock_pin_cooldown, cooldownLeft)
        errorMessage != null -> errorMessage.orEmpty()
        pinSet -> stringResource(R.string.lock_pin_hint)
        else -> stringResource(R.string.lock_hint)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 锁屏必须是不透明的：pageColor 在设了背景图时是透明的，
            // 那样底下的记录会直接透过锁屏露出来
            .background(AppTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        // 锁屏自己是整屏不透明的，背后那层采样源（背景图）在这里根本不出现，
        // 键盘上的玻璃就不该去采它——否则每个键都会变成一扇看见壁纸的小窗
        CompositionLocalProvider(LocalLiquidGlassBackdrop provides null) {
            Column(
                modifier = Modifier
                    .padding(horizontal = Spacing.xxl)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.lock_locked),
                    style = AppTheme.type.h2,
                    color = AppTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = hint,
                    style = AppTheme.type.body,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (pinSet) {
                    Spacer(modifier = Modifier.height(Spacing.xl))

                    AppPinInput(
                        value = pin,
                        onValueChange = { input ->
                            pin = input
                            errorMessage = null
                        },
                        isError = errorMessage != null,
                        enabled = !checking && cooldownLeft == 0,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(Spacing.lg))

                    AppButton(
                        text = stringResource(R.string.lock_action),
                        onClick = { submit() },
                        enabled = !checking &&
                            cooldownLeft == 0 &&
                            pin.length >= AppPin.MIN_LENGTH
                    )

                    if (biometricUnlock) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        AppButton(
                            text = stringResource(R.string.lock_pin_biometric),
                            onClick = { prompt?.authenticate(promptInfo) },
                            variant = AppButtonVariant.Text
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(Spacing.xl))

                    AppButton(
                        text = stringResource(R.string.lock_action),
                        onClick = { prompt?.authenticate(promptInfo) }
                    )
                }
            }
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
internal fun Context.findFragmentActivity(): FragmentActivity? {
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
