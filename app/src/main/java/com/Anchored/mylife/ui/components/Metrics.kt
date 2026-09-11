package com.Anchored.mylife.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 核心数字。这个 App 是「人生数据系统」，数字是主角：
 * 数字永远比它的标签更大更重。
 */
@Composable
fun MetricNumber(
    value: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.type.numberLarge,
    color: Color = AppTheme.colors.textPrimary
) {
    Text(
        text = value,
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * 带滚动动画的数字：首次出现时从 0 滚到目标值，
 * 之后数值变化也会平滑过渡（等宽数字保证不跳版）。
 */
@Composable
fun AnimatedMetricNumber(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.type.numberLarge,
    color: Color = AppTheme.colors.textPrimary
) {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }

    val shown by animateIntAsState(
        targetValue = target,
        animationSpec = AppMotion.value(),
        label = "metricNumber"
    )

    Text(
        text = shown.toString(),
        style = style,
        color = color,
        modifier = modifier
    )
}

/**
 * 数据块：一个大数字 + 标签 + 可选的补充说明。
 *
 * 刻意不带卡片外框——几个 StatTile 并排放在一个容器里，
 * 而不是每个数字各占一张卡。
 */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    emphasized: Boolean = false,
    valueColor: Color = AppTheme.colors.textPrimary
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = if (emphasized) AppTheme.type.numberLarge else AppTheme.type.numberMedium,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = AppTheme.type.bodySmall,
            color = AppTheme.colors.textSecondary
        )
        if (supporting != null) {
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = supporting,
                style = AppTheme.type.caption,
                color = AppTheme.colors.textTertiary
            )
        }
    }
}
