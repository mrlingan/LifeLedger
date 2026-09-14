package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.data.reward.RewardCatalog
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 积分商城的入口。
 *
 * 积分是在这一页攒出来的（完成成就、做完任务、事件奖励），花出去的地方是商城——
 * 两个页面之间给一个明确的口子，比让人回底栏绕一圈再找强。
 * 它放在整页最后：先把"攒了多少"讲完，再说"可以去花了"。
 */
@Composable
internal fun GrowthStoreCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(modifier = modifier, tone = AppCardTone.Glass, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = RewardCatalog.FALLBACK_ICON, style = AppTheme.type.h1)

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.growth_store_title),
                    style = AppTheme.type.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(R.string.growth_store_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }
    }
}
