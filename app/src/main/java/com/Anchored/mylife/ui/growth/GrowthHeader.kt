package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 成长页页头：大标题 + 一句这一页在讲什么。
 *
 * 和图鉴页头一样，水平留白交给外面的列表（contentPadding），
 * 这一层只管状态栏与垂直节奏，否则左右会各缩进两次。
 * 标题跟着内容一起滚——它是页面的一部分，不是一条固定的工具栏。
 */
@Composable
internal fun GrowthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            // 下面那段留白由列表自己的行距补上：两处都按满档给，标题和第一张卡会离得太远
            .padding(top = Spacing.sm, bottom = Spacing.sm)
    ) {
        Text(
            text = title,
            style = AppTheme.type.h1,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = subtitle,
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
