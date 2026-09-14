package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.FontScaleChoice
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.data.settings.ListDensity
import com.Anchored.mylife.data.settings.MotionChoice
import androidx.annotation.StringRes
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppSnackbarHost
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppTopBarStyle
import com.Anchored.mylife.ui.components.LevelPill
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.components.PageBackdrop
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.components.liquidglass.rememberSelectionGlassStyle
import com.Anchored.mylife.ui.demo.DemoDataSeeder
import com.Anchored.mylife.ui.demo.DemoAccess
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 设置项行首的符号。
 *
 * 这一页没有用线性图标，用的是系统自带符号字体里的一组单线符号：一来它们和
 * "人生账本"的气质更近（像账本上手写的小记号，而不是 App 的功能图标），
 * 二来省掉了为"主题 / 液态玻璃 / 语言"这类概念各画一条矢量路径。
 *
 * 每个字符都是从 emoji 之外的符号区里挑的（几何图形、箭头、杂项符号、带圈字母），
 * 所以渲染出来永远是单色的，会乖乖跟着 tint 走，不会突然冒出一个彩色小图。
 * 换字符时请挑同样这些区段里的：挑到 emoji 区会被彩色字体接管，符号就不再受色。
 */
private object SettingGlyph {
    const val Achievements = "★"
    const val HomeSections = "✦"
    const val Theme = "✾"
    const val Glass = "◍"
    const val Language = "文"
    const val Display = "▤"
    const val Motion = "❖"
    const val Backup = "↻"
    const val Export = "⇅"
    const val Reminder = "◷"
    const val AppPin = "♙"
    const val PinRemove = "ⓧ"
    const val Biometric = "◎"
    const val DataSecurity = "♢"
    const val About = "ⓘ"
    const val Network = "☍"
    const val DemoData = "✚"
    const val ClearData = "✕"
}

/** 规划中的条目统一标注，避免点了没反应（存资源 id，在使用处取文案） */
private val SOON_LABEL = R.string.common_coming_soon

/** 开发者选项为什么可见，标在分组标题上，方便自己确认命中的是哪一条 */
@StringRes
private fun DemoAccess.Reason.labelRes(): Int = when (this) {
    DemoAccess.Reason.DEBUG_SIGNATURE -> R.string.settings_dev_reason_signature
    DemoAccess.Reason.DEBUGGABLE -> R.string.settings_dev_reason_debuggable
    DemoAccess.Reason.FORCED -> R.string.settings_dev_reason_forced
    DemoAccess.Reason.HIDDEN -> R.string.settings_sec_dev_tools_plain
}

/**
 * 设置。
 *
 * 分组：我的系统 / 外观 / 数据 / 通知 / 隐私与安全 / 关于。
 * 目前落地的是「主题」和「备份与恢复」，其余条目先占位。
 */
