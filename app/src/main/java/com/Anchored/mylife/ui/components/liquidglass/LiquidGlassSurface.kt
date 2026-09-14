package com.Anchored.mylife.ui.components.liquidglass

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.R

/**
 * 液态玻璃的渲染参数。
 *
 * 默认值来自 sjtt2/HeyBox-LiquidGlass 里那套 AGSL 透镜管线：
 * 边缘一圈「折射带」把背后的画面按圆角矩形梯度掰弯，越靠边掰得越狠，
 * 再叠一层很轻的色散（红蓝分离）和极轻的模糊，玻璃才有厚度感。
 */
@Immutable
data class LiquidGlassStyle(
    /** 折射带宽度：从边缘往里多少距离开始掰弯画面 */
    val refractionHeight: Dp = 18.dp,
    /** 折射位移上限。负值表示把画面「往外拉」，看起来像凸透镜 */
    val refractionAmount: Dp = -26.dp,
    /** 色散强度：边缘的红蓝分离，0 = 无色散 */
    val dispersion: Float = 0.32f,
    /** 采样前的模糊半径（px sigma）。液态玻璃靠折射而不是糊，所以给得很小 */
    val blurSigma: Float = 1.2f,
    /** 梯度里「朝圆心」那一份的占比，越大越像厚玻璃 */
    val depth: Float = 0.3f,
    /** 饱和度倍数，1 = 不变 */
    val chroma: Float = 1f,
    /** 对比度增量，0 = 不变 */
    val contrast: Float = 0f,
    /** > 0 整体提亮、< 0 整体压暗，0 = 不变 */
    val whitePoint: Float = 0f,
    /** 玻璃本身的颜色，alpha 决定「奶」到什么程度 */
    val tint: Color,
    /** 上缘高光渐变 */
    val sheen: Color,
    /** 描边：上缘亮、下缘暗，玻璃的边缘才有转折 */
    val rimTop: Color,
    val rimBottom: Color,
    /** 没有 GPU 采样能力时的兜底底色 */
    val frosted: Color,
    /** 描边粗细 */
    val rimWidth: Dp = 1.dp
)

/**
 * 一块液态玻璃。
 *
 * 结构分三层，顺序很重要：
 * 1. 玻璃本体——采样 [backdrop]、按 [style] 折射，自带形状裁剪；
 * 2. [content]——图标 / 文字画在折射之上，保持清晰；
 * 3. 高光与描边——不受折射影响，盖在内容之上。
 *
 * [modifier] 必须给出尺寸（例如 `fillMaxWidth().height(...)`）：玻璃本体是按
 * 父容器尺寸铺的，自己不定尺寸就会量成 0。
 *
 * [backdrop] 传 null 表示当前不启用液态玻璃（设置里关掉了，或者跑在预览里）：
 * 这时不采样任何画面，只铺一层 [LiquidGlassStyle.frosted] 磨砂，
 * 高光与描边照常——和 API 31 以下设备的观感一致。
 */
