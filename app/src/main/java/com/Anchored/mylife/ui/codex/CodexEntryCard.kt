package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppEmblem
import com.Anchored.mylife.ui.components.label
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 图鉴里的一条记录。
 *
 * 版式是横排：徽记在左，右边第一行是名称、第二行是介绍，最后一行是稀有度。
 * 不做卡片外框——几条这样并排的条目本身就是一份档案索引。
 *
 * 已解锁与未解锁的区别只体现在对比度上：
 * 已解锁用主文字色 + 稀有度色描边的徽记，未解锁整体降一档并显示达成率。
 */
@Composable
internal fun CodexEntryCard(
    title: String,
    description: String,
    tier: RarityTier,
    rate: Double,
    unlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        AppEmblem(
            text = title,
            tier = tier,
            unlocked = unlocked,
            size = Sizes.avatarLg
        )

        Spacer(modifier = Modifier.width(Spacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = if (unlocked) colors.textPrimary else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Spacing.xxs))

            Text(
                text = description,
                style = AppTheme.type.bodySmall,
                color = if (unlocked) colors.textSecondary else colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RarityLine(tier = tier, muted = !unlocked)

                // 已解锁的不再显示完成时间（点进详情看）；未解锁的保留达成率，
                // 那是"这是什么难度"的线索，不是时间
                if (!unlocked) {
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        text = stringResource(R.string.codex_rate_short, formatRate(rate)),
                        style = AppTheme.type.caption,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 稀有度：一个小圆点 + 标签。
 *
 * 网格单元很窄，用带外框的徽章会占掉半行；这里退化成"点 + 字"，
 * 颜色还是稀有度色，但克制到只够辨认。
 */
@Composable
private fun RarityLine(
    tier: RarityTier,
    muted: Boolean
) {
    val color = AppTheme.colors.rarityColor(tier).let {
        if (muted) it.copy(alpha = 0.55f) else it
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(Spacing.sm)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = tier.label(),
            style = AppTheme.type.caption,
            color = color,
            maxLines = 1
        )
    }
}

internal fun formatRate(rate: Double): String =
    if (rate % 1.0 == 0.0) "${rate.toInt()}%" else "$rate%"
