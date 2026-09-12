package com.Anchored.mylife.ui.components

import com.Anchored.mylife.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 两种形态：大标题（首页、图鉴这类顶级页面）与紧凑（二级页面） */
enum class AppTopBarStyle {
    Large,
    Compact
}

/**
 * 统一顶栏，替代原先三个各自实现的 Header（HomeHeader / CodexHeader / FormHeader）。
 *
 * - `Large`：跟随内容滚动的大标题，无底色，靠留白建立层级
 * - `Compact`：固定高度，surface 底色 + 底部分隔线，不用阴影
 *
 * @param withStatusBarPadding 放在 Scaffold 的 topBar 里时保持 true；
 *        如果外层已经处理过状态栏内边距，传 false。
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    style: AppTopBarStyle = AppTopBarStyle.Compact,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    withStatusBarPadding: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = AppTheme.colors
    val insets = if (withStatusBarPadding) Modifier.statusBarsPadding() else Modifier

    when (style) {
        AppTopBarStyle.Compact -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .then(insets)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.topBar)
                        .padding(horizontal = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        AppIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            onClick = onBack
                        )
                    } else {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                    }

                    Text(
                        text = title,
                        style = AppTheme.type.h3,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = Spacing.xs)
                    )

                    actions()
                }

                AppDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        AppTopBarStyle.Large -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .then(insets)
                    .padding(
                        start = Sizes.gutter,
                        end = Sizes.gutter,
                        top = Spacing.sm,
                        bottom = Spacing.lg
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        AppIconButton(
                            icon = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            onClick = onBack
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    actions()
                }

                Text(
                    text = title,
                    style = AppTheme.type.h1,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = AppTheme.type.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
