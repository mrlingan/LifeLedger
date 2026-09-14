package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 核心数据：四个数字排在一张卡里，中间用细线分开。
 *
 * 以前是四张独立的小卡，每张一个图标 + 标签 + 数字。改成一张卡之后，
 * 这一段的阅读节奏从"看四张卡片"变成"扫一行数字"——四个数本来就是一起看的，
 * 分成四块只会让人多扫几眼；卡片也就此让给真正需要独立成块的内容
 * （个人卡片、分类进度、最近解锁）。
 *
 * 四格必须**一样高**，所以标签槽取四个标签里最高的那个（英文标签在窄格子里会
 * 折行，中文不会），不统一槽高的话，折行的那格会把整行顶高、数字也不再齐平。
 */
@Composable
internal fun OverviewMetrics(
    uiState: HomeUiState,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf(
        stringResource(R.string.home_metric_completed),
        stringResource(R.string.home_metric_in_progress),
        stringResource(R.string.home_metric_total),
        stringResource(R.string.home_metric_days)
    )
    val values = listOf(
        uiState.completedCount.toString(),
        uiState.inProgressCount.toString(),
        uiState.totalCount.toString(),
        uiState.recordedDays.toString()
    )

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.home_stats),
            action = {
                AppTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAll
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 一格的宽度 = 卡片内容宽（外层已经减掉 gutter 和卡片内边距）四等分，
            // 再留给每格左右各一点内边距。量标签用它。
            val cellWidth = (maxWidth - Spacing.xs * 4) / 4
            val labelSlot = rememberMetricLabelSlot(labels = labels, width = cellWidth)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                values.forEachIndexed { index, value ->
                    if (index > 0) {
                        AppVerticalDivider(height = Spacing.xxl)
                    }
                    MetricCell(
                        value = value,
                        label = labels[index],
                        labelSlot = labelSlot,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 四格共用的标签槽高度。
 *
 * 用同一套字体、同一档字号把四个标签都量一遍，取最高的那个：
 * 中文标签永远一行，英文标签在窄格子里（大号字体下尤其）会折成两行，
 * 谁折行谁就把那一格顶高——统一槽位之后，四个数字才对得齐。
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

/** 一格：数字在上、标签在下，整格居中 */
@Composable
private fun MetricCell(
    value: String,
    label: String,
    labelSlot: Dp,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier.padding(horizontal = Spacing.xxs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = AppTheme.type.numberMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = label,
            style = AppTheme.type.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                // 用 min 而不是固定高度：万一量出来和实际排版差一点点，也不至于裁字
                .heightIn(min = labelSlot)
        )
    }
}
