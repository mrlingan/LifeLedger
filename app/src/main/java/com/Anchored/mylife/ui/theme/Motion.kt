package com.Anchored.mylife.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * 动效系统。
 *
 * 只有四档时长，统一缓动。禁止回弹、禁止大幅度飞入飞出、禁止游戏式缩放。
 * 动效应该「快、自然、克制」。
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

    /** 退出：起步快，干脆收尾 */
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** 通用：按长度取缓动（进入用 Decelerate，退出用 Accelerate） */
    fun <T> spec(
        durationMillis: Int = Base,
        easing: Easing = Standard
    ): FiniteAnimationSpec<T> = tween(durationMillis = durationMillis, easing = easing)

    /** 数值 / 进度变化 */
    fun <T> value(): FiniteAnimationSpec<T> = tween(Medium, easing = Standard)

    /** 入场 */
    fun <T> enter(): FiniteAnimationSpec<T> = tween(Slow, easing = Decelerate)

    /** 退出 */
    fun <T> exit(): FiniteAnimationSpec<T> = tween(Base, easing = Accelerate)
}
