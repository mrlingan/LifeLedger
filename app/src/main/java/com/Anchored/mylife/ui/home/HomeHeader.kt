package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 首页顶部：问候语 + 日期 + 很轻的操作入口。
 *
 * 刻意没有用 AppTopBar：这里不是工具栏，没有底部分隔线、没有底色块，
 * 标题跟随内容一起滚动，层级完全靠字号和留白建立。
 *
 * @param avatar 设了个人资料时给的左上角头像槽位；没设就传 null，这块位置不占空间。
 */
@Composable
internal fun HomeHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    avatar: (@Composable () -> Unit)? = null,
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
                bottom = Spacing.xxl
            ),
        // 头像和"你好，xx"这一行对齐：文字块三行里问候语居中，
        // 头像竖直居中之后，视觉上就是贴着问候语那一行
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (avatar != null) {
            avatar()
            Spacer(modifier = Modifier.width(Spacing.md))
        }

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
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}
