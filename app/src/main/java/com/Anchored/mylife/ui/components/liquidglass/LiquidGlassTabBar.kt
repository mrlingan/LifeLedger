package com.Anchored.mylife.ui.components.liquidglass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.data.settings.MotionChoice
import com.Anchored.mylife.ui.components.AppBottomBarAction
import com.Anchored.mylife.ui.components.AppBottomBarItem
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 液态玻璃底部导航。
 *
 * 与参考实现（sjtt2/HeyBox-LiquidGlass）保持同一套结构：
 * - 整条栏是一块圆角液态玻璃，采样 [backdrop] 里那层画面；
 * - 选中态是一颗**玻璃滴**，切换时从旧位置滑到新位置，滑动途中被拉长、落位后收回；
 * - 玻璃滴可以按住拖着走，松手吸附到最近的 tab（和参考实现的「手指拖拽吸附」一致）。
 *
 * 图标与文字画在折射之上，保持清晰；玻璃只负责它下面那一层。
 *
 * @param backdrop 采样源，来自 [rememberLiquidGlassBackdrop]，由页面根部录制
 *   传 null 表示不启用液态玻璃：整条栏退回纯色磨砂，选中态仍是一颗磨砂的滴
 * @param centerAction 中间的主操作。它不是 tab：不参与选中态，点击只执行动作
 */