@Composable
fun SettingsRoute(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networkEnabled by viewModel.networkEnabled.collectAsStateWithLifecycle()
    val demoState by viewModel.demoState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.setBackgroundImage(uri) }

    LaunchedEffect(demoState.message) {
        val message = demoState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeDemoMessage()
    }

    SettingsScreen(
        uiState = uiState,
        demoState = demoState,
        snackbarHostState = snackbarHostState,
        // 设置从首页右上角进入，是二级页，给出明确的返回入口。
        onBack = { navController.popBackStack() },
        onOpenProfile = { navController.navigate("profile") },
        onOpenBackup = { navController.navigate("backup") },
        onOpenAchievementSettings = { navController.navigate("achievement_settings") },
        onOpenReminder = { navController.navigate("reminder") },
        onOpenDataSecurity = { navController.navigate("data_security") },
        onOpenHomeLayout = { navController.navigate(ROUTE_HOME_LAYOUT) },
        onThemeModeChange = viewModel::setThemeMode,
        onLiquidGlassChange = viewModel::setLiquidGlass,
        onPickBackground = {
            backgroundPicker.launchExternal(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemoveBackground = { viewModel.setBackgroundImage(null) },
        onBackgroundOpacityChange = viewModel::setBackgroundImageOpacity,
        onAppLockChange = viewModel::setAppLockEnabled,
        onPinSave = viewModel::setAppPin,
        onPinRemove = viewModel::clearAppPin,
        verifyPin = viewModel::verifyAppPin,
        onLanguageChange = viewModel::setLanguage,
        onListDensityChange = viewModel::setListDensity,
        onFontScaleChange = viewModel::setFontScale,
        onMotionChange = viewModel::setMotion,
        networkEnabled = networkEnabled,
        onNetworkEnabledChange = viewModel::setNetworkEnabled,
        onGenerateDemoData = viewModel::generateDemoData,
        onClearAllData = viewModel::clearAllData
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: (() -> Unit)?,
    onOpenProfile: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAchievementSettings: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenDataSecurity: () -> Unit,
    onOpenHomeLayout: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onPickBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    onBackgroundOpacityChange: (Float) -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onPinSave: (String) -> Unit,
    onPinRemove: () -> Unit,
    verifyPin: suspend (String) -> Boolean,
    onLanguageChange: (LanguageChoice) -> Unit,
    onListDensityChange: (ListDensity) -> Unit,
    onFontScaleChange: (FontScaleChoice) -> Unit,
    onMotionChange: (MotionChoice) -> Unit,
    networkEnabled: Boolean = false,
    onNetworkEnabledChange: (Boolean) -> Unit = {},
    /** 演示数据：只在可调试的包里出现，见下面的 demoDataAvailable */
    demoState: DemoDataState = DemoDataState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onGenerateDemoData: () -> Unit = {},
    onClearAllData: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    // 正式包（自己 keystore 签的、又不可调试）里这一组根本不渲染，
    // 不是渲染出来再禁用——少一个入口就少一次误触
    val demoReason = remember(context) { DemoAccess.reason(context) }
    val demoAvailable = demoReason != DemoAccess.Reason.HIDDEN
    var showAbout by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showPinRemove by remember { mutableStateOf(false) }
    var showDisplay by remember { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }
    var showQuickSettings by remember { mutableStateOf(false) }
    var showGenerateDemo by remember { mutableStateOf(false) }
    var showClearData by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppTheme.pageColor,
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 页面底色：和首页、成长页同一层很淡的蓝光。这一页的卡片也是玻璃，
            // 压在底色上才有"浮着"的感觉；纯灰底上玻璃只是一块灰白
            PageBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    // 底栏是浮在内容上的：末尾再多留出它压住的高度
                    .padding(bottom = Spacing.xxl + LocalBottomBarClearance.current)
            ) {
                SettingsHeader(
                    onBack = onBack,
                    onOpenQuickSettings = { showQuickSettings = true }
                )

            ProfileHeader(
                nickname = uiState.nickname,
                signature = uiState.signature,
                avatarPath = uiState.avatarPath,
                avatarPreset = uiState.avatarPreset,
                level = uiState.level,
                // 大标题那一条是通栏的（它自己带左右留白），从资料卡往下才回到栏内
                modifier = Modifier.padding(horizontal = Sizes.gutter),
                onClick = onOpenProfile
            )

            SettingsSection(
                title = stringResource(R.string.settings_sec_system),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.settings_achievements),
                    subtitle = stringResource(R.string.settings_achievements_desc),
                    leadingGlyph = SettingGlyph.Achievements,
                    showChevron = true,
                    onClick = onOpenAchievementSettings
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_sec_look),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.settings_home_sections),
                    subtitle = stringResource(R.string.settings_home_sections_desc),
                    leadingGlyph = SettingGlyph.HomeSections,
                    trailingText = stringResource(
                        R.string.settings_home_sections_value,
                        // 按"显示了几个板块"数：可重复的板块算一个，
                        // 不然加了三张自定义图片会显示成 5 / 6 这种读不通的分数
                        uiState.homeSections.map { it.type }.distinct().size,
                        HomeSection.entries.size
                    ),
                    showChevron = true,
                    onClick = onOpenHomeLayout
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_desc),
                    leadingGlyph = SettingGlyph.Theme,
                    trailingText = stringResource(uiState.themeMode.labelRes()),
                    showChevron = true,
                    onClick = { showTheme = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_liquid_glass),
                    subtitle = stringResource(R.string.settings_liquid_glass_desc),
                    leadingGlyph = SettingGlyph.Glass,
                    trailing = {
                        AppSwitch(
                            checked = uiState.liquidGlass,
                            onCheckedChange = onLiquidGlassChange
                        )
                    }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    leadingGlyph = SettingGlyph.Language,
                    trailingText = stringResource(uiState.language.labelRes),
                    showChevron = true,
                    onClick = { showLanguage = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_display),
                    subtitle = stringResource(R.string.settings_display_desc),
                    leadingGlyph = SettingGlyph.Display,
                    trailingText = stringResource(uiState.fontScale.labelRes()),
                    showChevron = true,
                    onClick = { showDisplay = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_animation),
                    subtitle = stringResource(R.string.settings_animation_desc),
                    leadingGlyph = SettingGlyph.Motion,
                    trailingText = stringResource(uiState.motion.labelRes()),
                    showChevron = true,
                    onClick = { showAnimation = true }
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_sec_data),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.backup_title),
                    subtitle = stringResource(
                        R.string.settings_backup_desc,
                        uiState.stats.achievementCount,
                        uiState.stats.noteCount,
                        uiState.stats.mediaCount
                    ),
                    leadingGlyph = SettingGlyph.Backup,
                    showChevron = true,
                    onClick = onOpenBackup
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_desc),
                    leadingGlyph = SettingGlyph.Export,
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_sec_notify),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.settings_reminder),
                    subtitle = stringResource(R.string.settings_reminder_desc),
                    leadingGlyph = SettingGlyph.Reminder,
                    trailingText = if (uiState.reminderEnabled) {
                        stringResource(
                            R.string.settings_reminder_time_value,
                            uiState.reminderHour,
                            uiState.reminderMinute
                        )
                    } else {
                        stringResource(R.string.common_off)
                    },
                    showChevron = true,
                    onClick = onOpenReminder
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_sec_privacy),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.settings_app_pin),
                    subtitle = stringResource(R.string.settings_app_pin_desc),
                    leadingGlyph = SettingGlyph.AppPin,
                    trailingText = if (uiState.appPinSet) {
                        stringResource(R.string.settings_app_pin_set)
                    } else {
                        stringResource(R.string.settings_app_pin_unset)
                    },
                    showChevron = true,
                    onClick = { showPin = true }
                )
                // 移除密码是不可逆操作，单独一项，不和「设置 / 修改」挤在一个弹窗里
                if (uiState.appPinSet) {
                    AppDivider()
                    AppSettingRow(
                        title = stringResource(R.string.settings_app_pin_remove),
                        subtitle = stringResource(R.string.settings_app_pin_remove_desc),
                        leadingGlyph = SettingGlyph.PinRemove,
                        destructive = true,
                        showChevron = true,
                        onClick = { showPinRemove = true }
                    )
                }
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_biometric_unlock),
                    subtitle = if (uiState.biometricAvailable) {
                        stringResource(R.string.settings_biometric_unlock_desc)
                    } else {
                        stringResource(R.string.settings_biometric_unlock_na)
                    },
                    leadingGlyph = SettingGlyph.Biometric,
                    enabled = uiState.biometricAvailable,
                    trailing = {
                        AppSwitch(
                            checked = uiState.appLockEnabled,
                            onCheckedChange = onAppLockChange,
                            enabled = uiState.biometricAvailable
                        )
                    }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_data_sec),
                    subtitle = stringResource(R.string.settings_data_sec_desc),
                    leadingGlyph = SettingGlyph.DataSecurity,
                    showChevron = true,
                    onClick = onOpenDataSecurity
                )
            }

            SettingsSection(
                title = stringResource(R.string.settings_sec_about),
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            ) {
                AppSettingRow(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_desc),
                    leadingGlyph = SettingGlyph.About,
                    showChevron = true,
                    onClick = { showAbout = true }
                )
            }

            // 演示数据：给截图和试用凑一批数据，正式包里没有这一组
            if (demoAvailable) {
                SettingsSection(
                    title = stringResource(
                        R.string.settings_sec_dev_tools,
                        stringResource(demoReason.labelRes())
                    ),
                    modifier = Modifier.padding(horizontal = Sizes.gutter)
                ) {
                    AppSettingRow(
                        title = stringResource(R.string.settings_network_enabled),
                        subtitle = stringResource(R.string.settings_network_enabled_desc),
                        leadingGlyph = SettingGlyph.Network,
                        trailing = {
                            AppSwitch(
                                checked = networkEnabled,
                                onCheckedChange = onNetworkEnabledChange
                            )
                        }
                    )
                    AppDivider()
                    AppSettingRow(
                        title = stringResource(R.string.settings_demo_generate),
                        subtitle = stringResource(R.string.settings_demo_generate_desc),
                        leadingGlyph = SettingGlyph.DemoData,
                        enabled = !demoState.busy,
                        showChevron = true,
                        onClick = { showGenerateDemo = true }
                    )
                    AppDivider()
                    AppSettingRow(
                        title = stringResource(R.string.settings_demo_clear),
                        subtitle = stringResource(R.string.settings_demo_clear_desc),
                        leadingGlyph = SettingGlyph.ClearData,
                        destructive = true,
                        enabled = !demoState.busy,
                        showChevron = true,
                        onClick = { showClearData = true }
                    )
                }
            }
            }
        }
    }

    if (showGenerateDemo) {
        AppDialog(
            title = stringResource(R.string.settings_demo_generate_title),
            onDismissRequest = { showGenerateDemo = false },
            onConfirm = {
                showGenerateDemo = false
                onGenerateDemoData()
            },
            confirmText = stringResource(R.string.settings_demo_generate)
        ) {
            AppDialogText(
                stringResource(
                    R.string.settings_demo_generate_body,
                    DemoDataSeeder.PREVIEW_COUNT
                )
            )
        }
    }

    if (showClearData) {
        AppDialog(
            title = stringResource(R.string.settings_demo_clear_title),
            onDismissRequest = { showClearData = false },
            onConfirm = {
                showClearData = false
                onClearAllData()
            },
            confirmText = stringResource(R.string.settings_demo_clear),
            destructive = true
        ) {
            AppDialogText(stringResource(R.string.settings_demo_clear_body))
        }
    }

    if (showAbout) {
        AboutDialog(
            appVersion = uiState.appVersion,
            onDismiss = { showAbout = false }
        )
    }

    if (showLanguage) {
        LanguageDialog(
            current = uiState.language,
            onPick = { choice ->
                showLanguage = false
                onLanguageChange(choice)
            },
            onDismiss = { showLanguage = false }
        )
    }

    if (showTheme) {
        ThemeDialog(
            themeMode = uiState.themeMode,
            backgroundImagePath = uiState.backgroundImagePath,
            backgroundOpacity = uiState.backgroundImageOpacity,
            onThemeModeChange = onThemeModeChange,
            onPickBackground = onPickBackground,
            onRemoveBackground = onRemoveBackground,
            onOpacityChange = onBackgroundOpacityChange,
            onDismiss = { showTheme = false }
        )
    }

    if (showPin) {
        AppPinDialog(
            pinSet = uiState.appPinSet,
            verifyCurrent = verifyPin,
            onSave = onPinSave,
            onDismiss = { showPin = false }
        )
    }

    if (showPinRemove) {
        AppPinRemoveDialog(
            verifyCurrent = verifyPin,
            onRemove = onPinRemove,
            onDismiss = { showPinRemove = false }
        )
    }

    if (showDisplay) {
        DisplayDialog(
            density = uiState.listDensity,
            fontScale = uiState.fontScale,
            onDensityChange = onListDensityChange,
            onFontScaleChange = onFontScaleChange,
            onDismiss = { showDisplay = false }
        )
    }

    if (showAnimation) {
        AnimationDialog(
            motion = uiState.motion,
            onMotionChange = onMotionChange,
            onDismiss = { showAnimation = false }
        )
    }

    if (showQuickSettings) {
        QuickSettingsDialog(
            themeMode = uiState.themeMode,
            liquidGlass = uiState.liquidGlass,
            onThemeModeChange = onThemeModeChange,
            onLiquidGlassChange = onLiquidGlassChange,
            onDismiss = { showQuickSettings = false }
        )
    }
}

