package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Anchored.mylife.R
import com.Anchored.mylife.data.settings.StartChoice
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppIndeterminateBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 首次启动的起点选择。
 *
 * 一屏只问一件事：成就列表是想装着 109 条预设开始，还是从空白开始。
 * 两张卡就是全部内容——没有插画、没有引导轮播，选完就进主页。
 */
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(uiState = uiState, onChoose = viewModel::choose)
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onChoose: (StartChoice) -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(containerColor = colors.background) { innerPadding ->
        // 还在判断要不要问（老用户）：什么都不画，避免闪一下
        if (uiState.stage == OnboardingStage.CHECKING) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            return@Scaffold
        }

        val choosing = uiState.stage == OnboardingStage.CHOOSING

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.gutter)
                .padding(bottom = Spacing.xxl)
        ) {
            Spacer(modifier = Modifier.height(Spacing.huge))

            Text(
                text = stringResource(R.string.app_name),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = stringResource(R.string.onboarding_title),
                style = AppTheme.type.h1,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            ChoiceCard(
                icon = Icons.AutoMirrored.Outlined.List,
                title = stringResource(R.string.onboarding_presets_title),
                description = stringResource(R.string.onboarding_presets_desc),
                enabled = choosing,
                onClick = { onChoose(StartChoice.PRESETS) }
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            ChoiceCard(
                icon = Icons.Outlined.Create,
                title = stringResource(R.string.onboarding_blank_title),
                description = stringResource(R.string.onboarding_blank_desc),
                enabled = choosing,
                onClick = { onChoose(StartChoice.BLANK) }
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            if (uiState.stage == OnboardingStage.APPLYING) {
                Text(
                    text = stringResource(R.string.onboarding_applying),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                AppIndeterminateBar()
                Spacer(modifier = Modifier.height(Spacing.xl))
            }

            Text(
                text = stringResource(R.string.onboarding_note),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) colors.accent else colors.textTertiary,
                modifier = Modifier.size(Sizes.iconLg)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.type.bodyLarge,
                    color = if (enabled) colors.textPrimary else colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = description,
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900, name = "首次启动")
@Composable
private fun OnboardingPreview() {
    LifeLedgerTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(stage = OnboardingStage.CHOOSING),
            onChoose = {}
        )
    }
}
