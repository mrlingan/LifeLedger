package com.Anchored.mylife.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 深色模式偏好 */
enum class ThemeMode {
    /** 跟随系统 */
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * 应用设置存储。
 *
 * 只存偏好，不碰业务数据，所以和 Room 分开放在 SharedPreferences 里。
 * 对外暴露 StateFlow，界面可以直接订阅。
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    private fun readThemeMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(
            prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        )
    }.getOrDefault(ThemeMode.SYSTEM)

    private companion object {
        const val PREFS_NAME = "lifeledger_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_LOCK = "app_lock_enabled"
    }
}
