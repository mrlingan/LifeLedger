package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.ui.CodexRecentUnlock
import com.Anchored.mylife.ui.PresetText
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppEmblem
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.appearAnimation
import com.Anchored.mylife.ui.rememberMediaThumbnail
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 一屏两列：卡片不大，两列才看得清封面和名字 */
private const val RECENT_COLUMNS = 2

/**
 * 最新解锁：最近完成的四条图鉴条目，两列摆开。
 *
 * 卡片带封面——用户当时给这条成就配过图就显示那张图，没配就用徽记占位；
 * 封面左下角压一个分类标签，下面依次是名称、描述和完成日期。
 * 「查看全部」把下面的列表切到「已解锁」，这一段就退场了（见 [PresetCodexUiState.isBrowsing]）。
 */
@Composable
internal fun CodexRecentSection(
    items: List<CodexRecentUnlock>,
    textOf: (PresetAchievement) -> PresetText,
    labelOfCategory: (String) -> String,
    tierOf: (PresetAchievement) -> RarityTier,
    onClick: (PresetAchievement) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.codex_section_recent),
            action = {
                AppTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAll
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            items.chunked(RECENT_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    row.forEach { item ->
                        val text = textOf(item.preset)
                        CodexRecentCard(
                            title = text.title,
                            description = text.description,
                            category = labelOfCategory(item.preset.category),
                            tier = tierOf(item.preset),
                            date = rememberDayText(item.completedDate),
                            photoPath = item.photoPath,
                            onClick = { onClick(item.preset) },
                            modifier = Modifier
                                .weight(1f)
                                .appearAnimation()
                        )
                    }
                    repeat(RECENT_COLUMNS - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CodexRecentCard(
    title: String,
    description: String,
    category: String,
    tier: RarityTier,
    date: String,
    photoPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val coverShape = RoundedCornerShape(Radius.md)

    AppCard(
        modifier = modifier,
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(Spacing.sm),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.codexRecentCover)
                .clip(coverShape)
                // 半透明：没配图的占位块也别把玻璃底挡死
                .background(colors.surfaceSunken.copy(alpha = 0.55f))
                .border(Sizes.hairline, colors.border, coverShape),
            contentAlignment = Alignment.Center
        ) {
            val thumbnail = photoPath?.let { path ->
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
                AppEmblem(
                    text = title,
                    tier = tier,
                    unlocked = true,
                    size = Sizes.categoryRing
                )
            }

            // 分类标签压在封面左下角，和参考稿里那个 tag 同一个位置
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.sm)
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(colors.surface.copy(alpha = 0.92f))
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
            ) {
                Text(
                    text = category,
                    style = AppTheme.type.caption,
                    color = colors.accentStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = title,
            style = AppTheme.type.bodyLarge,
            color = colors.textPrimary,
            // 固定两行：并排两张卡片的名字长短不一，标题高度不齐下沿就会错开
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(Spacing.xxs))

        Text(
            text = description,
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary,
            minLines = 1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = date,
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            maxLines = 1
        )
    }
}

/** 完成日期：和详情页的「已完成 · 2026.08.12」同一种写法 */
@Composable
private fun rememberDayText(millis: Long): String {
    val locale = Locale.getDefault()
    return remember(locale, millis) {
        SimpleDateFormat("yyyy.MM.dd", locale).format(Date(millis))
    }
}
