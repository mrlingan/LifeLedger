package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 核心数据：四张紧凑的信息卡。
 *
 * 相比一整条统计栏，独立卡片在深色界面上能更清楚地建立层级，
 * 同时仍保持一屏可扫读的密度。
 */
@Composable
internal fun OverviewMetrics(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter)
    ) {
        MetricCard(
            value = uiState.completedCount.toString(),
            iconRes = R.drawable.ic_metric_completed,
            label = stringResource(R.string.home_metric_completed),
            supporting = stringResource(R.string.home_metric_of_total, uiState.totalCount)
        )
        Spacer(modifier = Modifier.padding(horizontal = Spacing.xs))
        MetricCard(
            value = uiState.inProgressCount.toString(),
            iconRes = R.drawable.ic_metric_progress,
            label = stringResource(R.string.home_metric_in_progress)
        )
        Spacer(modifier = Modifier.padding(horizontal = Spacing.xs))
        MetricCard(
            value = uiState.totalCount.toString(),
            iconRes = R.drawable.ic_metric_records,
            label = stringResource(R.string.home_metric_total)
        )
        Spacer(modifier = Modifier.padding(horizontal = Spacing.xs))
        MetricCard(
            value = uiState.recordedDays.toString(),
            iconRes = R.drawable.ic_metric_streak,
            label = stringResource(R.string.home_metric_days),
            supporting = if (uiState.streakDays > 0) {
                stringResource(R.string.home_metric_streak, uiState.streakDays)
            } else null
        )
    }
}

@Composable
private fun RowScope.MetricCard(
    value: String,
    iconRes: Int,
    label: String,
    supporting: String? = null
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = Modifier.weight(1f),
        tone = AppCardTone.Elevated,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Spacing.xs,
            vertical = Spacing.md
        )
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier
                .size(Sizes.iconMd)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = AppTheme.type.caption,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = AppTheme.type.h2,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = supporting,
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