@Composable
fun LiquidGlassTabBar(
    items: List<AppBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdrop: LiquidGlassBackdrop?,
    modifier: Modifier = Modifier,
    centerAction: AppBottomBarAction? = null
) {
    if (items.isEmpty()) return

    val colors = AppTheme.colors
    val motion = AppTheme.display.motion
    val animated = motion != MotionChoice.OFF
    val leftCount = if (centerAction == null) items.size else (items.size + 1) / 2

    // 时长在这里取好：AppMotion.duration 是 @Composable，协程和绘制回调里不能再调
    val revealMs = AppMotion.duration(AppMotion.Slow)
    val slideMs = AppMotion.duration(AppMotion.Medium)

    val barStyle = rememberBarGlassStyle()
    val dropletStyle = rememberDropletGlassStyle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = Sizes.gutter,
                end = Sizes.gutter,
                top = Spacing.sm,
                bottom = Spacing.md
            )
    ) {
        // 进场：淡入 + 从下缘轻轻抬起，和参考实现的「玻璃栏升起」一个意思，但不带回弹
        val appear = remember { Animatable(if (animated) 0f else 1f) }
        LaunchedEffect(animated) {
            if (!animated) {
                appear.snapTo(1f)
            } else {
                appear.animateTo(1f, tween(revealMs, easing = AppMotion.Standard))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.bottomBar)
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 16.dp.toPx()
                }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = LocalDensity.current
                val barWidthPx = constraints.maxWidth.toFloat()
                val barHeightPx = constraints.maxHeight.toFloat()
                val centerWidthPx = with(density) { Sizes.bottomBarAction.toPx() }
                val tabWidthPx = (barWidthPx - centerWidthPx) / items.size
                val insetX = with(density) { Spacing.xs.toPx() }
                val insetY = with(density) { Spacing.xs.toPx() }
                val dropletHeightDp = with(density) { (barHeightPx - insetY * 2f).toDp() }

                fun tabLeftPx(index: Int): Float = tabWidthPx * index +
                    if (centerAction != null && index >= leftCount) centerWidthPx else 0f

                fun tabCenterPx(index: Int): Float = tabLeftPx(index) + tabWidthPx / 2f

                fun nearestIndex(x: Float): Int =
                    items.indices.minByOrNull { abs(tabCenterPx(it) - x) } ?: 0

                val targetLeftPx = tabLeftPx(selectedIndex) + insetX
                val targetRightPx = tabLeftPx(selectedIndex) + tabWidthPx - insetX

                val leftEdge = remember { Animatable(0f) }
                val rightEdge = remember { Animatable(0f) }
                val dragCenterPx = remember { mutableFloatStateOf(0f) }
                var dragging by remember { mutableStateOf(false) }

                LaunchedEffect(selectedIndex, tabWidthPx, dragging, animated, motion) {
                    if (dragging) return@LaunchedEffect
                    val movingRight = targetLeftPx > leftEdge.value
                    val leading = spring<Float>(
                        dampingRatio = 1f,
                        stiffness = 620f,
                        visibilityThreshold = 0.5f
                    )
                    val trailing = spring<Float>(
                        dampingRatio = 1f,
                        stiffness = 380f,
                        visibilityThreshold = 0.5f
                    )
                    val snap = tween<Float>(
                        durationMillis = slideMs,
                        easing = AppMotion.Standard
                    )
                    fun specFor(isLeading: Boolean): FiniteAnimationSpec<Float> = when {
                        !animated -> snap
                        motion == MotionChoice.REDUCED -> snap
                        isLeading == movingRight -> leading
                        else -> trailing
                    }
                    launch { leftEdge.animateTo(targetLeftPx, specFor(isLeading = false)) }
                    launch { rightEdge.animateTo(targetRightPx, specFor(isLeading = true)) }
                }

                GlassSurface(
                    backdrop = backdrop,
                    style = barStyle,
                    shape = RoundedCornerShape(Radius.pill),
                    cornerRadius = Radius.pill,
                    modifier = Modifier.fillMaxSize()
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(items.size, tabWidthPx) {
                                detectHorizontalDrag(
                                    onStart = { x ->
                                        dragCenterPx.floatValue = x
                                        dragging = true
                                    },
                                    onDrag = { dx ->
                                        dragCenterPx.floatValue = (
                                            dragCenterPx.floatValue + dx
                                            ).coerceIn(
                                            tabWidthPx / 2f + insetX,
                                            barWidthPx - tabWidthPx / 2f - insetX
                                        )
                                    },
                                    onEnd = {
                                        val index = nearestIndex(dragCenterPx.floatValue)
                                        dragging = false
                                        onSelect(index)
                                    },
                                    onCancel = { dragging = false }
                                )
                            }
                    ) {
                        Droplet(
                            backdrop = backdrop,
                            style = dropletStyle,
                            leftEdge = leftEdge,
                            rightEdge = rightEdge,
                            targetCenterPx = (targetLeftPx + targetRightPx) / 2f,
                            dragging = dragging,
                            dragCenterPx = dragCenterPx.floatValue,
                            naturalWidthPx = tabWidthPx - insetX * 2f,
                            heightDp = dropletHeightDp,
                            topInsetPx = insetY
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEachIndexed { index, item ->
                                if (centerAction != null && index == leftCount) {
                                    CenterActionSlot(
                                        action = centerAction,
                                        modifier = Modifier
                                            .width(Sizes.bottomBarAction)
                                            .fillMaxHeight()
                                    )
                                }

                                TabItem(
                                    item = item,
                                    selected = index == selectedIndex,
                                    onClick = { onSelect(index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 选中态的玻璃滴。
 *
 * 位置直接用两个边缘的动画值算，因此拖动时跟着手指走、松手后滑回去；
 * 位移还没走完的时候把它按剩余距离拉长一点，看起来像一滴液体被拽着跑，
 * 到站后自然收回——只有拉伸和回缩，没有回弹。
 */
@Composable
private fun Droplet(
    backdrop: LiquidGlassBackdrop?,
    style: LiquidGlassStyle,
    leftEdge: Animatable<Float, *>,
    rightEdge: Animatable<Float, *>,
    targetCenterPx: Float,
    dragging: Boolean,
    dragCenterPx: Float,
    naturalWidthPx: Float,
    heightDp: androidx.compose.ui.unit.Dp,
    topInsetPx: Float
) {
    val density = LocalDensity.current
    val animatedCenterPx = (leftEdge.value + rightEdge.value) / 2f
    val centerPx = if (dragging) dragCenterPx else animatedCenterPx
    val stretch = if (dragging) {
        0f
    } else {
        (abs(targetCenterPx - animatedCenterPx) * 0.5f).coerceAtMost(naturalWidthPx * 0.5f)
    }
    val widthPx = naturalWidthPx + stretch
    val leftPx = centerPx - widthPx / 2f

    GlassSurface(
        backdrop = backdrop,
        style = style,
        shape = RoundedCornerShape(Radius.pill),
        cornerRadius = Radius.pill,
        modifier = Modifier
            .offset {
                IntOffset(leftPx.roundToInt(), topInsetPx.roundToInt())
            }
            .size(
                width = with(density) { widthPx.toDp() },
                height = heightDp
            )
    )
}

/** 一个 tab：图标 + 文字；选中时图标轻轻放大一下再回到原位 */
@Composable
private fun TabItem(
    item: AppBottomBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val motion = AppTheme.display.motion
    val pop = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }
    val fastMs = AppMotion.duration(AppMotion.Fast)
    val baseMs = AppMotion.duration(AppMotion.Base)

    LaunchedEffect(selected) {
        if (!selected || motion == MotionChoice.OFF) {
            pop.snapTo(1f)
            return@LaunchedEffect
        }
        pop.snapTo(1f)
        pop.animateTo(1.14f, tween(fastMs, easing = AppMotion.Decelerate))
        pop.animateTo(1f, tween(baseMs, easing = AppMotion.Standard))
    }

    Column(
        modifier = modifier.selectable(
            selected = selected,
            interactionSource = interactionSource,
            indication = null,
            role = Role.Tab,
            onClick = onClick
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (selected) colors.accent else colors.textTertiary,
            modifier = Modifier
                .size(Sizes.iconLg)
                .scale(pop.value)
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

/**
 * 中间的主操作。
 *
 * 它从玻璃栏里微微浮起，用高透明表面和强调色描边与 tab 区分，
 * 不会像实心按钮那样打断整条导航的通透感。
 */
@Composable
private fun CenterActionSlot(
    action: AppBottomBarAction,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.pill)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.bottomBarAction)
                .shadow(elevation = Spacing.sm, shape = shape, clip = false)
                .clip(shape)
                .background(colors.surfaceElevated.copy(alpha = 0.86f))
                .border(
                    width = Sizes.hairline,
                    color = colors.accent.copy(alpha = 0.78f),
                    shape = shape
                )
                .clickable(onClick = action.onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.contentDescription,
                tint = colors.accent,
                modifier = Modifier.size(Sizes.iconLg)
            )
        }
    }
}

/**
 * 玻璃栏的玻璃参数：边缘一圈折射 + 轻微色散。
 *
 * `frosted` 是「没开玻璃」时铺的底色，见下面各字段的注释。
 */
@Composable
private fun rememberBarGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surface) {
        LiquidGlassStyle(
            refractionHeight = 18.dp,
            refractionAmount = -26.dp,
            dispersion = 0.30f,
            depth = 0.3f,
            tint = glass.tint,
            sheen = glass.sheen,
            rimTop = glass.rimTop,
            rimBottom = glass.rimBottom,
            // 没有玻璃可采样时的兜底填充（关掉开关、或设备不支持）：
            // 给实心表面色，半透明的磨砂底会让内容从栏里透出来，像没画完
            frosted = colors.surface
        )
    }
}

/** 玻璃滴更「厚」一点：折射带更窄、位移更小，色散更明显；兜底填充比栏深一档 */
@Composable
private fun rememberDropletGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surfaceSunken) {
        LiquidGlassStyle(
            refractionHeight = 12.dp,
            refractionAmount = -14.dp,
            dispersion = 0.5f,
            depth = 0.45f,
            tint = glass.droplet,
            sheen = glass.dropletSheen,
            rimTop = glass.dropletRimTop,
            rimBottom = glass.dropletRimBottom,
            frosted = colors.surfaceSunken
        )
    }
}
