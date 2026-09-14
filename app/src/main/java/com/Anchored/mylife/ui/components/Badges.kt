package com.Anchored.mylife.ui.components

import com.Anchored.mylife.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 成就状态 */
enum class AchievementStatus {
    /** 已完成 */
    Completed,

    /** 已加入列表，进行中 */
    InProgress,

    /** 图鉴里还没解锁 */
    Locked
}

/** 稀有度标签（跟随系统语言） */
@Composable
fun RarityTier.label(): String = when (this) {
    RarityTier.Bronze -> stringResource(R.string.rarity_bronze)
    RarityTier.Silver -> stringResource(R.string.rarity_silver)
    RarityTier.Gold -> stringResource(R.string.rarity_gold)
    RarityTier.Platinum -> stringResource(R.string.rarity_platinum)
    RarityTier.Legendary -> stringResource(R.string.rarity_legendary)
}

/**
 * 稀有度徽章。
 * 颜色只出现在描边和文字上，不做大面积铺色，不发光。
 */
@Composable
fun RarityBadge(
    tier: RarityTier,
    modifier: Modifier = Modifier
) {
    val color = AppTheme.colors.rarityColor(tier)
    val shape = RoundedCornerShape(Radius.sm)

    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .border(Sizes.hairline, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(
            text = tier.label(),
            style = AppTheme.type.caption,
            color = color
        )
    }
}

/**
 * 阶段小牌子：「Lv.12」。
 *
 * 一层强调色的浅底 + 强调色文字，摆在昵称后面。首页的个人卡片和「我的」页头
 * 都用它，两页说的阶段才是同一个东西。
 */
@Composable
fun LevelPill(
    level: Int,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.pill))
            .background(colors.accentSoft)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
    ) {
        Text(
            text = stringResource(R.string.home_level, level),
            style = AppTheme.type.caption,
            color = colors.accentStrong,
            maxLines = 1
        )
    }
}

/**
 * 阶段徽章：一个圆里写着 Lv.N。
 *
 * 和 [LevelPill] 同一套颜色（强调色浅底 + 强调色文字），只是形状从胶囊换成圆：
 * 成长页的 Hero 需要一个"头像位"大小的东西压住整张卡，胶囊在那一格里太轻。
 * 里面写的是全 App 同一个阶段数，所以它和首页、我的页永远对得上。
 */
@Composable
fun LevelBadge(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatarLg
) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.accentSoft)
            .border(Sizes.hairline, colors.accent.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.home_level, level),
            style = if (size >= Sizes.avatarLg) AppTheme.type.h2 else AppTheme.type.h3,
            color = colors.accentStrong,
            maxLines = 1
        )
    }
}

/**
 * 状态徽章：已完成 / 进行中 / 未解锁。
 *
 * [text] 可以覆盖默认文案，例如「已完成 · 2026.08.12」。
 */
@Composable
fun StatusBadge(
    status: AchievementStatus,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.sm)

    val background: Color
    val contentColor: Color
    val defaultText: String

    when (status) {
        AchievementStatus.Completed -> {
            background = colors.accentSoft
            contentColor = colors.accentStrong
            defaultText = stringResource(R.string.status_completed)
        }

        AchievementStatus.InProgress -> {
            background = colors.surfaceSunken
            contentColor = colors.textSecondary
            defaultText = stringResource(R.string.status_in_progress)
        }

        AchievementStatus.Locked -> {
            background = colors.surfaceSunken
            contentColor = colors.textTertiary
            defaultText = stringResource(R.string.status_locked)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Text(
            text = text ?: defaultText,
            style = AppTheme.type.caption,
            color = contentColor
        )
    }
}
