package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.theme.listRowPadding

/**
 * 列表里的呈现方式。
 *
 * 默认是 Plain（无卡片、靠留白和分隔线），因为「每个成就一张卡」
 * 会让页面变成一堆方块，反而没有层级。
 */
enum class AchievementCardVariant {
    Plain,
    Surface
}

/**
 * 成就卡片。
 *
 * 状态处理：
 * - 已完成：完整对比度，图标正常显示，带完成时间
 * - 进行中：正常显示，带进度
 * - 未解锁：整体降低视觉重量（不透明度 + 次要文字色），但保留轮廓，不做成一片灰
 *
 * @param icon 图标槽。当前数据里存的是 emoji，图标体系落地后直接换成矢量图，组件不用改。
 */
@Composable
fun AchievementCard(
    title: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    meta: String? = null,
    rarity: RarityTier? = null,
    status: AchievementStatus = AchievementStatus.InProgress,
    progress: Float? = null,
    variant: AchievementCardVariant = AchievementCardVariant.Plain,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val locked = status == AchievementStatus.Locked
    val iconAlpha = if (locked) 0.55f else 1f
    val titleColor = if (locked) colors.textSecondary else colors.textPrimary

    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 列表密度可调：紧凑 / 标准 / 宽松
                .padding(vertical = listRowPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.avatarMd)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.alpha(iconAlpha)) { icon() }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.type.h3,
                    color = titleColor,
                    // 两行：成就名称往往是这句话里唯一能确认"是哪一条"的信息，
                    // 截成一行会让长标题失去辨识度
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = description,
                        style = AppTheme.type.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (progress != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AppProgressBar(
                        progress = progress,
                        height = Spacing.xs,
                        color = if (locked) colors.textTertiary else colors.accent
                    )
                }
            }

            if (rarity != null || meta != null) {
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(horizontalAlignment = Alignment.End) {
                    if (rarity != null) {
                        RarityBadge(tier = rarity)
                    }
                    if (meta != null) {
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = meta,
                            style = AppTheme.type.caption,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                trailing()
            }
        }
    }

    when (variant) {
        AchievementCardVariant.Plain -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .alpha(if (locked) 0.85f else 1f)
            ) {
                body()
            }
        }

        AchievementCardVariant.Surface -> {
            AppCard(
                modifier = modifier,
                onClick = onClick,
                contentPadding = PaddingValues(vertical = Spacing.xs)
            ) {
                Box(modifier = Modifier.alpha(if (locked) 0.85f else 1f)) { body() }
            }
        }
    }
}
