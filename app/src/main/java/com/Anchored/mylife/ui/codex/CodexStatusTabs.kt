package com.Anchored.mylife.ui.codex

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 图鉴的状态标签：全部 / 已解锁 / 未解锁，选中那条下面压一条强调色的短横。
 *
 * 和「分段控件」的区别不只是长相：分段控件用滑块表达"切换视图"，
 * 这里用下划线表达"这一页的三个分页"，和参考稿里那排标签是同一个语义。
 * 每个标签等宽，短横的长度跟着文字走（`IntrinsicSize.Max`），字宽多少横就多长。
 */
@Composable
internal fun CodexStatusTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                val active = index == selectedIndex

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Sizes.touchTarget)
                        .clip(RoundedCornerShape(Radius.sm))
                        .clickable { onSelect(index) },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.width(IntrinsicSize.Max),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            style = if (active) {
                                AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold)
                            } else {
                                AppTheme.type.body
                            },
                            color = if (active) colors.accent else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = Spacing.sm)
                        )

                        // 选中时从中间长出来、落位；取消选中就缩回去
                        val selection by animateFloatAsState(
                            targetValue = if (active) 1f else 0f,
                            animationSpec = AppMotion.value(),
                            label = "codexTabIndicator"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(selection)
                                .height(Sizes.tabIndicator)
                                .background(
                                    color = colors.accent.copy(alpha = selection),
                                    shape = RoundedCornerShape(Radius.pill)
                                )
                        )
                    }
                }
            }
        }

        AppDivider()
    }
}