/**
 * 快速设置：页头齿轮打开的那一格。
 *
 * 只放**看完就能决定**的两项——明暗模式与液态玻璃。它们是这一页里唯二
 * "改一下整屏立刻变样"的设置，其余各项要么要选文件、要么要去下一级页面，
 * 放进这个弹窗只会把这里变成一个小号的设置页。
 */
@Composable
private fun QuickSettingsDialog(
    themeMode: ThemeMode,
    liquidGlass: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLiquidGlassChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    AppDialog(
        title = stringResource(R.string.settings_quick),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column {
                OptionLabel(
                    title = stringResource(R.string.settings_theme_mode),
                    description = null
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppSegmentedControl(
                    options = ThemeMode.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = ThemeMode.entries.indexOf(themeMode),
                    onSelect = { index -> onThemeModeChange(ThemeMode.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_liquid_glass),
                            style = AppTheme.type.bodyLarge,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = stringResource(R.string.settings_liquid_glass_desc),
                            style = AppTheme.type.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(Spacing.md))
                    AppSwitch(
                        checked = liquidGlass,
                        onCheckedChange = onLiquidGlassChange
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.settings_quick_note),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    )
}

/** 语言选择。用对话框而不是分段控件：以后加语言不用重排版面 */
@Composable
private fun LanguageDialog(
    current: LanguageChoice,
    onPick: (LanguageChoice) -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = stringResource(R.string.settings_language),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column {
                LanguageChoice.entries.forEachIndexed { index, choice ->
                    if (index > 0) AppDivider()
                    LanguageOptionRow(
                        label = stringResource(choice.labelRes),
                        selected = choice == current,
                        onClick = { onPick(choice) }
                    )
                }
            }
        }
    )
}

@Composable
private fun LanguageOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    val glassEnabled = LocalLiquidGlassEnabled.current

    // 选中的那个语言是一小块任务色玻璃，和分段控件里的"选中"是同一种材质
    if (glassEnabled && selected) {
        GlassSurface(
            backdrop = LocalLiquidGlassBackdrop.current,
            style = rememberSelectionGlassStyle(),
            shape = shape,
            cornerRadius = Radius.sm,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Spacing.sm,
                        vertical = Spacing.md
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = AppTheme.type.bodyLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = colors.accentStrong,
                    modifier = Modifier.size(Sizes.iconMd)
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            // 左右也给一点：选中那一行是玻璃块，文字要落在同一条竖线上
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTheme.type.bodyLarge,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.accentStrong,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }
    }
}

