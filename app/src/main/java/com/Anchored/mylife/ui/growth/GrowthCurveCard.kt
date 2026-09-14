package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.GrowthRecord
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.xpDelta
import java.util.Calendar
import kotlin.math.max

/** 曲线的四档时间窗。天数只用来算起点，[ALL] 表示不过滤 */
private enum class GrowthRange(val days: Int, val labelRes: Int) {
    Week(7, R.string.growth_range_week),
    Month(30, R.string.growth_range_month),
    Quarter(90, R.string.growth_range_quarter),
    All(Int.MAX_VALUE, R.string.growth_range_all)
}

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * 人生曲线：积分按天累加的那条线，以及今天 / 本周 / 本月各攒了多少。
 *
 * 曲线画的是**累计余额**（每天的余额连起来），不是每天的增量——
 * 增量在下面那行字里已经写了，一条起伏很大的柱状图反而看不出"我在往上走"。
 * 时间窗只有四种：7 天 / 30 天 / 90 天 / 全部，用分段控件切换。
 */
@Composable
internal fun GrowthCurveCard(
    transactions: List<GrowthRecord>,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var range by rememberSaveable { mutableStateOf(GrowthRange.Week) }

    val now = System.currentTimeMillis()
    val from = if (range == GrowthRange.All) Long.MIN_VALUE else now - range.days * MILLIS_PER_DAY

    val todayDelta = transactions.filter { sameDay(it.createdAt, now) }.sumOf { it.amount }
    val weekDelta = transactions.filter { it.createdAt >= now - 7 * MILLIS_PER_DAY }.sumOf { it.amount }
    val monthDelta = transactions.filter { it.createdAt >= now - 30 * MILLIS_PER_DAY }.sumOf { it.amount }
    val points = remember(transactions, from) { cumulativeBalance(transactions, from) }

    AppCard(modifier = modifier, tone = AppCardTone.Glass) {
        Text(
            text = stringResource(R.string.growth_curve_title),
            style = AppTheme.type.h3,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = stringResource(
                R.string.growth_curve_delta,
                xpDelta(todayDelta),
                xpDelta(weekDelta),
                xpDelta(monthDelta)
            ),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (points.size < 2) {
            // 一个点连不成线：与其画一段看起来像坏了的东西，不如说清楚为什么还没有
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.growthChart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.growth_curve_empty),
                    style = AppTheme.type.bodySmall,
                    color = colors.textTertiary
                )
            }
        } else {
            GrowthChart(points = points)
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        AppSegmentedControl(
            options = GrowthRange.entries.map { stringResource(it.labelRes) },
            selectedIndex = GrowthRange.entries.indexOf(range),
            onSelect = { index -> range = GrowthRange.entries[index] },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 曲线本身：一条强调色的折线，线下面压一层很淡的同色填充。
 *
 * 纵轴从这串数据的最小值画到最大值（而不是从 0 起）：积分余额是一个缓慢上涨的量，
 * 从 0 起画的话整条线会贴在顶上，看不出最近的走势。数据只有一个点时不画线
 * （上面已经处理），所以这里不会除以 0。
 */
@Composable
private fun GrowthChart(
    points: List<Int>,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val accent = colors.accent

    Canvas(modifier = modifier.fillMaxWidth().height(Sizes.growthChart)) {
        val low = points.minOrNull() ?: 0
        val high = max(low + 1, points.maxOrNull() ?: 1)
        val stepX = size.width / (points.size - 1)

        fun xOf(index: Int) = index * stepX
        fun yOf(value: Int) = size.height - (value - low).toFloat() / (high - low) * size.height

        val line = Path()
        points.forEachIndexed { index, value ->
            if (index == 0) line.moveTo(xOf(index), yOf(value))
            else line.lineTo(xOf(index), yOf(value))
        }

        // 填充：把折线首尾连到画布底边围成一个闭合区域
        val area = Path().apply {
            addPath(line)
            lineTo(xOf(points.lastIndex), size.height)
            lineTo(xOf(0), size.height)
            close()
        }

        drawPath(path = area, color = accent.copy(alpha = 0.10f))
        drawPath(
            path = line,
            color = accent,
            style = Stroke(Sizes.growthChartStroke.toPx(), cap = StrokeCap.Round)
        )

        // 最新那个数点一下：一眼能看出线走到哪儿了
        drawCircle(
            color = accent,
            radius = Sizes.growthChartDot.toPx(),
            center = Offset(xOf(points.lastIndex), yOf(points.last()))
        )
    }
}

/**
 * 按天累计的余额序列：只保留 [from] 之后的天。
 *
 * 每条流水都算进来（包括花掉的负数），所以这条线的终点就是当前余额。
 */
private fun cumulativeBalance(transactions: List<GrowthRecord>, from: Long): List<Int> {
    var running = 0
    return transactions
        .groupBy { startOfDay(it.createdAt) }
        .toSortedMap()
        .mapNotNull { (day, items) ->
            running += items.sumOf { it.amount }
            running.takeIf { day >= from }
        }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun sameDay(a: Long, b: Long) = startOfDay(a) == startOfDay(b)
