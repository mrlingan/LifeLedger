package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 首页收尾：一行「从什么时候开始记录」。
 *
 * 这行字是真实数据（第一条记录的时间 + 累计记录天数），不是标语——
 * 它回答「我用它记录了多久」，是别的页面看不到的信息。
 *
 * 现在它单独占一张很矮的卡、文字居中：这一段是全页最后一块，
 * 一条分割线加一行小字容易被当成列表的尾巴，包成卡片才看得出"这是页面在收尾"。
 */
@Composable
internal fun HomeFooter(
    recordLine: String?,
    modifier: Modifier = Modifier
) {
    if (recordLine == null) return

    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(
            horizontal = Spacing.lg,
            vertical = Spacing.lg
        )
    ) {
        Text(
            text = recordLine,
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