/**
 * 主题：明暗模式 + 全局背景图片。
 *
 * 图片和强度都是即时生效的——对话框背后的页面本身就是预览，
 * 所以这里再放一张小图，让"数值越大越明显"有个直观参照。
 */
@Composable
private fun ThemeDialog(
    themeMode: ThemeMode,
    backgroundImagePath: String?,
    backgroundOpacity: Float,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPickBackground: () -> Unit,
    onRemoveBackground: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    AppDialog(
        title = stringResource(R.string.settings_theme),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OptionLabel(
                    title = stringResource(R.string.settings_theme_mode),
                    description = null
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppSegmentedControl(
                    options = ThemeMode.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = ThemeMode.entries.indexOf(themeMode),
                    onSelect = { index -> onThemeModeChange(ThemeMode.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                OptionLabel(
                    title = stringResource(R.string.settings_background_image),
                    description = stringResource(R.string.settings_background_image_desc)
                )
                Spacer(modifier = Modifier.height(Spacing.md))

                BackgroundPreview(path = backgroundImagePath, opacity = backgroundOpacity)

                Spacer(modifier = Modifier.height(Spacing.md))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppButton(
                        text = stringResource(
                            if (backgroundImagePath == null) {
                                R.string.settings_background_pick
                            } else {
                                R.string.settings_background_change
                            }
                        ),
                        onClick = onPickBackground,
                        variant = AppButtonVariant.Secondary
                    )
                    if (backgroundImagePath != null) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        AppButton(
                            text = stringResource(R.string.settings_background_remove),
                            onClick = onRemoveBackground,
                            variant = AppButtonVariant.Text
                        )
                    }
                }

                if (backgroundImagePath != null) {
                    Spacer(modifier = Modifier.height(Spacing.xl))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_background_opacity),
                            style = AppTheme.type.bodyLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(
                                R.string.settings_background_opacity_value,
                                (backgroundOpacity * 100).roundToInt()
                            ),
                            style = AppTheme.type.caption,
                            color = colors.textTertiary
                        )
                    }

                    Slider(
                        value = backgroundOpacity,
                        onValueChange = onOpacityChange,
                        valueRange = AppSettings.BACKGROUND_OPACITY_MIN..
                            AppSettings.BACKGROUND_OPACITY_MAX,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSunken
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.settings_background_opacity_note),
                        style = AppTheme.type.caption,
                        color = colors.textTertiary
                    )
                }
            }
        }
    )
}

