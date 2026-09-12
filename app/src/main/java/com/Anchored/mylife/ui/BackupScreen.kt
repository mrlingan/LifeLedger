package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppIndeterminateBar
import com.Anchored.mylife.ui.components.AppSnackbarHost
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.StatTile
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 备份与恢复。
 *
 * 页面只做三件事：看数据量、导出、导入。
 * 导入是不可逆的覆盖操作，所以确认弹窗用危险态。
 */
@Composable
fun BackupRoute(
    navController: NavHostController,
    viewModel: BackupViewModel = viewModel(
        factory = BackupViewModel.factory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var importPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var encryptedImportUri by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.export(uri, exportPassphrase)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importPassphrase = null
            // 加密备份先问密码，普通备份直接进确认
            if (viewModel.isEncrypted(uri)) {
                encryptedImportUri = uri
            } else {
                pendingImportUri = uri
                showRestoreConfirm = true
            }
        }
    }

    LaunchedEffect(uiState.message) {
        val message = uiState.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    BackupScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onExportClick = { showExportDialog = true },
        onImportClick = {
            importLauncher.launch(
                arrayOf("application/zip", "application/octet-stream", "*/*")
            )
        },
        snackbarHostState = snackbarHostState
    )

    if (showRestoreConfirm) {
        AppDialog(
            title = stringResource(R.string.backup_confirm_title),
            onDismissRequest = { showRestoreConfirm = false },
            onConfirm = {
                showRestoreConfirm = false
                pendingImportUri?.let { viewModel.import(it, importPassphrase) }
                pendingImportUri = null
            },
            confirmText = stringResource(R.string.backup_confirm_action),
            destructive = true,
            content = {
                AppDialogText(stringResource(R.string.backup_confirm_message))
            }
        )
    }

    if (showExportDialog) {
        PassphraseDialog(
            requireConfirm = true,
            title = stringResource(R.string.backup_export_title),
            hint = stringResource(R.string.backup_passphrase_optional),
            onDismiss = { showExportDialog = false },
            onConfirm = { passphrase ->
                exportPassphrase = passphrase
                showExportDialog = false
                exportLauncher.launch(viewModel.suggestedFileName())
            }
        )
    }

    encryptedImportUri?.let { uri ->
        PassphraseDialog(
            requireConfirm = false,
            title = stringResource(R.string.backup_passphrase),
            hint = stringResource(R.string.backup_passphrase_required),
            onDismiss = { encryptedImportUri = null },
            onConfirm = { passphrase ->
                encryptedImportUri = null
                importPassphrase = passphrase
                pendingImportUri = uri
                showRestoreConfirm = true
            }
        )
    }
}

/**
 * 备份密码输入框。
 *
 * 导出时要求输两遍：密码只存在用户脑子里，打错一次等于这份备份废掉，
 * 多问一遍比事后解释便宜。
 */
@Composable
private fun PassphraseDialog(
    requireConfirm: Boolean,
    title: String,
    hint: String,
    onDismiss: () -> Unit,
    onConfirm: (CharArray?) -> Unit
) {
    var passphrase by remember { mutableStateOf("") }
    var repeated by remember { mutableStateOf("") }
    val mismatch = requireConfirm && repeated.isNotEmpty() && passphrase != repeated

    AppDialog(
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = {
            if (!mismatch) {
                onConfirm(passphrase.takeIf { it.isNotEmpty() }?.toCharArray())
            }
        },
        confirmText = stringResource(R.string.common_ok),
        dismissText = stringResource(R.string.common_cancel),
        content = {
            Column {
                AppTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = stringResource(R.string.backup_passphrase),
                    isError = mismatch,
                    password = true
                )
                if (requireConfirm) {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    AppTextField(
                        value = repeated,
                        onValueChange = { repeated = it },
                        label = stringResource(R.string.backup_passphrase_again),
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            stringResource(R.string.backup_passphrase_mismatch)
                        } else {
                            null
                        },
                        password = true
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                AppDialogText(hint)
                if (requireConfirm) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(R.string.backup_passphrase_warning),
                        style = AppTheme.type.caption,
                        color = AppTheme.colors.textTertiary
                    )
                }
            }
        }
    )
}

@Composable
fun BackupScreen(
    uiState: BackupUiState,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(title = stringResource(R.string.backup_title), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Sizes.gutter)
        ) {
            AppCard {
                SectionHeader(title = stringResource(R.string.backup_current))
                Spacer(modifier = Modifier.height(Spacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatTile(
                        value = uiState.stats.achievementCount.toString(),
                        label = stringResource(R.string.backup_stat_achievements),
                        modifier = Modifier.weight(1f)
                    )
                    AppVerticalDivider(height = Spacing.xxl)
                    StatTile(
                        value = uiState.stats.noteCount.toString(),
                        label = stringResource(R.string.backup_stat_notes),
                        modifier = Modifier.weight(1f)
                    )
                    AppVerticalDivider(height = Spacing.xxl)
                    StatTile(
                        value = uiState.stats.mediaCount.toString(),
                        label = stringResource(R.string.backup_stat_media),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            AppCard {
                Text(
                    text = stringResource(R.string.backup_export),
                    style = AppTheme.type.h3,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.backup_export_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                AppButton(
                    text = stringResource(R.string.backup_export_action),
                    onClick = onExportClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            AppCard {
                Text(
                    text = stringResource(R.string.backup_import),
                    style = AppTheme.type.h3,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.backup_import_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                AppButton(
                    text = stringResource(R.string.backup_import_action),
                    onClick = onImportClick,
                    variant = AppButtonVariant.Secondary,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (uiState.isBusy) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                AppIndeterminateBar()
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.backup_busy),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Text(
                text = stringResource(R.string.backup_tips),
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = stringResource(R.string.backup_tips_body),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900, name = "备份与恢复")
@Composable
private fun BackupPreview() {
    LifeLedgerTheme {
        BackupScreen(
            uiState = BackupUiState(stats = BackupSummary(3, 7, 12)),
            onBack = {},
            onExportClick = {},
            onImportClick = {}
        )
    }
}
