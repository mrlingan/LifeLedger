package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.CategoryProgress
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/** 首页只摆这么多个分类，其余的去图鉴看 */
private const val CATEGORY_LIMIT = 5

/**
 * 分类进度：图鉴各分类的收集情况，一行五个小圆环。
 *
 * 只放五个 + 一个「查看全部」，而不是把 14 个分类全铺开：
 * 这一段的职责是让人一眼看到「我在积累什么」，不是一张完整报表。
 * 分类名在数据库里是中文（稳定标识），显示前由 labelOf 换成本地化文案。
 */
@Composable
internal fun CategoryProgressSection(
    items: List<CategoryProgress>,
    unlockedCount: Int,
    totalCount: Int,
    labelOf: (String) -> String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Surface
    ) {
        SectionHeader(
            title = stringResource(R.string.home_category_progress),
            subtitle = stringResource(
                R.string.home_category_subtitle,
                unlockedCount,
                totalCount
            ),
            action = {
                AppTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAll
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            items.take(CATEGORY_LIMIT).forEach { item ->
                CategoryProgressItem(
                    item = item,
                    label = labelOf(item.category),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryProgressItem(
    item: CategoryProgress,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppProgressRing(
            progress = item.progress,
            diameter = Sizes.categoryRing,
            strokeWidth = Sizes.progressRing
        ) {
            Text(
                text = "${item.unlockedCount}",
                style = AppTheme.type.caption,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 固定两行高度：英文分类名长短差很多，固定高度百分比才能对齐
        Text(
            text = label,
            style = AppTheme.type.caption,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(Spacing.xxl)
        )

        Text(
            text = "${(item.progress * 100).roundToInt()}%",
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )
    }
}
