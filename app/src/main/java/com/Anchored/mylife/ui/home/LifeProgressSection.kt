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
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.HomeUiState
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.LevelPill
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 首页的个人卡片：谁在记录 + 走到哪一阶段了。
 *
 * 这是整页的 Hero：左边头像，中间昵称和阶段，右边完成百分比，下面一条进度条
 * 和「距离下一阶段还有几条」。原来是拆成两块的（问候语旁边的头像、单独一张
 * 「人生进度」）——合成一张之后，第一眼看的是"这是我的人生、我走到这儿了"，
 * 而不是一个孤零零的等级数字。
 *
 * 签名（用户自己写的那句话）如果设过就压在卡片底部：它是这一页唯一的个人文案，
 * 放在属于"我"的这张卡里比放在问候语下面更有归属感。没设过就整块不出现——
 * 一对空引号比没有引号更难看。
 *
 * 整张卡可点，点开的是阶段的说明（[onClick]），和以前一样。
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
            AppAvatar(
                name = uiState.nickname,
                path = uiState.avatarPath,
                preset = uiState.avatarPreset,
                size = Sizes.avatarLg
            )

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.nickname.ifBlank {
                            stringResource(R.string.my_default_name)
                        },
                        style = AppTheme.type.h2,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    LevelPill(level = uiState.level)
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = stringResource(
                        R.string.home_level_progress,
                        uiState.completedCount,
                        uiState.totalCount
                    ),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Text(
                text = "$percent%",
                style = AppTheme.type.numberMedium,
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

        if (uiState.signature.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            AppDivider()
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "「${uiState.signature}」",
                style = AppTheme.type.bodySmall,
                color = colors.textTertiary
            )
        }
    }
}
