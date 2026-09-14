package com.Anchored.mylife.ui.components.liquidglass

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerInputScope
import kotlin.math.abs

/**
 * 「按住那块玻璃左右拖着走」。
 *
 * 底栏的玻璃滴和分段控件的滑块是同一个手势，所以放在这里共用一份：
 * - 位移要超过 touch slop，而且**横向位移大于纵向**才算拖动——否则手指只是
 *   想上下滚页面，却被判成拖滑块；
 * - 一旦开始拖动就把事件消费掉，外层的纵向滚动不会再跟着动；
 * - 没到阈值就抬手 = 普通点击，交给底下各自的 clickable 处理（点哪个 tab / 哪一段）。
 *
 * @param onStart 开始拖动，参数是手指的起始 x（控件坐标系）
 * @param onDrag 横向增量（px）
 * @param onEnd 拖动结束（已经超过阈值）
 * @param onCancel 没超过阈值就抬手，或者手势被打断
 */
internal suspend fun PointerInputScope.detectHorizontalDrag(
    onStart: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = viewConfiguration.touchSlop
        var travelledX = 0f
        var travelledY = 0f
        var started = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            val dx = change.position.x - change.previousPosition.x
            val dy = change.position.y - change.previousPosition.y
            if (!started) {
                travelledX += dx
                travelledY += dy
                if (abs(travelledX) > slop && abs(travelledX) > abs(travelledY)) {
                    started = true
                    onStart(change.position.x)
                }
            } else if (dx != 0f) {
                onDrag(dx)
                change.consume()
            }
        }
        if (started) onEnd() else onCancel()
    }
}
