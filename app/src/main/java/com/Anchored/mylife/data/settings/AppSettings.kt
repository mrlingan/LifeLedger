package com.Anchored.mylife.data.settings

import android.content.Context
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.data.repository.AchievementRepository
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
 * 首次启动时选的起点。
 *
 * - [PRESETS]：把图鉴里的 109 条直接写进「我的成就」，一条条去达成
 * - [BLANK]：列表留空，只记录自己写下的
 * - [UNSET]：还没选过；已经有数据的老用户会被自动当成 [BLANK]
 */
enum class StartChoice {
    UNSET,
    PRESETS,
    BLANK
}

/** 列表密度：影响列表项之间的留白 */
enum class ListDensity {
    COMPACT,
    STANDARD,
    COMFY
}

/** 应用内字号缩放（在系统字号之上再乘一层） */
enum class FontScaleChoice(val scale: Float) {
    SMALL(0.9f),
    STANDARD(1f),
    LARGE(1.15f)
}

/** 动效强度 */
enum class MotionChoice {
    /** 优雅：更舒展的时长与柔和减速，适合沉浸式浏览 */
    ELEGANT,

    /** 完整：入场、进度增长、页面转场都有 */
    FULL,

    /** 精简：去掉入场错峰，时长缩短 */
    REDUCED,

    /** 关闭：只保留状态切换，不做动画 */
    OFF
}

/** 提醒频率 */
enum class ReminderFrequency {
    EVERY_DAY,
    WEEKDAYS
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

    private val _backgroundImageUri = MutableStateFlow(prefs.getString(KEY_BACKGROUND_IMAGE_URI, null))
    val backgroundImageUri: StateFlow<String?> = _backgroundImageUri.asStateFlow()

    /**
     * 背景图片的显示强度，取值 [BACKGROUND_OPACITY_MIN]..[BACKGROUND_OPACITY_MAX]。
     *
     * 数值越大图片越明显：主题层用 `1 - 这个值` 作为页面遮罩的不透明度，
     * 所以这里不叫"透明度"，免得和界面上的百分号对不上。
     */
    private val _backgroundImageOpacity = MutableStateFlow(
        prefs.getFloat(KEY_BACKGROUND_IMAGE_OPACITY, BACKGROUND_OPACITY_DEFAULT)
            .coerceIn(BACKGROUND_OPACITY_MIN, BACKGROUND_OPACITY_MAX)
    )
    val backgroundImageOpacity: StateFlow<Float> = _backgroundImageOpacity.asStateFlow()

    fun setBackgroundImageUri(uri: String?) {
        prefs.edit().putString(KEY_BACKGROUND_IMAGE_URI, uri).apply()
        _backgroundImageUri.value = uri
    }

