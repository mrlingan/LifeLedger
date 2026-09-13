package com.Anchored.mylife.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.Anchored.mylife.data.settings.MotionChoice
import kotlin.math.roundToInt

/**
 * 动效系统。
 *
 * 只有四档时长，统一缓动。禁止回弹、禁止大幅度飞入飞出、禁止游戏式缩放。
 * 动效应该「快、自然、克制」；「优雅」档则在此基础上延长落点，营造更舒展的节奏。
 *
 * 用法：
 * ```
 * val progress by animateFloatAsState(
 *     targetValue = value,
 *     animationSpec = AppMotion.value(),
 *     label = "progress"
 * )
 * ```
 */
object AppMotion {

    /** 微反馈：按下、勾选、图标切换 */
    const val Fast = 150

    /** 默认：状态切换、颜色变化 */
    const val Base = 200

    /** 数值变化、进度增长 */
    const val Medium = 250

    /** 页面与列表入场 */
    const val Slow = 300

    /** 标准缓动：先快后缓，最常用 */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** 进入：起步轻，落点稳 */
    val Decelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** 优雅：前段快速建立反馈，后段更长、更柔和地贴合最终位置。 */
    val Elegant: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** 退出：起步快，干脆收尾 */
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** 通用：按长度取缓动（进入用 Decelerate，退出用 Accelerate） */
    @Composable
    fun <T> spec(
        durationMillis: Int = Base,
        easing: Easing = Standard
    ): FiniteAnimationSpec<T> = tween(durationMillis = duration(durationMillis), easing = easing)

    /** 数值 / 进度变化 */
    @Composable
    fun <T> value(): FiniteAnimationSpec<T> = tween(duration(Medium), easing = Standard)

    /** 入场 */
    @Composable
    fun <T> enter(): FiniteAnimationSpec<T> = tween(duration(Slow), easing = enterEasing())

    /** 退出 */
    @Composable
    fun <T> exit(): FiniteAnimationSpec<T> = tween(duration(Base), easing = exitEasing())

    /** 当前设置对应的进入曲线，供页面转场和列表入场共用。 */
    @Composable
    @ReadOnlyComposable
    fun enterEasing(): Easing = when (AppTheme.display.motion) {
        MotionChoice.ELEGANT -> Elegant
        else -> Decelerate
    }

    /** 优雅模式的退出不急刹，仍使用标准缓动保持视觉连贯。 */
    @Composable
    @ReadOnlyComposable
    fun exitEasing(): Easing = when (AppTheme.display.motion) {
        MotionChoice.ELEGANT -> Standard
        else -> Accelerate
    }

    /**
     * 按当前动效设置折算时长。
     *
     * 「精简」整体打六折，「关闭」直接给 0（等同于立即到位）。
     * 上面的时长常量本身不变——它们仍是四种反馈的语义刻度。
     */
    @Composable
    @ReadOnlyComposable
    fun duration(base: Int): Int = when (AppTheme.display.motion) {
        MotionChoice.ELEGANT -> (base * 1.45f).roundToInt()
        MotionChoice.FULL -> base
        MotionChoice.REDUCED -> (base * 0.6f).roundToInt()
        MotionChoice.OFF -> 0
    }
}
