package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 核心数据：一行四个数字，标签在上、数字在下。
 *
 * 不用四张卡片，也不用分隔线——四列的间距和字号本身就形成了节奏，
 * 数字明显大于标签，扫一眼就能读到量级。
 */
@Composable
internal fun OverviewMetrics(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.lg)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCell(
                value = uiState.completedCount.toString(),
                label = stringResource(R.string.home_metric_completed),
                supporting = stringResource(R.string.home_metric_of_total, uiState.totalCount)
            )
            MetricCell(
                value = uiState.inProgressCount.toString(),
                label = stringResource(R.string.home_metric_in_progress)
            )
            MetricCell(
                value = uiState.totalCount.toString(),
                label = stringResource(R.string.home_metric_total)
            )
            MetricCell(
                value = uiState.recordedDays.toString(),
                label = stringResource(R.string.home_metric_days),
                supporting = if (uiState.streakDays > 0) {
                    stringResource(R.string.home_metric_streak, uiState.streakDays)
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun RowScope.MetricCell(
    value: String,
    label: String,
    supporting: String? = null
) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = AppTheme.type.caption,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = AppTheme.type.numberMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = supporting,
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}