/** 背景图小预览：按当前强度画一遍，没选图时显示提示文字 */
@Composable
private fun BackgroundPreview(path: String?, opacity: Float) {
    val colors = AppTheme.colors
    val thumbnail = rememberStoredImageThumbnail(path, sizePx = 360)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.wallpaperPreview)
            .clip(RoundedCornerShape(Radius.md))
            .background(colors.surfaceSunken),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(opacity)
            )
        } else {
            Text(
                text = stringResource(R.string.settings_background_empty),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }
    }
}

/**
 * 页头：大标题 + 一句话 + 右上角的快速设置。
 *
 * 和首页、成长页一样用大标题顶栏：进设置先要看清"这是哪一页"，
 * 而不是先撞上一条导航栏。返回箭头占左、齿轮占右，各在一头。
 */
@Composable
private fun SettingsHeader(
    onBack: (() -> Unit)?,
    onOpenQuickSettings: () -> Unit
) {
    AppTopBar(
        title = stringResource(R.string.settings_title),
        subtitle = stringResource(R.string.settings_subtitle),
        style = AppTopBarStyle.Large,
        onBack = onBack,
        actions = {
            // 齿轮给的是"最常用的那两项"：不想为一句话翻到下面去找
            AppIconButton(
                icon = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_quick),
                onClick = onOpenQuickSettings
            )
        }
    )
}

