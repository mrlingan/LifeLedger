package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 底部导航的一项 */
data class AppBottomBarItem(
    val icon: ImageVector,
    val label: String
)

/**
 * 底部导航中间的主操作。
 *
 * 它不是 tab：不参与选中态，点击就是执行一个动作（例如「记录成就」）。
 */
data class AppBottomBarAction(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)

/**
 * 底部导航。
 *
 * 没有用 Material 的 NavigationBar：这里没有选中色块、没有药丸指示器，
 * 选中态只体现为图标和文字换成强调色；顶部分隔线用 1dp hairline，不用阴影。
 * 系统导航栏的内边距由组件自己处理，外层不用再补。
 *
 * 传了 [centerAction] 时，四个 tab 会被均分成左右两组，主操作插在正中间——
 * 这样最常用的动作永远在拇指够得到的位置，顶栏就能腾出来只放标题。
 */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    centerAction: AppBottomBarAction? = null
) {
    val colors = AppTheme.colors
    val leftCount = if (centerAction == null) items.size else (items.size + 1) / 2

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        AppDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(Sizes.bottomBar),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                if (centerAction != null && index == leftCount) {
                    CenterActionButton(action = centerAction)
                }

                val selected = index == selectedIndex

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (selected) colors.accent else colors.textTertiary,
                        modifier = Modifier.size(Sizes.iconLg)
                    )
                    Spacer(modifier = Modifier.height(Spacing.xxs))
                    Text(
                        text = item.label,
                        style = AppTheme.type.caption,
                        color = if (selected) colors.accentStrong else colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * 中间的强调色方块。
 *
 * 全站只有这一个实心强调色块出现在导航里：它是"记录"这个动作本身，
 * 不是某个页面的选中态，所以用实心是合理的，也不需要发光或阴影。
 */
@Composable
private fun CenterActionButton(action: AppBottomBarAction) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)

    Box(
        modifier = Modifier
            .size(Sizes.bottomBarAction)
            .clip(shape)
            .background(colors.accent)
            .clickable(onClick = action.onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = action.contentDescription,
            tint = colors.onAccent,
            modifier = Modifier.size(Sizes.iconLg)
        )
    }
}
