package com.Anchored.mylife.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.data.settings.MotionChoice
import com.Anchored.mylife.ui.components.liquidglass.GlassSurface
import com.Anchored.mylife.ui.components.liquidglass.LiquidGlassStyle
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.components.liquidglass.detectHorizontalDrag
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 轻量分段控件，替代 Material 的 FilterChip / SegmentedButton。
 *
 * 结构就是底部导航那一套的小号版：底是一条**凹槽玻璃**（比卡片暗一档，读起来像嵌进去的槽），
 * 上面是一块**会滑的玻璃滑块**——换选项时从旧位置滑到新位置，滑动途中被拉长一点，
 * 落位后收回，和底栏那颗玻璃滴同一个手法。文字画在滑块**之上**，所以滑动过程中一直是清晰的。
 *
 * 滑块按固定高度算位置：分段控件的每一格宽度相等、高度固定，
 * 拿屏幕坐标做动画才不需要每帧重新测量。
 */
@Composable
fun AppSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return

    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.sm)
    val glassEnabled = LocalLiquidGlassEnabled.current

    val body: @Composable BoxScope.() -> Unit = {
        SegmentedTrack(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            glassEnabled = glassEnabled
        )
    }

    if (glassEnabled) {
        GlassSurface(
            backdrop = LocalLiquidGlassBackdrop.current,
            style = rememberTrackGlassStyle(),
            shape = shape,
            cornerRadius = Radius.sm,
            modifier = modifier.height(Sizes.segmented),
            content = body
        )
        return
    }

    Box(
        modifier = modifier
            .height(Sizes.segmented)
            .clip(shape)
            .background(colors.surfaceSunken)
    ) {
        body()
    }
}

