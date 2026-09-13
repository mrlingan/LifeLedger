package com.Anchored.mylife.ui.components.liquidglass

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize

/**
 * 液态玻璃的采样源。
 *
 * 玻璃栏要「折射背后的画面」，就必须先把背后那层画面录进一张 GPU 图层，
 * 玻璃再带着位移去采样它。这个对象就是那层画面的句柄：
 *
 * ```
 * val backdrop = rememberLiquidGlassBackdrop()
 *
 * // 页面 + 背景：录进图层，正常画一遍
 * Box(Modifier.liquidGlassBackdrop(backdrop)) { ...页面... }
 *
 * // 玻璃栏：画在采样层之外，避免采样到自己
 * AppBottomBar(backdrop = backdrop, ...)
 * ```
 *
 * **调用方必须保证玻璃栏不在 [liquidGlassBackdrop] 覆盖的子树里**：
 * 采样层包含玻璃栏自己的话，玻璃就会去采样自己，轻则糊成一片，重则整条栏空白。
 *
 * API 31 以下没有 `RenderEffect`，[supported] 为 false，玻璃自动退化成纯色磨砂，
 * 所以低版本设备不会有这层额外的录制开销。
 */
@Stable
class LiquidGlassBackdrop internal constructor(
    internal val layer: GraphicsLayer?
) {
    /** 当前设备能不能真正采样背景（API 31+） */
    val supported: Boolean get() = layer != null

    /** 采样层在根坐标系里的位置：玻璃栏靠它把背景对齐到自己身上 */
    internal var originInRoot by mutableStateOf(Offset.Zero)

    /** 采样层尺寸，用来判断是否已经录到过画面 */
    internal var size by mutableStateOf(IntSize.Zero)
}

/**
 * 创建一块采样源。
 *
 * 放在「背景 + 页面内容」的父容器上，玻璃栏所在的那一层不要包进来。
 */
@Composable
fun rememberLiquidGlassBackdrop(): LiquidGlassBackdrop {
    val layer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        rememberGraphicsLayer()
    } else {
        null
    }
    return remember(layer) { LiquidGlassBackdrop(layer) }
}

/**
 * 把这块内容录进 [backdrop]，同时照常显示。
 *
 * [enabled] 传 false 时整个修饰符是个空操作：底栏收起（二级页面）的时候没必要
 * 每帧多录一遍整屏画面。
 */
fun Modifier.liquidGlassBackdrop(
    backdrop: LiquidGlassBackdrop?,
    enabled: Boolean = true
): Modifier {
    val layer = backdrop?.layer
    if (layer == null || !enabled) return this
    return this
        .onGloballyPositioned { coordinates ->
            backdrop.originInRoot = coordinates.positionInRoot()
            backdrop.size = coordinates.size
        }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }
}
