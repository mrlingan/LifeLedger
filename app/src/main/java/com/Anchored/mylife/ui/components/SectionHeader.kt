package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 分区标题，用于把页面切成几段（最近解锁 / 人生数据 / 继续完成）。
 * 右侧可以挂一个次要操作。
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.h3,
                color = AppTheme.colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = AppTheme.type.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }

        if (action != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            action()
        }
    }
}

/**
 * 时间线节点，用于「解锁记录」和未来的成长时间线。
 * 靠轴线和留白分隔，不用卡片。
 */
@Composable
fun TimelineItem(
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    isLast: Boolean = false,
    emphasized: Boolean = false
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(Spacing.sm)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Spacing.sm)
                    .clip(CircleShape)
                    .background(if (emphasized) colors.accent else colors.border)
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(Sizes.hairline)
                        .weight(1f)
                        .background(colors.divider)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Spacing.lg)
        ) {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = time,
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = description,
                    style = AppTheme.type.body,
                    color = colors.textSecondary
                )
            }
        }
    }
}
