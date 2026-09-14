package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.CategoryProgress
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 一行摆几个分类格子：四个两字分类刚好铺满一行，英文分类名也放得下 */
private const val CATEGORY_COLUMNS = 4

/** 收起状态下先露几个分类：加上「全部」和「更多分类」正好三整行 */
private const val COLLAPSED_CATEGORIES = 10

/**
 * 分类图鉴：一格格摆出「全部 + 每个分类」，每格写明收了多少 / 共多少。
 *
 * 它取代了原来那一排横向滚动的筛选标签：分类是图鉴的骨架，
 * 横着滚动一次只能看见四五个，还得先滑才知道有哪些分类；铺成格子一眼看全。
 * 分类多了会撑得很高，所以默认只露三行，最后那格是「更多分类」，点开才全部展开。
 *
 * 选中的格子是强调色浅底 + 一圈淡描边，和首页的分类圆环是两套切法：
 * 那一边看"我在哪些方向上有积累"，这一边是"我要挑哪一类去看"。
 */
@Composable
internal fun CodexCategoryCard(
    categories: List<CategoryProgress>,
    labelOf: (String) -> String,
    selected: String?,
    onSelect: (String?) -> Unit,
    totalUnlocked: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val visible = if (expanded) categories else categories.take(COLLAPSED_CATEGORIES)
    val remaining = categories.size - visible.size
    val canCollapse = categories.size > COLLAPSED_CATEGORIES

    val allLabel = stringResource(R.string.codex_category_all)
    val moreLabel = stringResource(R.string.codex_more_categories)
    val lessLabel = stringResource(R.string.codex_fewer_categories)

    val tiles = ArrayList<CodexCategoryTile>(categories.size + 2)
    tiles += CodexCategoryTile(
        label = allLabel,
        count = stringResource(R.string.codex_count_ratio, totalUnlocked, totalCount),
        category = null
    )
    visible.forEach { item ->
        tiles += CodexCategoryTile(
            label = labelOf(item.category),
            count = stringResource(
                R.string.codex_count_ratio,
                item.unlockedCount,
                item.totalCount
            ),
            category = item.category
        )
    }
    when {
        remaining > 0 -> tiles += CodexCategoryTile(
            label = moreLabel,
            count = stringResource(R.string.codex_more_categories_count, remaining),
            category = null,
            toggle = true
        )

        expanded && canCollapse -> tiles += CodexCategoryTile(
            label = lessLabel,
            count = "",
            category = null,
            toggle = false
        )
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.codex_categories_title),
            subtitle = stringResource(R.string.codex_categories_hint)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            tiles.chunked(CATEGORY_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    row.forEach { tile ->
                        CodexCategoryTileView(
                            tile = tile,
                            selected = tile.category == selected ||
                                (tile.category == null && tile.toggle == null && selected == null),
                            onClick = {
                                when (tile.toggle) {
                                    true -> expanded = true
                                    false -> expanded = false
                                    null -> onSelect(tile.category)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 最后一行不满四个：补空位，格子才不会被拉宽
                    repeat(CATEGORY_COLUMNS - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** 一个格子要显示的内容：文字、计数、点下去选哪个分类（或展开 / 收起） */
private data class CodexCategoryTile(
    val label: String,
    val count: String,
    val category: String?,
    /** 「更多分类 / 收起」这类的动作格：true 是展开、false 是收起、null 表示这是一格分类 */
    val toggle: Boolean? = null
)

@Composable
private fun CodexCategoryTileView(
    tile: CodexCategoryTile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val background = when {
        selected -> colors.accentSoft
        tile.toggle != null -> colors.surfaceSunken.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Column(
        modifier = modifier
            .height(Sizes.codexCategoryTile)
            .clip(shape)
            .background(background)
            .then(
                if (selected) {
                    Modifier.border(Sizes.hairline, colors.accent.copy(alpha = 0.35f), shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tile.label,
            style = AppTheme.type.caption,
            color = when {
                selected -> colors.accentStrong
                tile.toggle != null -> colors.textSecondary
                else -> colors.textPrimary
            },
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (tile.count.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = tile.count,
                style = AppTheme.type.caption,
                color = if (selected) colors.accent else colors.textTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
