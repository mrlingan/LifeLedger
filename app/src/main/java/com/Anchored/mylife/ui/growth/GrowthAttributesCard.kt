package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.ATTRIBUTE_XP_PER_LEVEL
import com.Anchored.mylife.ui.GrowthAttribute
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.firstGlyph
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.xpText

/** 一行摆三个属性：格子够宽，"知识 / Lv.12 / 320 600" 三行都放得下 */
private const val ATTRIBUTE_COLUMNS = 3

/** 收起时先露两整行，剩下的折进「查看全部」 */
private const val COLLAPSED_ATTRIBUTES = 6

/**
 * 人生属性：完成的经历按分类长成一格一格的属性。
 *
 * 这一格回答的是"我的积累偏在哪几个方向"：每个分类下完成了多少条、
 * 折成多少 XP、到了第几级。等级和进度都是现算的（见 [GrowthAttribute]），
 * 所以它不是一个需要维护的新系统，只是同一批经历的另一种读法。
 *
 * 分类名和颜色沿用首页那一套：名字走 [labelOf]（图鉴分类在数据库里存中文）、
 * 颜色走 [colorOf]（以前挑过分类颜色的就用他自己的那个，没有的跟随主题），
 * 两页看到的同一个分类才是同一个东西。
 */
@Composable
internal fun GrowthAttributesCard(
    attributes: List<GrowthAttribute>,
    labelOf: (String) -> String,
    colorOf: (String) -> Color?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val visible = if (expanded) attributes else attributes.take(COLLAPSED_ATTRIBUTES)

    AppCard(modifier = modifier, tone = AppCardTone.Glass) {
        SectionHeader(
            title = stringResource(R.string.growth_attributes_title),
            subtitle = stringResource(R.string.growth_attributes_hint),
            action = if (attributes.size > COLLAPSED_ATTRIBUTES) {
                {
                    AppTextLink(
                        text = stringResource(
                            if (expanded) R.string.growth_show_less else R.string.home_view_all
                        ),
                        onClick = { onExpandedChange(!expanded) },
                        // 「查看全部」是去别处看，「收起」是就地折起来：只有前者带箭头
                        showChevron = !expanded
                    )
                }
            } else {
                null
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (attributes.isEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.growth_attributes_empty_title),
                    style = AppTheme.type.body,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.growth_attributes_empty_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
            return@AppCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            visible.chunked(ATTRIBUTE_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    row.forEach { attribute ->
                        AttributeTile(
                            attribute = attribute,
                            label = labelOf(attribute.category),
                            color = colorOf(attribute.category),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 最后一行不满三个：补上空位，格子才不会被拉宽
                    repeat(ATTRIBUTE_COLUMNS - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 一格属性：分类图标 + 名字 + 等级 + 进度 + 「这一级攒了多少 / 一级要多少」。
 *
 * 图标用的是分类名的首字（和成就徽记、头像同一套做法）：一句话里能写出来的名字，
 * 首字在任何语言、任何字体下都稳定，不用为十几个分类再配一套图标资源。
 */
@Composable
private fun AttributeTile(
    attribute: GrowthAttribute,
    label: String,
    color: Color?,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val accent = color ?: colors.accent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            // 比卡片再亮一档的浅色块：玻璃卡片上要看得见"这里是一格"，
            // 用半透明的表面色而不是灰底，深色下才不会变成一块黑斑
            .background(colors.surface.copy(alpha = 0.55f))
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.growthAttributeIcon)
                .clip(CircleShape)
                // 一层强调色的淡底：挑过颜色的分类就用它自己的色，和首页那几条进度条同源
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.firstGlyph(),
                style = AppTheme.type.bodyLarge,
                color = accent
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = label,
            style = AppTheme.type.bodySmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xxs))

        Text(
            text = stringResource(R.string.home_level, attribute.level),
            style = AppTheme.type.caption,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        AppProgressBar(
            progress = attribute.levelProgress,
            color = accent,
            height = Sizes.growthMicroBar
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = stringResource(
                R.string.growth_attribute_value,
                xpText(attribute.xpIntoLevel),
                xpText(ATTRIBUTE_XP_PER_LEVEL)
            ),
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
