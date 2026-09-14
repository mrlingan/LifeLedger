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
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 人生进度：阶段 + 进度条 + 百分比。
 *
 * 这是首页的 Hero 卡片：等级和进度占主视觉，距离下一等级是次要信息。
 * 既保留真实数据，又给首页一个明确的视觉锚点。
 */
@Composable
internal fun LifeProgressSection(
    uiState: HomeUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val percent = (uiState.completionRate * 100).roundToInt()

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_life_progress),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.home_level, uiState.level),
                    style = AppTheme.type.display,
                    color = colors.textPrimary
                )
            }
            Text(
                text = "$percent%",
                style = AppTheme.type.numberLarge,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        AppProgressBar(progress = uiState.completionRate, color = colors.accent)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.home_level_next, uiState.toNextLevel),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
    }
}
