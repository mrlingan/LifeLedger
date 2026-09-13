package com.Anchored.mylife.ui

import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.FontScaleChoice
import com.Anchored.mylife.data.settings.ListDensity
import com.Anchored.mylife.data.settings.MotionChoice
import androidx.annotation.StringRes
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/** 规划中的条目统一标注，避免点了没反应（存资源 id，在使用处取文案） */
private val SOON_LABEL = R.string.common_coming_soon

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
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> viewModel.setBackgroundImageUri(uri?.toString()) }

    SettingsScreen(
        uiState = uiState,
        // 设置是底部导航的 tab，不需要返回箭头
        onBack = null,
        onOpenProfile = { navController.navigate("profile") },
        onOpenBackup = { navController.navigate("backup") },
        onOpenAchievementSettings = { navController.navigate("achievement_settings") },
        onOpenReminder = { navController.navigate("reminder") },
        onOpenDataSecurity = { navController.navigate("data_security") },
        onThemeModeChange = viewModel::setThemeMode,
        onPickBackground = {
            backgroundPicker.launchExternal(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemoveBackground = { viewModel.setBackgroundImageUri(null) },
        onBackgroundOpacityChange = viewModel::setBackgroundImageOpacity,
        onAppLockChange = viewModel::setAppLockEnabled,
        onPinSave = viewModel::setAppPin,
        onPinRemove = viewModel::clearAppPin,
        verifyPin = viewModel::verifyAppPin,
        onLanguageChange = viewModel::setLanguage,
        onListDensityChange = viewModel::setListDensity,
        onFontScaleChange = viewModel::setFontScale,
        onMotionChange = viewModel::setMotion
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
    onThemeModeChange: (ThemeMode) -> Unit,
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
    onMotionChange: (MotionChoice) -> Unit
) {
    val colors = AppTheme.colors
    var showAbout by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }
    var showPinRemove by remember { mutableStateOf(false) }
    var showDisplay by remember { mutableStateOf(false) }
    var showAnimation by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.gutter)
                // 底栏是浮在内容上的：末尾再多留出它压住的高度
                .padding(bottom = Spacing.xxl + LocalBottomBarClearance.current)
        ) {
            ProfileHeader(
                nickname = uiState.nickname,
                signature = uiState.signature,
                avatarPath = uiState.avatarPath,
                onClick = onOpenProfile
            )

            SettingsSection(title = stringResource(R.string.settings_sec_system)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_achievements),
                    subtitle = stringResource(R.string.settings_achievements_desc),
                    showChevron = true,
                    onClick = onOpenAchievementSettings
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_look)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_desc),
                    trailingText = stringResource(uiState.themeMode.labelRes()),
                    showChevron = true,
                    onClick = { showTheme = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_desc),
                    trailingText = stringResource(uiState.language.labelRes),
                    showChevron = true,
                    onClick = { showLanguage = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_display),
                    subtitle = stringResource(R.string.settings_display_desc),
                    trailingText = stringResource(uiState.fontScale.labelRes()),
                    showChevron = true,
                    onClick = { showDisplay = true }
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_animation),
                    subtitle = stringResource(R.string.settings_animation_desc),
                    trailingText = stringResource(uiState.motion.labelRes()),
                    showChevron = true,
                    onClick = { showAnimation = true }
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_data)) {
                AppSettingRow(
                    title = stringResource(R.string.backup_title),
                    subtitle = stringResource(
                        R.string.settings_backup_desc,
                        uiState.stats.achievementCount,
                        uiState.stats.noteCount,
                        uiState.stats.mediaCount
                    ),
                    showChevron = true,
                    onClick = onOpenBackup
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_desc),
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_notify)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_reminder),
                    subtitle = stringResource(R.string.settings_reminder_desc),
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

            SettingsSection(title = stringResource(R.string.settings_sec_privacy)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_app_pin),
                    subtitle = stringResource(R.string.settings_app_pin_desc),
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
                    showChevron = true,
                    onClick = onOpenDataSecurity
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_about)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_desc),
                    showChevron = true,
                    onClick = { showAbout = true }
                )
            }
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
            backgroundImageUri = uiState.backgroundImageUri,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
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
    backgroundImageUri: String?,
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

                BackgroundPreview(uri = backgroundImageUri, opacity = backgroundOpacity)

                Spacer(modifier = Modifier.height(Spacing.md))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppButton(
                        text = stringResource(
                            if (backgroundImageUri == null) {
                                R.string.settings_background_pick
                            } else {
                                R.string.settings_background_change
                            }
                        ),
                        onClick = onPickBackground,
                        variant = AppButtonVariant.Secondary
                    )
                    if (backgroundImageUri != null) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        AppButton(
                            text = stringResource(R.string.settings_background_remove),
                            onClick = onRemoveBackground,
                            variant = AppButtonVariant.Text
                        )
                    }
                }

                if (backgroundImageUri != null) {
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
private fun BackgroundPreview(uri: String?, opacity: Float) {
    val colors = AppTheme.colors
    val thumbnail = uri?.let { rememberUriThumbnail(Uri.parse(it), sizePx = 360) }

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

/** 一个分组：小标题 + 一张容器卡片 */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Text(
        text = title,
        style = AppTheme.type.caption,
        color = AppTheme.colors.textTertiary,
        modifier = Modifier.padding(
            start = Spacing.xs,
            top = Spacing.xl,
            bottom = Spacing.sm
        )
    )

    AppCard(contentPadding = PaddingValues(0.dp), content = content)
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
 * 设置页顶部的资料卡：头像 + 昵称 + 签名，点一下进个人资料页。
 *
 * 没设置过昵称时退回「个人资料」这个标题和说明，
 * 让第一眼仍然是"这里可以设置资料"，而不是一片空白。
 */
@Composable
private fun ProfileHeader(
    nickname: String,
    signature: String,
    avatarPath: String?,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xl),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(
                name = nickname,
                path = avatarPath,
                size = Sizes.avatarLg
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nickname.ifBlank { stringResource(R.string.settings_profile) },
                    style = AppTheme.type.h3,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                tint = colors.textTertiary
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
                stats = BackupSummary(12, 7, 31),
                appVersion = "1.0"
            ),
            onBack = {},
            onOpenProfile = {},
            onOpenBackup = {},
            onOpenAchievementSettings = {},
            onOpenReminder = {},
            onOpenDataSecurity = {},
            onThemeModeChange = {},
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
