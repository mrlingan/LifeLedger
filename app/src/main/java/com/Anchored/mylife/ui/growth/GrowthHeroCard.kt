package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.LEVEL_STEP
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.LevelBadge
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.xpText

/**
 * 成长页的 Hero：走到哪一阶段 + 账上有多少积分。
 *
 * 左边那颗圆是阶段徽章（Lv.N），右边的数字是账上的积分——两件事分开讲：
 * 阶段看的是完成了多少条经历，积分是能花的那本账。
 * 下面的进度条走的是当前阶段，和首页、我的页同一个数。
 */
@Composable
internal fun GrowthHeroCard(
    balance: Int,
    level: Int,
    /** 当前阶段已经走完几条 */
    stageProgress: Int,
    toNextLevel: Int,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val progress = stageProgress.toFloat() / LEVEL_STEP

    AppCard(modifier = modifier, tone = AppCardTone.Glass) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LevelBadge(level = level)

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.store_balance_label),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(Spacing.xs))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = xpText(balance),
                        style = AppTheme.type.numberLarge,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.store_xp_unit),
                        style = AppTheme.type.bodyLarge,
                        color = colors.accent
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = stringResource(
                        R.string.growth_stage_progress,
                        stageProgress,
                        LEVEL_STEP
                    ),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppProgressBar(progress = progress, color = colors.accent)

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.home_level_next, toNextLevel),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
    }
}
