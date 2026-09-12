package com.Anchored.mylife.ui.home

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
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 人生进度：阶段 + 进度条 + 百分比。
 *
 * 版式是一行：左边阶段、中间进度条、右边百分比，读起来像一句话。
 * 数字都走等宽数字，变化时不会左右跳。
 */
@Composable
internal fun LifeProgressSection(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val percent = (uiState.completionRate * 100).roundToInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter)
    ) {
        Text(
            text = stringResource(R.string.home_life_progress),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_level, uiState.level),
                style = AppTheme.type.h2,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            AppProgressBar(
                progress = uiState.completionRate,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = "$percent%",
                style = AppTheme.type.numberMedium,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.home_level_next, uiState.toNextLevel),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )
    }
}
