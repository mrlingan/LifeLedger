package com.Anchored.mylife.ui

import com.Anchored.mylife.R

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

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

    SettingsScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onOpenBackup = { navController.navigate("backup") },
        onThemeModeChange = viewModel::setThemeMode,
        onAppLockChange = viewModel::setAppLockEnabled
    )
}

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAppLockChange: (Boolean) -> Unit
) {
    val colors = AppTheme.colors
    var showAbout by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.background,
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
                .padding(bottom = Spacing.xxl)
        ) {
            SettingsSection(title = stringResource(R.string.settings_sec_system)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_profile),
                    subtitle = stringResource(R.string.settings_profile_desc),
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_achievements),
                    subtitle = stringResource(R.string.settings_achievements_desc),
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_look)) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = AppTheme.type.bodyLarge,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = stringResource(R.string.settings_theme_desc),
                        style = AppTheme.type.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    AppSegmentedControl(
                        options = listOf(stringResource(R.string.settings_theme_system), stringResource(R.string.settings_theme_light), stringResource(R.string.settings_theme_dark)),
                        selectedIndex = ThemeMode.entries.indexOf(uiState.themeMode),
                        onSelect = { index -> onThemeModeChange(ThemeMode.entries[index]) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_display),
                    subtitle = stringResource(R.string.settings_display_desc),
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
                AppDivider()
                AppSettingRow(
                    title = stringResource(R.string.settings_animation),
                    subtitle = stringResource(R.string.settings_animation_desc),
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
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
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
                )
            }

            SettingsSection(title = stringResource(R.string.settings_sec_privacy)) {
                AppSettingRow(
                    title = stringResource(R.string.settings_app_lock),
                    subtitle = if (uiState.biometricAvailable) {
                        stringResource(R.string.settings_app_lock_desc)
                    } else {
                        stringResource(R.string.settings_app_lock_na)
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
                    trailingText = stringResource(SOON_LABEL),
                    enabled = false
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
            onOpenBackup = {},
            onThemeModeChange = {},
            onAppLockChange = {}
        )
    }
}
