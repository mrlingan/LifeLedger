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
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLockEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val stats: BackupSummary = BackupSummary(),
    val appVersion: String = "",
    /**
     * 应用内语言，空字符串表示跟随系统——这是 AppCompat 自己的约定，
     * 所以这里不另做一层映射，免得两边对不上。
     */
    val languageTag: String = ""
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

    val uiState: StateFlow<SettingsUiState> = combine(
        settings.themeMode,
        settings.appLockEnabled,
        stats,
        version,
        language
    ) { themeMode, appLockEnabled, backupSummary, appVersion, languageTag ->
        SettingsUiState(
            themeMode = themeMode,
            appLockEnabled = appLockEnabled,
            biometricAvailable = biometricAvailable,
            stats = backupSummary,
            appVersion = appVersion,
            languageTag = languageTag
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

    fun setAppLockEnabled(enabled: Boolean) {
        // 设备不支持验证方式时不允许开启，否则用户会被关在外面
        if (!enabled || biometricAvailable) {
            settings.setAppLockEnabled(enabled)
        }
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
}
