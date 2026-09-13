package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 数据安全：把"数据存在哪、有没有加密、加密挡得住什么"摊开讲清楚。
 */
@Composable
fun DataSecurityRoute(
    navController: NavHostController,
    viewModel: DataSecurityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DataSecurityScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onOpenBackup = { navController.navigate("backup") },
        onEncryptionChange = viewModel::setEncryptionEnabled,
        onRetryMigration = viewModel::retryMigration
    )
}

@Composable
fun DataSecurityScreen(
    uiState: DataSecurityUiState,
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onEncryptionChange: (Boolean) -> Unit,
    onRetryMigration: () -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.data_security_title),
                onBack = onBack
            )
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
            Spacer(modifier = Modifier.height(Spacing.lg))

            AppCard {
                SectionHeader(title = stringResource(R.string.data_security_location))
                Spacer(modifier = Modifier.height(Spacing.md))

                StorageLine(
                    label = stringResource(R.string.data_security_database_label),
                    path = uiState.databasePath,
                    size = uiState.databaseSize
                )
                AppDivider(modifier = Modifier.padding(vertical = Spacing.md))
                StorageLine(
                    label = stringResource(R.string.backup_stat_media),
                    path = uiState.mediaPath,
                    size = uiState.mediaSize
                )
                AppDivider(modifier = Modifier.padding(vertical = Spacing.md))
                StorageLine(
                    label = stringResource(R.string.profile_title),
                    path = uiState.profilePath,
                    size = uiState.profileSize
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    text = stringResource(R.string.data_security_location_value),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            EncryptionCard(
                state = uiState,
                onEncryptionChange = onEncryptionChange,
                onRetryMigration = onRetryMigration
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.data_security_backup_note),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            AppButton(
                text = stringResource(R.string.data_security_open_backup),
                onClick = onOpenBackup,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 本地加密卡片。
 *
 * 开关是唯一能改变状态的东西；下面那行小字永远在回答"现在到底加没加密"，
 * 迁移中、迁移失败、关掉开关但老数据是密文，这三种情况都各有各的说法。
 */
@Composable
private fun EncryptionCard(
    state: DataSecurityUiState,
    onEncryptionChange: (Boolean) -> Unit,
    onRetryMigration: () -> Unit
) {
    val colors = AppTheme.colors
    val migrating = state.migration is EncryptionMigrationUi.Running

    AppCard {
        SectionHeader(
            title = stringResource(R.string.data_security_encryption),
            subtitle = stringResource(
                if (state.encryptionEnabled) {
                    R.string.data_security_encryption_state_on
                } else {
                    R.string.data_security_encryption_state_off
                }
            )
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.data_security_encryption_switch),
                    style = AppTheme.type.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(R.string.data_security_encryption_switch_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
            AppSwitch(
                checked = state.encryptionEnabled,
                onCheckedChange = onEncryptionChange,
                enabled = !migrating
            )
        }

        MigrationStatus(
            state = state,
            onRetryMigration = onRetryMigration
        )

        Spacer(modifier = Modifier.height(Spacing.lg))
        AppDivider()
        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.data_security_encryption_desc),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun MigrationStatus(
    state: DataSecurityUiState,
    onRetryMigration: () -> Unit
) {
    val colors = AppTheme.colors

    when (val migration = state.migration) {
        EncryptionMigrationUi.Idle -> {
            // 关掉开关时，已经加密的老数据仍然读得出来，这一点要主动讲，
            // 否则用户会以为"关掉 = 那些数据废了"。
            if (!state.encryptionEnabled && state.encryptedRowCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = stringResource(
                        R.string.data_security_encryption_kept,
                        state.encryptedRowCount
                    ),
                    style = AppTheme.type.bodySmall,
                    color = colors.textTertiary
                )
            }
        }

        is EncryptionMigrationUi.Running -> {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(
                    R.string.data_security_encryption_progress,
                    migration.done,
                    migration.total
                ),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            AppProgressBar(
                progress = if (migration.total == 0) {
                    0f
                } else {
                    migration.done.toFloat() / migration.total
                }
            )
        }

        is EncryptionMigrationUi.Done -> {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = if (migration.newlyEncrypted > 0) {
                    stringResource(
                        R.string.data_security_encryption_done,
                        migration.newlyEncrypted
                    )
                } else {
                    // 库里本来就没有明文，说"已加密 0 条"只会让人困惑
                    stringResource(R.string.data_security_encryption_done_none)
                },
                style = AppTheme.type.bodySmall,
                color = colors.success
            )
        }

        is EncryptionMigrationUi.Failed -> {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.data_security_encryption_pending, migration.pending),
                style = AppTheme.type.bodySmall,
                color = colors.warning
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            AppButton(
                text = stringResource(R.string.data_security_encryption_retry),
                onClick = onRetryMigration,
                variant = AppButtonVariant.Secondary
            )
        }
    }
}

@Composable
private fun StorageLine(label: String, path: String, size: Long) {
    val colors = AppTheme.colors

    Text(
        text = label,
        style = AppTheme.type.bodyLarge,
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(Spacing.xxs))
    Text(
        text = formatFileSize(size),
        style = AppTheme.type.caption,
        color = colors.textSecondary
    )
    Spacer(modifier = Modifier.height(Spacing.xxs))
    Text(
        text = path,
        style = AppTheme.type.caption,
        color = colors.textTertiary
    )
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