/**
 * 一个分组：小标题 + 一张容器卡片。卡片是液态玻璃，和首页板块同一种材质。
 *
 * 圆角比普通卡片大一档（[Radius.xl]）：这种卡里从上到下装了七八行，
 * 用普通卡片的 16dp 圆角会显得四角太尖、和里面那排 42dp 的图标方块也不是一个尺度。
 *
 * @param modifier 会**同时**套在小标题和卡片上：两样东西一起承接外部的
 *   左右留白（本页大标题那条是通栏的，所以留白只能加在这一层，不能加在整列上）。
 */
@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = AppTheme.type.caption,
        color = AppTheme.colors.textTertiary,
        modifier = modifier.padding(
            start = Spacing.xs,
            top = Spacing.xl,
            bottom = Spacing.sm
        )
    )

    AppCard(
        modifier = modifier,
        tone = AppCardTone.Glass,
        shape = RoundedCornerShape(Radius.xl),
        cornerRadius = Radius.xl,
        contentPadding = PaddingValues(0.dp),
        content = content
    )
}

@Composable
private fun AboutDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    AppDialog(
        title = stringResource(R.string.settings_about),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.settings_about_intro),
                    style = AppTheme.type.body,
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AboutLine(label = stringResource(R.string.settings_version), value = appVersion.ifBlank { "—" })
                AboutLine(
                    label = stringResource(R.string.settings_developer),
                    value = stringResource(R.string.settings_developer_value)
                )
                AboutLine(label = stringResource(R.string.settings_license), value = "MIT License")
                AboutLine(label = stringResource(R.string.settings_storage), value = stringResource(R.string.settings_storage_value))
                AboutLine(label = stringResource(R.string.settings_backup_format), value = stringResource(R.string.settings_backup_format_value))

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.settings_credits),
                    style = AppTheme.type.caption,
                    color = AppTheme.colors.textTertiary
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                CreditLine(text = stringResource(R.string.settings_credit_data))
                CreditLine(text = stringResource(R.string.settings_credit_fw))
                CreditLine(text = stringResource(R.string.settings_credit_icons))
            }
        }
    )
}

/** 致谢条目 */
@Composable
private fun CreditLine(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs)
    ) {
        Text(
            text = "·",
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textTertiary,
            modifier = Modifier.width(Spacing.md)
        )
        Text(
            text = text,
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxs)) {
        Text(
            text = label,
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textTertiary,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.weight(0.65f)
        )
    }
}

/**
 * 设置页顶部的资料卡：头像 + 昵称 + 阶段 + 签名，点一下进个人资料页。
 *
 * 没设置过昵称时退回「个人资料」这个标题和说明，
 * 让第一眼仍然是"这里可以设置资料"，而不是一片空白。
 *
 * 阶段牌子（Lv.N）和首页、「我的」是同一个数（见 [SettingsUiState.level]）：
 * 这一页是二级页，但"我走到哪一阶段了"在这三处必须是同一个答案。
 */
