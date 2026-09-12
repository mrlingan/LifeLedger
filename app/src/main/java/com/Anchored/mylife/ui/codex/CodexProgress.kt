package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 图鉴总进度：已解锁数量（主数字）+ 总数 + 百分比 + 一条进度条。
 *
 * 数字层级：已解锁数是主角（numberLarge），总数是陪衬（bodySmall），
 * 百分比介于两者之间。没有环形、没有渐变，就是一条 6dp 的轨道。
 */
@Composable
internal fun CodexProgress(
    unlockedCount: Int,
    totalCount: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val percent = (progress * 100).roundToInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = unlockedCount.toString(),
                style = AppTheme.type.numberLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.home_metric_of_total, totalCount),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$percent%",
                style = AppTheme.type.numberMedium,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        AppProgressBar(progress = progress)
    }
}
