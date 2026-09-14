package com.Anchored.mylife.ui.codex

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 图鉴页头：大标题 + 副标题 + 一颗搜索药丸。
 *
 * 搜索默认收成一颗药丸贴在标题右边（还是 [Sizes.searchPill] 的高度，展开前后不跳）。
 * 图鉴的主要内容是浏览，搜索是「想找某一条」时才走的捷径，默认状态不该占掉一整行；
 * 点一下药丸才展开成输入框，右边的「取消」把输入收回去并清空关键词。
 *
 * 水平留白交给外面的列表（contentPadding），这一层只管状态栏与垂直节奏，
 * 否则左右会各缩进两次。
 */
@Composable
internal fun CodexHeader(
    title: String,
    subtitle: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = Spacing.sm, bottom = Spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (onBack != null) {
                AppIconButton(
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    onClick = onBack
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = AppTheme.type.h1,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!expanded) {
                Spacer(modifier = Modifier.width(Spacing.md))
                SearchPill(onClick = { expanded = true })
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.codex_search_hint),
                    leadingIcon = Icons.Outlined.Search,
                    focusRequester = focusRequester,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stringResource(R.string.common_cancel),
                    style = AppTheme.type.body,
                    color = colors.accentStrong,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Radius.sm))
                        .clickable {
                            onQueryChange("")
                            expanded = false
                            keyboard?.hide()
                        }
                        .heightIn(min = Sizes.touchTarget)
                        // 点击区域是整块 48dp，文字跟着行高居中
                        .padding(horizontal = Spacing.md)
                )
            }

            // 展开就是为了打字：把焦点和键盘一起交出去，少一次点击
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}

/**
 * 收起状态的搜索入口：一个圆角药丸，图标 + 一行提示。
 *
 * 药丸本身 40dp 高（和展开后的输入框等高，切换时不会跳），但点击区域撑到 48dp——
 * 看得见的形状小一点，手指够得着的范围不能跟着变小。
 */
@Composable
private fun SearchPill(onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.pill)

    Box(
        modifier = Modifier
            .height(Sizes.touchTarget)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(Sizes.searchPill)
                .clip(shape)
                .background(colors.surface)
                .border(Sizes.hairline, colors.border, shape)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(Sizes.iconMd)
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.codex_search_pill),
                style = AppTheme.type.bodySmall,
                color = colors.textTertiary,
                maxLines = 1
            )
        }
    }
}
