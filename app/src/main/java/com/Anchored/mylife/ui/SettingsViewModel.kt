package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.data.settings.FontScaleChoice
import com.Anchored.mylife.data.settings.ListDensity
import com.Anchored.mylife.data.settings.MotionChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundImageUri: String? = null,
    /** 背景图片显示强度，0.05..0.90；越大图片越明显 */
    val backgroundImageOpacity: Float = AppSettings.BACKGROUND_OPACITY_DEFAULT,
    val appLockEnabled: Boolean = false,
    /** 是否已经设过应用密码（4–9 位数字） */
    val appPinSet: Boolean = false,
    val biometricAvailable: Boolean = false,
    val stats: BackupSummary = BackupSummary(),
    val appVersion: String = "",
    /**
     * 应用内语言，空字符串表示跟随系统——这是 AppCompat 自己的约定，
     * 所以这里不另做一层映射，免得两边对不上。
     */
    val languageTag: String = "",
    /** 个人资料：设置页顶部的资料卡要用 */
    val nickname: String = "",
    val signature: String = "",
    val avatarPath: String? = null,
    /** 显示与动效偏好 */
    val listDensity: ListDensity = ListDensity.STANDARD,
    val fontScale: FontScaleChoice = FontScaleChoice.STANDARD,
    val motion: MotionChoice = MotionChoice.FULL,
    /** 每日提醒 */
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0
) {
    /** 设置页里可选的语言 */
    val language: LanguageChoice get() = LanguageChoice.of(languageTag)
}

/**
 * 设置页里可选的语言。
 *
 * 只列应用真正有资源的语言：英文是默认资源（values/），中文在 values-zh。
 * 加新语言时这里和 res/xml/locales_config.xml 都要补。
 */
enum class LanguageChoice(val tag: String, @param:StringRes val labelRes: Int) {
    SYSTEM("", R.string.settings_language_system),
    CHINESE("zh", R.string.language_name_zh),
    ENGLISH("en", R.string.language_name_en);

    companion object {
        fun of(tag: String): LanguageChoice = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings: AppSettings = repositories.settings

    private val stats = MutableStateFlow(BackupSummary())
    private val version = MutableStateFlow("")
    private val language = MutableStateFlow(AppCompatDelegate.getApplicationLocales().toLanguageTags())

    private val biometricAvailable: Boolean = runCatching {
        BiometricManager.from(application).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }.getOrDefault(false)

    // combine 直接接的上限是 5 个流，这里先把同类项打包，免得层层嵌套
    private val appearanceSettings: Flow<AppearanceSettings> = combine(
        settings.themeMode,
        settings.backgroundImageUri,
        settings.backgroundImageOpacity
    ) { themeMode, backgroundImageUri, backgroundImageOpacity ->
        AppearanceSettings(themeMode, backgroundImageUri, backgroundImageOpacity)
    }

    private val coreSettings: Flow<CoreSettings> = combine(
        appearanceSettings,
        settings.appLockEnabled,
        settings.appPinHash,
        stats,
        version
    ) { appearance, appLockEnabled, appPinHash, backupSummary, appVersion ->
        CoreSettings(
            themeMode = appearance.themeMode,
            backgroundImageUri = appearance.backgroundImageUri,
            backgroundImageOpacity = appearance.backgroundImageOpacity,
            appLockEnabled = appLockEnabled,
            appPinHash = appPinHash,
            stats = backupSummary,
            version = appVersion
        )
    }

    private val profileSummary: Flow<ProfileSummary> = combine(
        settings.nickname,
        settings.signature,
        settings.avatarPath
    ) { nickname, signature, avatarPath ->
        ProfileSummary(nickname, signature, avatarPath)
    }

    private val lookSettings: Flow<Triple<ListDensity, FontScaleChoice, MotionChoice>> = combine(
        settings.listDensity,
        settings.fontScale,
        settings.motion
    ) { density, fontScale, motion ->
        Triple(density, fontScale, motion)
    }

    private val reminderSettings: Flow<Triple<Boolean, Int, Int>> = combine(
        settings.reminderEnabled,
        settings.reminderHour,
        settings.reminderMinute
    ) { enabled, hour, minute ->
        Triple(enabled, hour, minute)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        coreSettings,
        language,
        profileSummary,
        lookSettings,
        reminderSettings
    ) { core, languageTag, profile, look, reminder ->
        SettingsUiState(
            themeMode = core.themeMode,
            backgroundImageUri = core.backgroundImageUri,
            backgroundImageOpacity = core.backgroundImageOpacity,
            appLockEnabled = core.appLockEnabled,
            appPinSet = core.appPinHash != null,
            biometricAvailable = biometricAvailable,
            stats = core.stats,
            appVersion = core.version,
            languageTag = languageTag,
            nickname = profile.nickname,
            signature = profile.signature,
            avatarPath = profile.avatarPath,
            listDensity = look.first,
            fontScale = look.second,
            motion = look.third,
            reminderEnabled = reminder.first,
            reminderHour = reminder.second,
            reminderMinute = reminder.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            stats.value = runCatching { repositories.backupManager.stats() }
                .getOrDefault(BackupSummary())
        }
        viewModelScope.launch {
            val context = getApplication<Application>()
            version.value = runCatching {
                context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty()
            }.getOrDefault("")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settings.setThemeMode(mode)
    }
    fun setBackgroundImageUri(uri: String?) = settings.setBackgroundImageUri(uri)

    fun setBackgroundImageOpacity(opacity: Float) = settings.setBackgroundImageOpacity(opacity)

    fun setAppLockEnabled(enabled: Boolean) {
        // 设备不支持验证方式时不允许开启，否则用户会被关在外面
        if (!enabled || biometricAvailable) {
            settings.setAppLockEnabled(enabled)
        }
    }

    /**
     * 设置应用密码。
     *
     * 派生 PBKDF2 要 100ms 上下，所以放到后台线程。
     * 设了密码就一定上锁（见 AppLockGate），不用再动指纹那个开关。
     */
    fun setAppPin(pin: String) {
        viewModelScope.launch(Dispatchers.Default) {
            settings.setAppPin(pin)
        }
    }

    fun clearAppPin() {
        settings.clearAppPin()
    }

    /** 校验当前密码；由对话框调用，所以做成挂起函数，别占主线程 */
    suspend fun verifyAppPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        AppPin.verify(pin, settings.appPinHash.value)
    }

    /**
     * 切换应用内语言。
     *
     * 传空字符串表示跟随系统。AppCompat 会自己把选择持久化，
     * 并让当前页面重建一次，所以这里不用再存一份。
     */
    fun setLanguage(choice: LanguageChoice) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(choice.tag))
        language.value = choice.tag
    }

    fun setListDensity(value: ListDensity) = settings.setListDensity(value)

    fun setFontScale(value: FontScaleChoice) = settings.setFontScale(value)

    fun setMotion(value: MotionChoice) = settings.setMotion(value)
}

private data class AppearanceSettings(
    val themeMode: ThemeMode,
    val backgroundImageUri: String?,
    val backgroundImageOpacity: Float
)

/** 设置页里几个"成组"的状态，打包后一起流给界面 */
private data class CoreSettings(
    val themeMode: ThemeMode,
    val backgroundImageUri: String?,
    val backgroundImageOpacity: Float,
    val appLockEnabled: Boolean,
    val appPinHash: String?,
    val stats: BackupSummary,
    val version: String
)

private data class ProfileSummary(
    val nickname: String,
    val signature: String,
    val avatarPath: String?
)
