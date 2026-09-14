package com.Anchored.mylife.ui.components.liquidglass

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
 * 页面内玻璃（卡片）的采样源。
 *
 * 页面里的卡片和底栏不一样：它自己就长在页面上，所以**不能**用底栏那块
 * 「背景 + 页面」的采样层——那块层里包含卡片自己，玻璃会去采样自己（见上文的警告）。
 * 卡片背后真正属于「别的东西」的，只有背景图与遮罩那一层，而那一层由
 * [com.Anchored.mylife.ui.theme.LifeLedgerTheme] 铺在页面之下，页面录不到，
 * 于是由主题录好之后通过这个 CompositionLocal 发给页面。
 *
 * 没有主题提供时是 null（预览、单测），卡片退回普通磨砂底，不会消失。
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<LiquidGlassBackdrop?> { null }

/**
 * 液态玻璃这个材质本身开没开。
 *
 * 和 [LocalLiquidGlassBackdrop] 是两件事：**有没有东西可以采样** ≠ **要不要玻璃**。
 * 对话框就是最典型的例子——它活在自己的窗口里，采不到背后的页面（见
 * [com.Anchored.mylife.ui.components.AppDialog]），但它里面的按钮、分段控件仍然
 * 应该是玻璃材质：折射那一步退化成一层磨砂，高光与描边照旧。
 *
 * 所以组件判断"要不要画成玻璃"看这个值，判断"能不能折射"看采样源是不是 null：
 * - 设置里关了玻璃 / 布局预览 → 这里 false，组件退回普通平面观感；
 * - 页面上开着玻璃 → true + 有采样源，真折射；
 * - 对话框里开着玻璃 → true + 没有采样源，磨砂玻璃。
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { false }

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
