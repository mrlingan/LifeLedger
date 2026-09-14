package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 首页顶部：日期 + 问候语 + 座右铭，右边是很轻的操作入口。
 *
 * 刻意没有用 AppTopBar：这里不是工具栏，没有底部分隔线、没有底色块，
 * 标题跟随内容一起滚动，层级完全靠字号和留白建立。日期用第三档文字压在最上面，
 * 问候语是整页字号最大的一行——第一眼看的是"我是谁"，不是任何一个数字；
 * 数字留给下面的卡片。头像也不在这里：它属于紧接着的那张个人卡片。
 *
 * @param eyebrow 日期这类"上面一行小字"，一般传当天日期。
 */
@Composable
internal fun HomeHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Sizes.gutter,
                end = Spacing.sm,
                top = Spacing.xl,
                bottom = Spacing.lg
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            Text(
                text = title,
                style = AppTheme.type.h1,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = subtitle,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }

        Row(
            content = actions
        )
    }
}