@Composable
private fun ProfileHeader(
    nickname: String,
    signature: String,
    avatarPath: String?,
    avatarPreset: AvatarPreset?,
    level: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.lg),
        tone = AppCardTone.Glass,
        shape = RoundedCornerShape(Radius.xl),
        cornerRadius = Radius.xl,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(
                name = nickname,
                path = avatarPath,
                preset = avatarPreset,
                size = Sizes.avatarLg
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = nickname.ifBlank { stringResource(R.string.settings_profile) },
                        style = AppTheme.type.h2,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // 名字长的时候先挤名字，阶段牌子始终留在行里：
                        // 牌子被挤掉就没法一眼看出走到哪了
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (nickname.isNotBlank()) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        LevelPill(level = level)
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = signature.ifBlank { stringResource(R.string.settings_profile_desc) },
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                // 这一枚跟着强调色走：它是"整张卡可以进去"的提示，
                // 比行尾那些"这一项还有下一页"的箭头更值得被看见
                tint = colors.accent
            )
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@StringRes
private fun ListDensity.labelRes(): Int = when (this) {
    ListDensity.COMPACT -> R.string.display_density_compact
    ListDensity.STANDARD -> R.string.display_density_standard
    ListDensity.COMFY -> R.string.display_density_comfy
}

@StringRes
private fun FontScaleChoice.labelRes(): Int = when (this) {
    FontScaleChoice.SMALL -> R.string.display_font_small
    FontScaleChoice.STANDARD -> R.string.display_font_standard
    FontScaleChoice.LARGE -> R.string.display_font_large
}

@StringRes
private fun MotionChoice.labelRes(): Int = when (this) {
    MotionChoice.ELEGANT -> R.string.animation_elegant
    MotionChoice.FULL -> R.string.animation_full
    MotionChoice.REDUCED -> R.string.animation_reduced
    MotionChoice.OFF -> R.string.animation_off
}

/** 显示：列表密度 + 字号缩放，两项都是即时生效 */
@Composable
private fun DisplayDialog(
    density: ListDensity,
    fontScale: FontScaleChoice,
    onDensityChange: (ListDensity) -> Unit,
    onFontScaleChange: (FontScaleChoice) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    AppDialog(
        title = stringResource(R.string.display_title),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column {
                OptionLabel(
                    title = stringResource(R.string.display_density),
                    description = null
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppSegmentedControl(
                    options = ListDensity.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = ListDensity.entries.indexOf(density),
                    onSelect = { index -> onDensityChange(ListDensity.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                OptionLabel(
                    title = stringResource(R.string.display_font),
                    description = null
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppSegmentedControl(
                    options = FontScaleChoice.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = FontScaleChoice.entries.indexOf(fontScale),
                    onSelect = { index -> onFontScaleChange(FontScaleChoice.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(R.string.display_note),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    )
}

/** 动画强度：完整 / 精简 / 关闭 */
@Composable
private fun AnimationDialog(
    motion: MotionChoice,
    onMotionChange: (MotionChoice) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    AppDialog(
        title = stringResource(R.string.animation_title),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column {
                AppSegmentedControl(
                    options = MotionChoice.entries.map { stringResource(it.labelRes()) },
                    selectedIndex = MotionChoice.entries.indexOf(motion),
                    onSelect = { index -> onMotionChange(MotionChoice.entries[index]) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(R.string.animation_note),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    )
}

@Composable
private fun OptionLabel(title: String, description: String?) {
    val colors = AppTheme.colors
    Text(
        text = title,
        style = AppTheme.type.bodyLarge,
        color = colors.textPrimary
    )
    if (description != null) {
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = description,
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "设置")
@Composable
private fun SettingsPreview() {
    LifeLedgerTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                themeMode = ThemeMode.SYSTEM,
                nickname = "小满",
                signature = "慢慢来，比较快。",
                level = 11,
                stats = BackupSummary(12, 7, 31),
                appVersion = "1.0"
            ),
            onBack = {},
            onOpenProfile = {},
            onOpenBackup = {},
            onOpenAchievementSettings = {},
            onOpenReminder = {},
            onOpenDataSecurity = {},
            onOpenHomeLayout = {},
            onThemeModeChange = {},
            onLiquidGlassChange = {},
            onPickBackground = {},
            onRemoveBackground = {},
            onBackgroundOpacityChange = {},
            onAppLockChange = {},
            onPinSave = {},
            onPinRemove = {},
            verifyPin = { true },
            onLanguageChange = {},
            onListDensityChange = {},
            onFontScaleChange = {},
            onMotionChange = {}
        )
    }
}
