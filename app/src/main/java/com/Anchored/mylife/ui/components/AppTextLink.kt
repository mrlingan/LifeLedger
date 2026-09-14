package com.Anchored.mylife.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 分区标题右侧的次要动作，例如「查看全部 ›」。
 *
 * 比 AppButton 轻一档：只有文字 + 箭头，用强调色，不占视觉重量，
 * 但保留 48dp 的最小点击高度。
 *
 * @param showChevron 箭头只在"点了会去别处"时有意义；就地展开 / 收起这类动作
 *        不画箭头，否则会让人以为要跳走（见成长页的「收起」「新建」）
 */
@Composable
fun AppTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showChevron: Boolean = true
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .clickable(onClick = onClick)
            .heightIn(min = Sizes.touchTarget)
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppTheme.type.caption,
            color = colors.accentStrong
        )
        if (showChevron) {
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.accentStrong,
                modifier = Modifier.size(Sizes.iconSm)
            )
        }
    }
}
