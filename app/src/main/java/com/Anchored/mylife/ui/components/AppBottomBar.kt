package com.Anchored.mylife.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassTabBar

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
 * 悬浮底栏压在内容上的高度。
 *
 * 底栏不再占走页面的一行空间，而是浮在页面之上——列表因此可以从玻璃底下穿过去，
 * 玻璃才有东西可以折射。代价是每个顶级页面要自己把「最后一条」抬到玻璃上方，
 * 也就是在滚动内容的下内边距里加上这个值。
 *
 * 二级页面（详情 / 新建 / 备份）没有底栏，那里的值是 [0.dp]。
 */
val LocalBottomBarClearance = staticCompositionLocalOf<Dp> { 0.dp }

/**
 * 液态玻璃底部导航。
 *
 * 整条栏是一块采样背后画面的玻璃，选中态是一颗会滑动、拉长、吸附的玻璃滴；
 * 图标与文字画在玻璃之上，保持清晰。具体渲染见
 * [com.Anchored.mylife.ui.components.liquidglass.GlassSurface]。
 *
 * 系统导航栏的内边距由组件自己处理，外层不用再补。
 *
 * 传了 [centerAction] 时，四个 tab 会被均分成左右两组，主操作插在正中间——
 * 这样最常用的动作永远在拇指够得到的位置，顶栏就能腾出来只放标题。
 *
 * @param backdrop 页面画面采样源。必须来自包裹了「背景 + 页面内容」的那一层，
 *   且底栏自己不在那一层里，否则玻璃会采样到自己
 *   传 null 表示不启用液态玻璃：整条栏变成纯色磨砂，不再折射背后的内容
 */
@Composable
fun AppBottomBar(
    items: List<AppBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LiquidGlassBackdrop?,
    modifier: Modifier = Modifier,
    centerAction: AppBottomBarAction? = null
) {
    LiquidGlassTabBar(
        items = items,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        backdrop = backdrop,
        modifier = modifier,
        centerAction = centerAction
    )
}
