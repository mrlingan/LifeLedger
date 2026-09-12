package com.Anchored.mylife.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 首页收尾：一行「从什么时候开始记录」。
 *
 * 这行字是真实数据（第一条记录的时间 + 累计记录天数），不是标语——
 * 它回答「我用它记录了多久」，是别的页面看不到的信息。
 */
@Composable
internal fun HomeFooter(
    recordLine: String?,
    modifier: Modifier = Modifier
) {
    if (recordLine == null) return

    Column(modifier = modifier) {
        AppDivider(modifier = Modifier.padding(horizontal = Sizes.gutter))
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = recordLine,
            style = AppTheme.type.caption,
            color = AppTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Sizes.gutter)
        )
    }
}
