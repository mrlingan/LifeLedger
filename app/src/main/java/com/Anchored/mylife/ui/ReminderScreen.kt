package com.Anchored.mylife.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.settings.ReminderFrequency
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 每日提醒设置。
 *
 * 打开时才申请通知权限（Android 13+），关掉通知的用户会看到一句说明，
 * 而不是一个悄悄不响的开关。
 */
@Composable
fun ReminderRoute(
    navController: NavHostController,
    viewModel: ReminderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        viewModel.setEnabled(granted)
    }

    ReminderScreen(
        uiState = uiState,
        permissionDenied = permissionDenied,
        onBack = { navController.popBackStack() },
        onEnabledChange = { enabled ->
            when {
                !enabled -> {
                    permissionDenied = false
                    viewModel.setEnabled(false)
                }

                needsNotificationPermission(context) ->
                    permissionLauncher.launchExternal(Manifest.permission.POST_NOTIFICATIONS)

                else -> {
                    permissionDenied = false
                    viewModel.setEnabled(true)
                }
            }
        },
        onTimeChange = viewModel::setTime,
        onFrequencyChange = viewModel::setFrequency
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED

@Composable
fun ReminderScreen(
    uiState: ReminderUiState,
    permissionDenied: Boolean,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onFrequencyChange: (ReminderFrequency) -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.reminder_title),
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

            AppCard(
                tone = AppCardTone.Glass,
                contentPadding = PaddingValues(horizontal = Spacing.lg)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reminder_enable),
                            style = AppTheme.type.bodyLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(top = Spacing.lg)
                        )
                        Text(
                            text = stringResource(R.string.reminder_enable_desc),
                            style = AppTheme.type.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = Spacing.lg)
                        )
                    }
                    AppSwitch(
                        checked = uiState.enabled,
                        onCheckedChange = onEnabledChange
                    )
                }
            }

            if (uiState.enabled) {
                Spacer(modifier = Modifier.height(Spacing.xl))

                AppCard(
                    tone = AppCardTone.Glass,
                    contentPadding = PaddingValues(horizontal = Spacing.lg)
                ) {
                    AppSettingRow(
                        title = stringResource(R.string.reminder_time),
                        subtitle = stringResource(R.string.reminder_enable_desc),
                        trailingText = stringResource(
                            R.string.settings_reminder_time_value,
                            uiState.hour,
                            uiState.minute
                        ),
                        showChevron = true,
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> onTimeChange(hour, minute) },
                                uiState.hour,
                                uiState.minute,
                                DateFormat.is24HourFormat(context)
                            ).show()
                        }
                    )

                    AppDivider()

                    Column(modifier = Modifier.padding(vertical = Spacing.lg)) {
                        Text(
                            text = stringResource(R.string.reminder_frequency),
                            style = AppTheme.type.bodyLarge,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        AppSegmentedControl(
                            options = ReminderFrequency.entries.map {
                                stringResource(
                                    when (it) {
                                        ReminderFrequency.EVERY_DAY -> R.string.reminder_every_day
                                        ReminderFrequency.WEEKDAYS -> R.string.reminder_weekdays
                                    }
                                )
                            },
                            selectedIndex = ReminderFrequency.entries.indexOf(uiState.frequency),
                            onSelect = { index ->
                                onFrequencyChange(ReminderFrequency.entries[index])
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (permissionDenied) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                Text(
                    text = stringResource(R.string.reminder_permission_denied),
                    style = AppTheme.type.bodySmall,
                    color = colors.warning
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.reminder_note),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }
    }
}
