package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.TierProgress
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.label
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 稀有度图鉴：青铜到传奇五个圆环，一行摆开。
 *
 * 分类那一块看的是「在哪些方向上有积累」，这一块看的是「攒下了多少稀有的东西」，
 * 两套切法都不进详表——想细看就点下面的条目。环里是已解锁的数量，
 * 环的颜色是这一档的金属色，未解锁的条目没有别的提示，就是环没走满。
 */
@Composable
internal fun CodexRarityCard(
    tiers: List<TierProgress>,
    modifier: Modifier = Modifier
) {
    if (tiers.isEmpty()) return

    AppCard(
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.codex_section_rarity),
            subtitle = stringResource(R.string.codex_rarity_hint)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            tiers.forEach { item ->
                CodexTierRing(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CodexTierRing(
    item: TierProgress,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val tierColor = colors.rarityColor(item.tier)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppProgressRing(
            progress = item.progress,
            diameter = Sizes.codexTierRing,
            strokeWidth = Sizes.progressRing,
            color = tierColor
        ) {
            Text(
                text = item.unlockedCount.toString(),
                style = AppTheme.type.caption,
                color = tierColor
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = item.tier.label(),
            style = AppTheme.type.caption,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = stringResource(
                R.string.codex_count_ratio,
                item.unlockedCount,
                item.totalCount
            ),
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
