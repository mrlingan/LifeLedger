package com.Anchored.mylife.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.RecentAchievement
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.RarityBadge
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.appearAnimation
import com.Anchored.mylife.ui.components.firstGlyph
import com.Anchored.mylife.ui.rememberMediaThumbnail
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 最近解锁：三张卡片横排。
 *
 * 卡片带封面——用户当时给这条成就配过图就显示那张图，没有就用标题首字占位。
 * 阅读顺序是 封面 → 名称 → 描述 → 时间 → 稀有度，不做手游式徽章堆叠。
 */
@Composable
internal fun RecentAchievementsSection(
    items: List<RecentAchievement>,
    onClick: (Long) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.home_recent),
            modifier = Modifier.padding(start = Sizes.gutter, end = Spacing.xs),
            action = {
                AppTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAll
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.home_recent_empty),
                style = AppTheme.type.bodySmall,
                color = AppTheme.colors.textTertiary,
                modifier = Modifier.padding(horizontal = Sizes.gutter)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Sizes.gutter),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(items = items, key = { it.achievement.id }) { item ->
                    RecentAchievementCard(
                        item = item,
                        onClick = { onClick(item.achievement.id) },
                        modifier = Modifier
                            .width(Sizes.recentCard)
                            .appearAnimation()
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentAchievementCard(
    item: RecentAchievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val achievement = item.achievement
    val shape = RoundedCornerShape(Radius.md)

    AppCard(
        modifier = modifier.clickable(onClick = onClick),
        tone = AppCardTone.Elevated,
        contentPadding = PaddingValues(Spacing.sm)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f)
                .clip(shape)
                .background(colors.surfaceSunken)
                .border(Sizes.hairline, colors.border, shape),
            contentAlignment = Alignment.Center
        ) {
            val thumbnail = item.photoPath?.let { path ->
                rememberMediaThumbnail(path = path, isVideo = false, sizePx = 480)
            }

            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = achievement.title.firstGlyph(),
                    style = AppTheme.type.h3,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = achievement.title,
            style = AppTheme.type.bodyLarge,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (achievement.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = achievement.description,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 时间不在这里露面：卡片只负责"我完成了什么"，具体日期进详情看
        if (item.tier != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            RarityBadge(tier = item.tier)
        }
    }
}