/** 一条槽 + 一块滑块 + 一行文字 */
@Composable
private fun BoxScope.SegmentedTrack(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    glassEnabled: Boolean
) {
    val colors = AppTheme.colors
    val motion = AppTheme.display.motion
    val animated = motion != MotionChoice.OFF
    // 时长要在组合里取好：协程里不能再调 @Composable
    val slideMs = AppMotion.duration(AppMotion.Medium)
    // 点击不带水波纹：按下时那圈方块阴影和玻璃不是一套语言。
    // 反馈交给滑块自己的位移——它滑过去就是"选中了"。
    val interactionSource = remember { MutableInteractionSource() }

    // 按住滑块（或任意一段）左右拖：滑块跟着手指走，松手吸附到最近的一段
    var dragging by remember { mutableStateOf(false) }
    var dragCenterPx by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xxs)
    ) {
        val density = LocalDensity.current
        val trackWidthPx = constraints.maxWidth.toFloat()
        val trackHeightPx = constraints.maxHeight.toFloat()
        val segmentPx = trackWidthPx / options.size

        val targetLeft = segmentPx * selectedIndex
        val targetRight = targetLeft + segmentPx
        val targetCenter = (targetLeft + targetRight) / 2f

        fun nearestIndex(x: Float): Int =
            if (segmentPx <= 0f) 0 else (x / segmentPx).toInt().coerceIn(0, options.lastIndex)

        // 两条边各自动画：往哪边走，哪条边先动、另一条边跟上，滑块就会被拉长再收回
        val left = remember { Animatable(targetLeft) }
        val right = remember { Animatable(targetRight) }

        LaunchedEffect(selectedIndex, segmentPx, animated, motion) {
            if (!animated || segmentPx <= 0f) {
                left.snapTo(targetLeft)
                right.snapTo(targetRight)
                return@LaunchedEffect
            }
            val movingRight = targetLeft > left.value
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
            val snap = tween<Float>(durationMillis = slideMs, easing = AppMotion.Standard)
            fun specFor(isLeading: Boolean): FiniteAnimationSpec<Float> = when {
                motion == MotionChoice.REDUCED -> snap
                isLeading == movingRight -> leading
                else -> trailing
            }
            launch { left.animateTo(targetLeft, specFor(isLeading = false)) }
            launch { right.animateTo(targetRight, specFor(isLeading = true)) }
        }

        val center = (left.value + right.value) / 2f
        val stretch = if (animated) {
            // 拖拽中不拉伸：手指在哪滑块就在哪，拉伸只用在"落位"那一段
            if (dragging) 0f else (abs(targetCenter - center) * 0.5f).coerceAtMost(segmentPx * 0.3f)
        } else {
            0f
        }
        val pillCenter = if (dragging) dragCenterPx else center
        val pillWidthPx = segmentPx + stretch
        val pillLeftPx = (pillCenter - pillWidthPx / 2f)
            .coerceIn(0f, (trackWidthPx - pillWidthPx).coerceAtLeast(0f))

        val pillModifier = Modifier
            .offset { IntOffset(pillLeftPx.roundToInt(), 0) }
            .size(
                width = with(density) { pillWidthPx.toDp() },
                height = with(density) { trackHeightPx.toDp() }
            )

        // 手势挂在滑块与文字的共同父节点上：它不会挡住下面那一段的点击，
        // 只有真正拖起来之后才把事件消费掉（见 detectHorizontalDrag）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(options.size, segmentPx) {
                    detectHorizontalDrag(
                        onStart = { x ->
                            dragCenterPx = x
                            dragging = true
                        },
                        onDrag = { dx ->
                            val half = segmentPx / 2f
                            dragCenterPx = (dragCenterPx + dx)
                                .coerceIn(half, (trackWidthPx - half).coerceAtLeast(half))
                        },
                        onEnd = {
                            val index = nearestIndex(dragCenterPx)
                            dragging = false
                            if (index != selectedIndex) onSelect(index)
                        },
                        onCancel = { dragging = false }
                    )
                }
        ) {
            if (glassEnabled) {
                GlassSurface(
                    backdrop = LocalLiquidGlassBackdrop.current,
                    style = rememberSegmentGlassStyle(),
                    shape = RoundedCornerShape(Radius.sm),
                    cornerRadius = Radius.sm,
                    modifier = pillModifier
                )
            } else {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pillLeftPx.roundToInt(), 0) }
                        .size(
                            width = with(density) { pillWidthPx.toDp() },
                            height = with(density) { trackHeightPx.toDp() }
                        )
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(colors.surface)
                )
            }

            // 文字在滑块之上：滑动过程中也一直是清楚的
            Row(modifier = Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = AppTheme.type.bodySmall,
                            color = if (index == selectedIndex) {
                                colors.textPrimary
                            } else {
                                colors.textSecondary
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 凹槽玻璃：底色用 [com.Anchored.mylife.ui.theme.AppColors.surfaceSunken]，
 * 比卡片暗一档才读得出"嵌进去"。
 */
@Composable
private fun rememberTrackGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surfaceSunken) {
        LiquidGlassStyle(
            refractionHeight = 8.dp,
            refractionAmount = -8.dp,
            dispersion = 0.4f,
            depth = 0.5f,
            tint = colors.surfaceSunken.copy(alpha = 0.9f),
            sheen = glass.sheen,
            rimTop = glass.rimBottom,
            rimBottom = glass.rimTop,
            frosted = colors.surfaceSunken
        )
    }
}

/** 滑块玻璃：用底栏那颗玻璃滴的颜色，比槽亮一档才浮得起来 */
@Composable
private fun rememberSegmentGlassStyle(): LiquidGlassStyle {
    val colors = AppTheme.colors
    val glass = colors.glass
    return remember(glass, colors.surfaceElevated) {
        LiquidGlassStyle(
            refractionHeight = 9.dp,
            refractionAmount = -10.dp,
            dispersion = 0.42f,
            depth = 0.45f,
            tint = glass.droplet,
            sheen = glass.dropletSheen,
            rimTop = glass.dropletRimTop,
            rimBottom = glass.dropletRimBottom,
            frosted = colors.surfaceElevated
        )
    }
}
