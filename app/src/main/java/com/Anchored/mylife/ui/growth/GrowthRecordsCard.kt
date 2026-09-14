package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.PointType
import com.Anchored.mylife.ui.GrowthRecord
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.xpDelta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 收起时先露四条，剩下的折进「查看全部」 */
private const val COLLAPSED_RECORDS = 4

/**
 * 成长记录：账本上最近几笔。
 *
 * 这一栏就是流水账的正脸——攒到的每一笔都在这里，不只是成就：
 * 每日任务的奖励、事件的分、商城兑换花掉的、撤销退回来的。
 * 花钱的那几笔金额是负的，颜色也跟着降一档，一眼能分出"攒"和"花"。
 *
 * [presetTitleOf] 用来把成就那条的标题换成当前语言：数据库里存的是记账当时的原文，
 * 从图鉴来的条目自带 presetId，能查到现在语言的写法就用它。
 */
@Composable
internal fun GrowthRecordsCard(
    records: List<GrowthRecord>,
    presetTitleOf: (GrowthRecord) -> String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    // 流水按时间正序存着，这一栏最新的在最上面
    val newestFirst = remember(records) { records.asReversed() }
    val visible = if (expanded) newestFirst else newestFirst.take(COLLAPSED_RECORDS)
    val dateFormat = remember {
        // 固定成 yyyy.MM.dd：这一列是窄的右对齐，换语言时数字格式不会忽长忽短
        SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
    }

    AppCard(modifier = modifier, tone = AppCardTone.Glass) {
        SectionHeader(
            title = stringResource(R.string.growth_records_title),
            subtitle = stringResource(R.string.growth_records_hint),
            action = if (newestFirst.size > COLLAPSED_RECORDS) {
                {
                    AppTextLink(
                        text = stringResource(
                            if (expanded) R.string.growth_show_less else R.string.home_view_all
                        ),
                        onClick = { onExpandedChange(!expanded) },
                        showChevron = !expanded
                    )
                }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (visible.isEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.growth_records_empty_title),
                    style = AppTheme.type.body,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.growth_records_empty_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
            return@AppCard
        }

        Column {
            visible.forEachIndexed { index, record ->
                if (index > 0) AppDivider()
                GrowthRecordRow(
                    record = record,
                    title = presetTitleOf(record) ?: record.title,
                    date = dateFormat.format(Date(record.createdAt))
                )
            }
        }
    }
}

/**
 * 一条记录：左边这笔记的是哪一类，中间是什么事，右边攒了多少、什么时候。
 *
 * 金额是这一行唯一带颜色的字：正数用强调色（攒到了），负数用次要色
 * （花掉的、退回去的）——不用红绿去判断好坏，"花掉积分"本来也不是错误。
 */
@Composable
private fun GrowthRecordRow(
    record: GrowthRecord,
    title: String,
    date: String
) {
    val colors = AppTheme.colors
    val source = record.source()
    val positive = record.amount >= 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.growthRecordIcon)
                .clip(CircleShape)
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = source.icon,
                contentDescription = null,
                tint = if (positive) colors.accent else colors.textTertiary,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(source.labelRes),
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${xpDelta(record.amount)} ${stringResource(R.string.store_xp_unit)}",
                style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (positive) colors.accent else colors.textSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = date,
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                maxLines = 1
            )
        }
    }
}

/**
 * 这一笔属于哪一类经历。
 *
 * 事件的奖励和罚款是同一个类型，靠金额正负分开：负的那笔是"错过了"。
 * 认不出来的类型不会掉出去，落到 [Other] 显示成"积分变动"——
 * 以后加了新的积分来源，老版本也不会把这一行吞掉。
 */
private enum class GrowthRecordSource(val icon: ImageVector, val labelRes: Int) {
    Achievement(Icons.Outlined.Star, R.string.growth_source_achievement),
    AchievementUndone(Icons.Outlined.Star, R.string.growth_source_achievement_undone),
    DailyTask(Icons.Outlined.CheckCircle, R.string.growth_source_task),
    Event(Icons.Outlined.Notifications, R.string.growth_source_event),
    EventMissed(Icons.Outlined.Notifications, R.string.growth_source_event_missed),
    Redeem(Icons.Outlined.ShoppingCart, R.string.growth_source_redeem),
    Refund(Icons.Outlined.ShoppingCart, R.string.growth_source_refund),
    Other(Icons.Outlined.Star, R.string.growth_source_other)
}

private fun GrowthRecord.source(): GrowthRecordSource = when (type) {
    // 负数的那笔是"取消完成"退回去的分：开发版本里这一类还没单独标类型，
    // 靠金额的正负认出来，免得它在记录里被当成一条完成
    PointType.ACHIEVEMENT ->
        if (amount < 0) GrowthRecordSource.AchievementUndone else GrowthRecordSource.Achievement

    PointType.ACHIEVEMENT_REVERSAL -> GrowthRecordSource.AchievementUndone
    PointType.DAILY_TASK -> GrowthRecordSource.DailyTask
    PointType.RANDOM_EVENT ->
        if (amount < 0) GrowthRecordSource.EventMissed else GrowthRecordSource.Event

    PointType.REWARD_REDEEM -> GrowthRecordSource.Redeem
    PointType.REWARD_REFUND -> GrowthRecordSource.Refund
    else -> GrowthRecordSource.Other
}