    fun setBackgroundImageOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(BACKGROUND_OPACITY_MIN, BACKGROUND_OPACITY_MAX)
        prefs.edit().putFloat(KEY_BACKGROUND_IMAGE_OPACITY, clamped).apply()
        _backgroundImageOpacity.value = clamped
    }

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    /**
     * 应用密码的落盘值（PBKDF2 派生 + 盐，见 [com.Anchored.mylife.data.crypto.AppPin]）。
     *
     * null 表示没设过。界面只关心"设没设"，校验一律走 AppPin.verify。
     */
    private val _appPinHash = MutableStateFlow(prefs.getString(KEY_APP_PIN_HASH, null))
    val appPinHash: StateFlow<String?> = _appPinHash.asStateFlow()

    /**
     * 设置应用密码。
     *
     * 派生一次 PBKDF2 要 100ms 上下，**调用方要放到后台线程**（见 SettingsViewModel）。
     * 密码不合规直接拒绝，返回 false。
     */
    fun setAppPin(pin: String): Boolean {
        if (!AppPin.isWellFormed(pin)) return false
        val stored = AppPin.hash(pin)
        prefs.edit().putString(KEY_APP_PIN_HASH, stored).apply()
        _appPinHash.value = stored
        return true
    }

    fun clearAppPin() {
        prefs.edit().remove(KEY_APP_PIN_HASH).apply()
        _appPinHash.value = null
    }

    private val _nickname = MutableStateFlow(prefs.getString(KEY_NICKNAME, "").orEmpty())
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _signature = MutableStateFlow(prefs.getString(KEY_SIGNATURE, "").orEmpty())
    val signature: StateFlow<String> = _signature.asStateFlow()

    /** 头像副本的绝对路径；没设置过就是 null。文件本身由 ProfileImageStore 管理。 */
    private val _avatarPath = MutableStateFlow(prefs.getString(KEY_AVATAR_PATH, null))
    val avatarPath: StateFlow<String?> = _avatarPath.asStateFlow()

    private val _startChoice = MutableStateFlow(readStartChoice())
    val startChoice: StateFlow<StartChoice> = _startChoice.asStateFlow()

    fun setStartChoice(choice: StartChoice) {
        prefs.edit().putString(KEY_START_CHOICE, choice.name).apply()
        _startChoice.value = choice
    }

    private fun readStartChoice(): StartChoice = runCatching {
        StartChoice.valueOf(
            prefs.getString(KEY_START_CHOICE, StartChoice.UNSET.name) ?: StartChoice.UNSET.name
        )
    }.getOrDefault(StartChoice.UNSET)

    // ---------------- 本地数据加密 ----------------

    /**
     * 是否加密本地数据（成就标题/描述、笔记正文）。
     *
     * 默认关闭：加密是用户主动做的选择，不是被强加的。
     * 这个开关只决定"之后写进去的东西加不加密"，读路径永远都认密文，
     * 所以关掉它不会让已经加密的数据打不开。
     */
    private val _dataEncryptionEnabled = MutableStateFlow(prefs.getBoolean(KEY_DATA_ENCRYPTION, false))
    val dataEncryptionEnabled: StateFlow<Boolean> = _dataEncryptionEnabled.asStateFlow()

    fun setDataEncryptionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_ENCRYPTION, enabled).apply()
        _dataEncryptionEnabled.value = enabled
    }

    // ---------------- 成就设置 ----------------

    private val _defaultIcon = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_ICON, null) ?: AchievementRepository.DEFAULT_ICON
    )
    val defaultIcon: StateFlow<String> = _defaultIcon.asStateFlow()

    private val _confirmCompletion = MutableStateFlow(prefs.getBoolean(KEY_CONFIRM_COMPLETION, false))
    val confirmCompletion: StateFlow<Boolean> = _confirmCompletion.asStateFlow()

    /** 关注的图鉴分类：这些分类在首页和列表里排前面 */
    private val _favoriteCategories = MutableStateFlow(
        prefs.getStringSet(KEY_FAVORITE_CATEGORIES, emptySet())?.toSet().orEmpty()
    )
    val favoriteCategories: StateFlow<Set<String>> = _favoriteCategories.asStateFlow()

    fun setDefaultIcon(icon: String) {
        prefs.edit().putString(KEY_DEFAULT_ICON, icon).apply()
        _defaultIcon.value = icon
    }

    fun setConfirmCompletion(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_COMPLETION, value).apply()
        _confirmCompletion.value = value
    }

    fun setFavoriteCategories(categories: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITE_CATEGORIES, categories).apply()
        _favoriteCategories.value = categories
    }

    // ---------------- 显示与动效 ----------------

    private val _listDensity = MutableStateFlow(readEnum(KEY_LIST_DENSITY, ListDensity.STANDARD))
    val listDensity: StateFlow<ListDensity> = _listDensity.asStateFlow()

    private val _fontScale = MutableStateFlow(readEnum(KEY_FONT_SCALE, FontScaleChoice.STANDARD))
    val fontScale: StateFlow<FontScaleChoice> = _fontScale.asStateFlow()

    private val _motion = MutableStateFlow(readEnum(KEY_MOTION, MotionChoice.FULL))
    val motion: StateFlow<MotionChoice> = _motion.asStateFlow()

    fun setListDensity(value: ListDensity) {
        writeEnum(KEY_LIST_DENSITY, value)
        _listDensity.value = value
    }

    fun setFontScale(value: FontScaleChoice) {
        writeEnum(KEY_FONT_SCALE, value)
        _fontScale.value = value
    }

    fun setMotion(value: MotionChoice) {
        writeEnum(KEY_MOTION, value)
        _motion.value = value
    }

    // ---------------- 每日提醒 ----------------

    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 21))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _reminderFrequency = MutableStateFlow(
        readEnum(KEY_REMINDER_FREQUENCY, ReminderFrequency.EVERY_DAY)
    )
    val reminderFrequency: StateFlow<ReminderFrequency> = _reminderFrequency.asStateFlow()

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
        _reminderEnabled.value = enabled
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    fun setReminderFrequency(value: ReminderFrequency) {
        writeEnum(KEY_REMINDER_FREQUENCY, value)
        _reminderFrequency.value = value
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, fallback: T): T = runCatching {
        enumValueOf<T>(prefs.getString(key, fallback.name) ?: fallback.name)
    }.getOrDefault(fallback)

    private fun <T : Enum<T>> writeEnum(key: String, value: T) {
        prefs.edit().putString(key, value.name).apply()
    }

    /**
     * 保存个人资料。
     *
     * 昵称和签名去掉首尾空白再存；头像是文件，这里只记路径。
     * 备份恢复也会走这个方法，所以它必须一次把三项都写全。
     */
    fun setProfile(nickname: String, signature: String, avatarPath: String?) {
        val name = nickname.trim()
        val motto = signature.trim()
        prefs.edit()
            .putString(KEY_NICKNAME, name)
            .putString(KEY_SIGNATURE, motto)
            .putString(KEY_AVATAR_PATH, avatarPath)
            .apply()
        _nickname.value = name
        _signature.value = motto
        _avatarPath.value = avatarPath
    }

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

    companion object {
        /**
         * 背景图默认强度。
         *
         * 旧实现把页面底色的不透明度写死成 0.82，图片本身只剩 18%，
         * 页面再叠一层同样的底色后实际不到 4%，等于看不见。
         */
        const val BACKGROUND_OPACITY_DEFAULT = 0.45f
        const val BACKGROUND_OPACITY_MIN = 0.05f
        const val BACKGROUND_OPACITY_MAX = 0.90f

        private const val PREFS_NAME = "lifeledger_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BACKGROUND_IMAGE_URI = "background_image_uri"
        private const val KEY_BACKGROUND_IMAGE_OPACITY = "background_image_opacity"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_APP_PIN_HASH = "app_pin_hash"
        private const val KEY_NICKNAME = "profile_nickname"
        private const val KEY_SIGNATURE = "profile_signature"
        private const val KEY_AVATAR_PATH = "profile_avatar_path"
        private const val KEY_START_CHOICE = "start_choice"
        private const val KEY_DATA_ENCRYPTION = "data_encryption_enabled"
        private const val KEY_DEFAULT_ICON = "default_icon"
        private const val KEY_CONFIRM_COMPLETION = "confirm_completion"
        private const val KEY_FAVORITE_CATEGORIES = "favorite_categories"
        private const val KEY_LIST_DENSITY = "list_density"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_MOTION = "motion_level"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_REMINDER_FREQUENCY = "reminder_frequency"
    }
}
