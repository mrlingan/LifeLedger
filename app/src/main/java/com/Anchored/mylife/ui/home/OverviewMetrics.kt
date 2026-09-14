package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
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
 *
 * 四张卡必须**一样大**，所以每张卡都按同一套固定行来排：图标 / 标签槽 / 数字。
 * 标签槽取四个标签里最高的那个（英文标签在窄卡片里会折行，中文不会），
 * 不统一槽高的话，折行的那张会把整张卡顶高。
 *
 * 这里只放四张卡和数字本身：补充说明（以前「已完成」下面的「/ 110」、
 * 「坚持天数」下面的「连续 N 天」）已经去掉，卡片因此矮了一整行。
 */
@Composable
internal fun OverviewMetrics(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val labels = listOf(
        stringResource(R.string.home_metric_completed),
        stringResource(R.string.home_metric_in_progress),
        stringResource(R.string.home_metric_total),
        stringResource(R.string.home_metric_days)
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter)
    ) {
        // 卡片宽度 = 整行减去三个间距再四等分，和 Row 里 weight 分出来的一致
        val labelWidth = (maxWidth - Spacing.sm * 3) / 4 - Spacing.xs * 2
        val labelSlot = rememberMetricLabelSlot(labels = labels, width = labelWidth)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            MetricCard(
                value = uiState.completedCount.toString(),
                iconRes = R.drawable.ic_metric_completed,
                label = labels[0],
                labelSlot = labelSlot,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = uiState.inProgressCount.toString(),
                iconRes = R.drawable.ic_metric_progress,
                label = labels[1],
                labelSlot = labelSlot,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = uiState.totalCount.toString(),
                iconRes = R.drawable.ic_metric_records,
                label = labels[2],
                labelSlot = labelSlot,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = uiState.recordedDays.toString(),
                iconRes = R.drawable.ic_metric_streak,
                label = labels[3],
                labelSlot = labelSlot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 四张卡共用的标签槽高度。
 *
 * 用同一套字体、同一档字号把四个标签都量一遍，取最高的那个：
 * 中文标签永远一行，英文标签在窄卡片里（大号字体下尤其）会折成两行，
 * 谁折行谁就把卡片顶高——统一槽位之后，四张卡才对得齐。
 */
@Composable
private fun rememberMetricLabelSlot(labels: List<String>, width: Dp): Dp {
    val measurer = rememberTextMeasurer()
    val style = AppTheme.type.caption
    val density = LocalDensity.current
    val widthPx = with(density) { width.roundToPx() }

    return remember(measurer, style, labels, widthPx) {
        labels
            .maxOf { label ->
                measurer.measure(
                    text = label,
                    style = style,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    constraints = Constraints(maxWidth = widthPx)
                ).size.height
            }
            .let { heightPx -> with(density) { heightPx.toDp() } }
    }
}

@Composable
private fun MetricCard(
    value: String,
    iconRes: Int,
    label: String,
    labelSlot: Dp,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier,
        // 四张数据卡也是玻璃：和底栏同一套折射，只是卡片很小，边缘那一圈更含蓄
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(
            horizontal = Spacing.xs,
            // 少了一行说明之后，上下留白也收一档，整块更矮更紧凑
            vertical = Spacing.sm
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
            modifier = Modifier
                .fillMaxWidth()
                // 用 min 而不是固定高度：万一量出来和实际排版差一点点，也不至于裁字
                .heightIn(min = labelSlot)
        )

        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = value,
            style = AppTheme.type.h2,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
