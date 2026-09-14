package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 分区标题，用于把页面切成几段（最近解锁 / 人生数据 / 继续完成）。
 * 右侧可以挂一个次要操作。
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.h3,
                color = AppTheme.colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = AppTheme.type.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }

        if (action != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            action()
        }
    }
}

/**
 * 卡片里的分区标题：行首一枚符号方块 + 标题，行尾挂一句补充说明。
 *
 * 和 [SectionHeader] 是两种用法，不要互换：那个是"页面上的一段"（标题自带标题字重，
 * 挂在卡片外面），这个是"卡片里的一段"——它通常就是一张分组卡的第一行，
 * 下面紧跟着 [AppSettingRow]。
 *
 * 行首的方块复用设置行的 [SettingMark]（软色底 + 符号），行尾的说明用次要文字色：
 * 一行里最重的永远是"这一段叫什么"，说明和值都只是补充。
 *
 * @param glyph 行首方块里的符号，见 [AppSettingRow] 里对符号的说明；
 *   传 null 表示只要标题
 * @param hint 行尾的补充说明（例如"完善信息，解锁更多功能"）。
 *   放不下时省略号收尾——它是补充，不能把标题挤断
 */
@Composable
fun AppSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    glyph: String? = null,
    hint: String? = null
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (glyph != null) {
            SettingMark(background = colors.accentSoft) {
                Text(
                    text = glyph,
                    style = AppTheme.type.glyph,
                    color = colors.accentStrong
                )
            }
            Spacer(modifier = Modifier.width(Spacing.md))
        }

        Text(
            text = title,
            style = AppTheme.type.h3,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (hint != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = hint,
                style = AppTheme.type.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 时间线节点，用于「解锁记录」和未来的成长时间线。
 * 靠轴线和留白分隔，不用卡片。
 */
@Composable
fun TimelineItem(
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isLast: Boolean = false,
    emphasized: Boolean = false
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(Spacing.sm)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(if (emphasized) colors.accent else colors.border)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(Sizes.hairline)
                        .weight(1f)
                        .background(colors.divider)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Spacing.lg)
        ) {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = time,
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = description,
                    style = AppTheme.type.body,
                    color = colors.textSecondary
                )
            }
        }
    }
}