@Composable
fun GlassSurface(
    backdrop: LiquidGlassBackdrop?,
    style: LiquidGlassStyle,
    shape: Shape,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val density = LocalDensity.current
    val radiusPx = with(density) { cornerRadius.toPx() }
    val densityScale = density.density
    val pipeline = rememberLiquidGlassPipeline()
    var originInRoot by remember { mutableStateOf(Offset.Zero) }

    // 采样源为 null（对话框、锁屏这类采不到背后画面的地方）时玻璃铺的是
    // [LiquidGlassStyle.frosted] 那层磨砂。tint 的语义是"叠在**背后画面**上的颜色"，
    // 这时候再按它去混，等于把 frosted 自己冲淡一遍 —— 锁屏上的键盘键会淡到几乎
    // 看不见。所以把 tint 换成 frosted 本身：mix 一次等于没混，磨砂底原样呈现，
    // 高光与描边照旧。有采样源时什么都不变。
    val bodyStyle = if (backdrop == null) style.copy(tint = style.frosted) else style

    Box(
        modifier = modifier.onGloballyPositioned { originInRoot = it.positionInRoot() },
        // 玻璃是个容器：内容比它小的时候就居中放。Box 默认把子项量成"最小宽度 0"，
        // 也就是内容自己有多宽就多宽，再按自己的 contentAlignment 摆 —— 不写这一行，
        // 固定宽度的玻璃按钮里的文字会贴在左边，看起来就像"没居中"。
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .glassBody(
                    backdrop = backdrop,
                    style = bodyStyle,
                    shape = shape,
                    radiusPx = radiusPx,
                    densityScale = densityScale,
                    originInRoot = originInRoot,
                    pipeline = pipeline
                )
        )

        // 上缘高光画在内容**之下**：它是玻璃表面的反光，不是盖在字上的一层白雾。
        // 画在上面会把按钮标签、导航栏文字的上半截冲淡，字看着就像没对齐。
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val sheen = Brush.verticalGradient(
                        0f to style.sheen,
                        0.45f to Color.Transparent,
                        1f to Color.Transparent
                    )
                    onDrawBehind {
                        drawRoundRect(brush = sheen, cornerRadius = CornerRadius(radiusPx))
                    }
                }
        )

        content()

        // 描边压在内容之上：玻璃的边缘在内容外面，不会被文字盖住
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val stroke = with(density) { style.rimWidth.toPx() }
                    val inset = stroke / 2f
                    val radius = (radiusPx - inset).coerceAtLeast(0f)
                    val outline = Path().apply {
                        addRoundRect(
                            RoundRect(
                                rect = Rect(
                                    left = inset,
                                    top = inset,
                                    right = size.width - inset,
                                    bottom = size.height - inset
                                ),
                                topLeft = CornerRadius(radius),
                                topRight = CornerRadius(radius),
                                bottomRight = CornerRadius(radius),
                                bottomLeft = CornerRadius(radius)
                            )
                        )
                    }
                    val rim = Brush.verticalGradient(listOf(style.rimTop, style.rimBottom))
                    onDrawBehind {
                        drawPath(path = outline, brush = rim, style = Stroke(width = stroke))
                    }
                }
        )
    }
}

/**
 * 玻璃本体：把采样层按屏幕坐标对齐着画进来，再交给着色器折射。
 *
 * 对齐方式与参考实现一致——不裁切背后那张图，而是把它整体平移，
 * 让「屏幕坐标」和「玻璃自己的坐标」重合，于是 `content.eval(coord)` 拿到的
 * 就是玻璃这一块位置背后真正的像素。多出来的部分由 [shape] 裁掉。
 */
private fun Modifier.glassBody(
    backdrop: LiquidGlassBackdrop?,
    style: LiquidGlassStyle,
    shape: Shape,
    radiusPx: Float,
    densityScale: Float,
    originInRoot: Offset,
    pipeline: LiquidGlassPipeline
): Modifier = this
    .graphicsLayer {
        if (size.width <= 0f || size.height <= 0f) return@graphicsLayer
        clip = true
        this.shape = shape
        compositingStrategy = CompositingStrategy.Offscreen
        renderEffect = pipeline.effect(
            widthPx = size.width,
            heightPx = size.height,
            radiusPx = radiusPx,
            densityScale = densityScale,
            style = style
        )
    }
    .drawWithCache {
        val backdropLayer = backdrop?.layer
        val offsetX = (backdrop?.originInRoot?.x ?: 0f) - originInRoot.x
        val offsetY = (backdrop?.originInRoot?.y ?: 0f) - originInRoot.y
        val fallbackRadius = CornerRadius(radiusPx)
        onDrawBehind {
            if (backdropLayer != null) {
                translate(left = offsetX, top = offsetY) {
                    drawLayer(backdropLayer)
                }
            } else {
                // 没开玻璃（或低版本没有 RenderEffect）：不采样，只铺一层磨砂
                drawRoundRect(color = style.frosted, cornerRadius = fallbackRadius)
            }
        }
    }

/**
 * 着色器 + 渲染管线的持有者。
 *
 * `RenderEffect` 按 uniform 的取值缓存：同一组 uniform 反复画就复用同一个对象，
 * 取值一变就重建一份（原因见 [effect]）。
 */
