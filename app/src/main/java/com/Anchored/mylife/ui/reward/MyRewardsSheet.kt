package com.Anchored.mylife.ui.reward

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.RewardRedemption
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.xpText
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 「我的奖励」底部弹层：换过的都在这里，可以撤销。
 *
 * 撤销做在这个弹层里而不是商城卡片上，是因为"我换到了什么"本来就是一件要回头看的
 * 事——顺手也给撤销留了一个不在主流程上的位置，不会误点。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyRewardsSheet(
    redemptions: List<RewardRedemption>,
    spentTotal: Int,
    onDismiss: () -> Unit,
    onUndo: (RewardRedemption) -> Unit
) {
    val colors = AppTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = Radius.hero, topEnd = Radius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    bottom = Spacing.xxl
                )
        ) {
            Text(
                text = stringResource(R.string.store_my_rewards),
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(
                    R.string.store_rewards_summary,
                    redemptions.size,
                    xpText(spentTotal)
                ),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            if (redemptions.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.store_rewards_empty),
                    description = stringResource(R.string.store_rewards_empty_desc)
                )
            } else {
                redemptions.forEach { record ->
                    RedemptionRow(record = record, onUndo = { onUndo(record) })
                }
            }
        }
    }
}

/** 一条兑换记录：图标 + 名称 / 花掉的积分与日期 + 撤销 */
@Composable
private fun RedemptionRow(
    record: RewardRedemption,
    onUndo: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = record.icon, style = AppTheme.type.h2)

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                // 内置奖励按当前语言显示；自己写的那条还是记账时的原文
                text = rememberRewardTitle(record.title),
                style = AppTheme.type.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(
                    R.string.store_redeemed_line,
                    xpText(record.price),
                    redeemedDate(record.redeemedAt)
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }

        AppButton(
            text = stringResource(R.string.store_undo),
            onClick = onUndo,
            variant = AppButtonVariant.Text
        )
    }
}

/** 兑换日期。列表里只要"哪天"就够了，年份跟着一起给，跨年回看时不会误读 */
private fun redeemedDate(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(millis))
