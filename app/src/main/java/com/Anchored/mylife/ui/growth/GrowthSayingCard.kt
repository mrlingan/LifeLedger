package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.DailyEvent
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 每日一言：一天一句，取回来就存住，当天不再变。
 *
 * 这是全 App 唯一联网的功能（设置里默认关着），所以文案上要给它一个"没取到也不影响"
 * 的姿态：只有开关打开时才出现；取不到时写明原因并给一个重试，而不是留一片空白。
 * 卡片用强调色浅底，在这一页里是唯一的暖色块——它本来就是一句题外话。
 */
@Composable
internal fun GrowthSayingCard(
    event: DailyEvent?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(modifier = modifier, tone = AppCardTone.Accent) {
        Text(
            text = stringResource(R.string.growth_saying_title),
            style = AppTheme.type.h3,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        if (event == null) {
            Text(
                text = stringResource(R.string.growth_saying_empty),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            AppButton(
                text = stringResource(R.string.growth_saying_retry),
                onClick = onRefresh,
                variant = AppButtonVariant.Text
            )
        } else {
            Text(
                text = event.title,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )

            if (event.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = event.description,
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
