package com.Anchored.mylife.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.RarityBadge
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.firstGlyph
import com.Anchored.mylife.ui.rememberMediaThumbnail
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 最近解锁：一张卡里三行，每行 封面 + 标题 + 完成日期。
 *
 * 原来是三张横滑卡片（封面 + 标题 + 描述 + 稀有度）。改成竖排的行之后，
 * 三条记录一眼扫得完，不用左右滑；首页本来就是"往下看"的页面，
 * 只有一横行内容需要横滑的话，那条内容基本等于没人看。
 *
 * 卡片带封面——用户当时给这条成就配过图就显示那张图，没有就用标题首字占位。
 * 描述不在这里出现：一行只讲一件事，点进详情再看。
 */
@Composable
internal fun RecentAchievementsSection(
    items: List<RecentAchievement>,
    onClick: (Long) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.home_recent),
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
                color = AppTheme.colors.textTertiary
            )
        } else {
            items.forEachIndexed { index, item ->
                if (index > 0) AppDivider()

                RecentRow(
                    item = item,
                    onClick = { onClick(item.achievement.id) }
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    item: RecentAchievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val achievement = item.achievement
    val shape = RoundedCornerShape(Radius.md)

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 圆角在外、点击在水波纹以内：按下去的高亮不会溢出到卡片边角
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = Sizes.homeThumbWidth,
                    height = Sizes.homeThumbHeight
                )
                .clip(shape)
                .background(colors.surfaceSunken),
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

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = achievement.title,
                    style = AppTheme.type.bodyLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // 稀有度跟在标题后面（自己写的那条没有档位，就什么都不挂）
                if (item.tier != null) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    RarityBadge(tier = item.tier)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = stringResource(
                    R.string.home_meta_completed,
                    rememberCompletedDate(achievement.completedDate ?: achievement.createdDate)
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 完成日期按系统语言格式化：中文是「2026年9月14日」，英文是「Sep 14, 2026」 */
@Composable
private fun rememberCompletedDate(millis: Long): String {
    val locale = Locale.getDefault()
    return remember(millis, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "yMMMd")
        SimpleDateFormat(pattern, locale).format(Date(millis))
    }
}
