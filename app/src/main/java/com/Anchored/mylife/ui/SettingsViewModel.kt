package com.Anchored.mylife.ui

import android.app.Application
import androidx.biometric.BiometricManager
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
    val appVersion: String = ""
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings: AppSettings = repositories.settings

    private val stats = MutableStateFlow(BackupSummary())
    private val version = MutableStateFlow("")

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
        version
    ) { themeMode, appLockEnabled, backupSummary, appVersion ->
        SettingsUiState(
            themeMode = themeMode,
            appLockEnabled = appLockEnabled,
            biometricAvailable = biometricAvailable,
            stats = backupSummary,
            appVersion = appVersion
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
}