internal class LiquidGlassPipeline private constructor(
    private val shader: RuntimeShader?,
    private val blurSigma: Float
) {
    /** 没有着色器也没有模糊（API 31 以下）时，玻璃退化成纯色磨砂 */
    val enabled: Boolean get() = shader != null || blurSigma > 0.01f

    private var effect: ComposeRenderEffect? = null
    /** [effect] 是按哪一组 uniform 建出来的，null 表示还没建 */
    private var effectUniforms: Uniforms? = null

    fun effect(
        widthPx: Float,
        heightPx: Float,
        radiusPx: Float,
        densityScale: Float,
        style: LiquidGlassStyle
    ): ComposeRenderEffect? {
        if (!enabled) return null
        shader?.let { bindUniforms(it, widthPx, heightPx, radiusPx, densityScale, style) }

        // `RenderEffect` 在创建的那一刻就把 RuntimeShader 当时的 uniform 固化进了
        // 它内部的 GPU 程序，之后再调用 setFloatUniform 都不会影响它；而 Compose
        // 只在 renderEffect **对象换了** 的时候才重新 setRenderEffect。
        //
        // 两者叠在一起就是：只要 uniform 变了却不重建 effect，玻璃会一直用第一次
        // 创建时的那套颜色。最典型的就是切浅色 / 深色 —— tint / rim / sheen 整组
        // 都换掉了，玻璃却还是原来的明暗，只有杀掉进程重启（管线重新建一遍）才
        // 恢复正常。所以这里按 uniform 的取值缓存：取值一变就重建，交给
        // RenderNode 重新设置一次。
        val uniforms = Uniforms(widthPx, heightPx, radiusPx, densityScale, style)
        if (effect == null || uniforms != effectUniforms) {
            effect = createEffect()
            effectUniforms = uniforms
        }
        return effect
    }

    private fun createEffect(): ComposeRenderEffect? = runCatching {
        val blur = if (blurSigma > 0.01f) {
            RenderEffect.createBlurEffect(blurSigma, blurSigma, Shader.TileMode.CLAMP)
        } else {
            null
        }
        val android = when {
            shader != null && blur != null -> RenderEffect.createChainEffect(
                /* outer = */ RenderEffect.createRuntimeShaderEffect(shader, "content"),
                /* inner = */ blur
            )

            shader != null -> RenderEffect.createRuntimeShaderEffect(shader, "content")
            else -> blur
        } ?: return null
        android.asComposeRenderEffect()
    }.getOrNull()

    companion object {
        /** 玻璃的模糊很轻：厚度感来自折射，糊过头就成磨砂贴膜了 */
        private const val DEFAULT_BLUR_SIGMA = 1.2f

        fun create(shaderCode: String?): LiquidGlassPipeline {
            // API 31 才有 RenderEffect；31–32 没有 RuntimeShader，只有模糊可退
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return LiquidGlassPipeline(shader = null, blurSigma = 0f)
            }
            val shader = if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderCode != null
            ) {
                runCatching { RuntimeShader(shaderCode) }.getOrNull()
            } else {
                null
            }
            return LiquidGlassPipeline(shader, DEFAULT_BLUR_SIGMA)
        }
    }
}

/**
 * 一份 [LiquidGlassPipeline.effect] 的缓存键。
 *
 * 列的必须是**全部会写进着色器的 uniform**：尺寸、圆角、密度换算和 [LiquidGlassStyle]
 * 本身。少列一个，那个值就会永远停在第一次绘制时的样子。
 */
private data class Uniforms(
    val widthPx: Float,
    val heightPx: Float,
    val radiusPx: Float,
    val densityScale: Float,
    val style: LiquidGlassStyle
)

@Composable
private fun rememberLiquidGlassPipeline(): LiquidGlassPipeline {
    val context = LocalContext.current
    return remember(context) {
        LiquidGlassPipeline.create(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readShaderSource(context)
        } else {
            null
        })
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun readShaderSource(context: Context): String? = runCatching {
    context.resources.openRawResource(R.raw.liquidglass_effect)
        .bufferedReader()
        .use { it.readText() }
}.getOrNull()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun bindUniforms(
    shader: RuntimeShader,
    widthPx: Float,
    heightPx: Float,
    radiusPx: Float,
    densityScale: Float,
    style: LiquidGlassStyle
) {
    shader.setFloatUniform("size", widthPx, heightPx)
    shader.setFloatUniform("offset", 0f, 0f)
    shader.setFloatUniform("cornerRadii", radiusPx, radiusPx, radiusPx, radiusPx)
    shader.setFloatUniform("refractionHeight", style.refractionHeight.value * densityScale)
    shader.setFloatUniform("refractionAmount", style.refractionAmount.value * densityScale)
    shader.setFloatUniform("depthEffect", style.depth)
    shader.setFloatUniform("chromaticAberration", style.dispersion)
    shader.setFloatUniform("contrast", style.contrast)
    shader.setFloatUniform("whitePoint", style.whitePoint)
    shader.setFloatUniform("chromaMultiplier", style.chroma)
    shader.setFloatUniform("tintColor", style.tint.red, style.tint.green, style.tint.blue)
    shader.setFloatUniform("tintAlpha", style.tint.alpha)
}
