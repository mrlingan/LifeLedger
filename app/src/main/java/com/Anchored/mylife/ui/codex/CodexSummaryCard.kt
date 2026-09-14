package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 图鉴的收集进度：左边一个圆环（百分比在环里），右边写清已解锁多少条、还剩多少，
 * 底下压一句图鉴自己的口号，右下角是三座很淡的山。
 *
 * 山是装饰，不参与布局（`matchParentSize` 铺满整张卡），所以窄屏上也不会把文字挤走；
 * 用的是强调色的低透明度，深浅两套主题都只是"背景里有一点起伏"，不是一张插画。
 */
@Composable
internal fun CodexSummaryCard(
    unlockedCount: Int,
    totalCount: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val percent = (progress * 100).roundToInt()

    AppCard(
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Glass
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CodexPeaks(
                color = colors.accent,
                modifier = Modifier.matchParentSize()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppProgressRing(
                    progress = progress,
                    diameter = Sizes.codexRing,
                    strokeWidth = Sizes.codexRingStroke
                ) {
                    Text(
                        text = "$percent%",
                        style = AppTheme.type.numberMedium,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.lg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.codex_summary_title),
                        style = AppTheme.type.h3,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(Spacing.xs))

                    Text(
                        text = stringResource(
                            R.string.codex_summary_unlocked,
                            unlockedCount,
                            totalCount
                        ),
                        style = AppTheme.type.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = stringResource(R.string.codex_summary_quote),
                        style = AppTheme.type.caption,
                        color = colors.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 三座交叠的山，贴在整张卡的右下角。
 *
 * 只画三个三角形：中间那座最高、两侧依次矮下去，透明度都压得很低，
 * 内容画在它上面，所以它只是卡片底色里的一点起伏。
 */
@Composable
private fun CodexPeaks(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        fun peak(centerX: Float, halfWidth: Float, peakHeight: Float, alpha: Float) {
            val path = Path().apply {
                moveTo(centerX - halfWidth, height)
                lineTo(centerX, height - peakHeight)
                lineTo(centerX + halfWidth, height)
                close()
            }
            drawPath(path = path, color = color.copy(alpha = alpha))
        }

        peak(centerX = width * 0.34f, halfWidth = width * 0.30f, peakHeight = height * 0.52f, alpha = 0.06f)
        peak(centerX = width * 0.58f, halfWidth = width * 0.40f, peakHeight = height * 0.78f, alpha = 0.09f)
        peak(centerX = width * 0.84f, halfWidth = width * 0.26f, peakHeight = height * 0.44f, alpha = 0.05f)
    }
}
