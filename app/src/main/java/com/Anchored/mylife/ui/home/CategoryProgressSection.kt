package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.CategoryProgress
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressBar
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
 * 分类进度：左边一个总完成度的圆环，右边一行一个分类。
 *
 * 左边回答"整体攒到什么程度了"，右边回答"在哪些方向上攒"——先看总量再看分布，
 * 比把每个分类都画成一个一样大的圆环更符合"仪表盘"的读法：
 * 原来一行五个圆环，五个圈一样大、一样重，看完记不住任何一个数字。
 *
 * 分类名在数据库里是中文（稳定标识），显示前由 [labelOf] 换成本地化文案；
 * 每条的颜色由 [colorOf] 给（用户在「设置 → 首页板块」里挑过），没挑过用主题强调色。
 */
@Composable
internal fun CategoryProgressSection(
    items: List<CategoryProgress>,
    unlockedCount: Int,
    totalCount: Int,
    /** 总完成度：已完成的记录 / 全部记录，进圆环的那个百分比 */
    overallProgress: Float,
    labelOf: (String) -> String,
    colorOf: (String) -> Color? = { null },
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val colors = AppTheme.colors
    val percent = (overallProgress.coerceIn(0f, 1f) * 100).roundToInt()

    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            AppProgressRing(
                progress = overallProgress,
                diameter = Sizes.codexRing,
                strokeWidth = Sizes.codexRingStroke
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$percent%",
                        style = AppTheme.type.h3,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.home_category_overall),
                        style = AppTheme.type.caption,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items.take(CATEGORY_LIMIT).forEach { item ->
                    CategoryRow(
                        item = item,
                        label = labelOf(item.category),
                        color = colorOf(item.category)
                    )
                }
            }
        }
    }
}

/** 一个分类：左边名字、右边计数，下面一条这个分类自己颜色的进度条 */
@Composable
private fun CategoryRow(
    item: CategoryProgress,
    label: String,
    color: Color?
) {
    val colors = AppTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = AppTheme.type.bodySmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(Spacing.sm))

            Text(
                text = stringResource(
                    R.string.home_category_count,
                    item.unlockedCount,
                    item.totalCount
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        AppProgressBar(
            progress = item.progress,
            color = color ?: colors.accent
        )
    }
}
