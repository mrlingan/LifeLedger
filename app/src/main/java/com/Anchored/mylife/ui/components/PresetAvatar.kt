package com.Anchored.mylife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.ui.theme.Sizes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 内置头像的画法。
 *
 * 为什么不用图片资源：一套头像要配 mdpi~xxxhdpi 五档，还都是一次性的图形。
 * 这里画的是"圆底 + 一个白色图案"，在任何尺寸下都清晰，也不用往仓库里塞二进制。
 *
 * 图案统一画在 100×100 的坐标系里，最后按控件边长整体缩放——
 * 这样每个图案的坐标可以直接照着草图写，不用一边写一边算 dp。
 */
private const val ARTWORK_UNIT = 100f

/** 图案统一用纯白：半透明的话，多块形状叠在一起（云、花）会在重叠处露出接缝 */
private val MARK_COLOR = Color.White

/** 一片叶子中间那道折痕：白色图案上压一层很淡的黑，比再画一条白线更像叶子 */
private val VEIN_COLOR = Color.Black.copy(alpha = 0.12f)

/** 一个内置头像长什么样：底色渐变 + 图案 */
private data class AvatarArtwork(
    val from: Color,
    val to: Color,
    val mark: AvatarMark
)

private enum class AvatarMark {
    SUN,
    WAVE,
    MOUNTAIN,
    LEAF,
    MOON,
    CLOUD,
    SPARKLE,
    PETAL
}

/**
 * 内置头像。
 *
 * 底色用的是和分类颜色同一套色板（见 CategoryColorChoices），所以一排头像
 * 和界面其他地方的彩色元素是同一个家族，不会突然跳出来一个荧光绿。
 */
@Composable
fun PresetAvatar(
    preset: AvatarPreset,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatarMd
) {
    val artwork = artworkOf(preset)

    Canvas(modifier = modifier.size(size)) {
        val unit = this.size.minDimension / ARTWORK_UNIT

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(artwork.from, artwork.to),
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height)
            )
        )

        drawAvatarMark(artwork, unit)
    }
}

private fun artworkOf(preset: AvatarPreset): AvatarArtwork = when (preset) {
    AvatarPreset.SUNRISE -> AvatarArtwork(Color(0xFFFFD60A), Color(0xFFFF9500), AvatarMark.SUN)
    AvatarPreset.WAVE -> AvatarArtwork(Color(0xFF32ADE6), Color(0xFF007AFF), AvatarMark.WAVE)
    AvatarPreset.MOUNTAIN -> AvatarArtwork(Color(0xFF64D2A6), Color(0xFF0B7D6B), AvatarMark.MOUNTAIN)
    AvatarPreset.LEAF -> AvatarArtwork(Color(0xFFA8E063), Color(0xFF2F9E44), AvatarMark.LEAF)
    AvatarPreset.MOON -> AvatarArtwork(Color(0xFF7B7BE8), Color(0xFF3B2F86), AvatarMark.MOON)
    AvatarPreset.CLOUD -> AvatarArtwork(Color(0xFFB8C6FF), Color(0xFF7E8CE0), AvatarMark.CLOUD)
    AvatarPreset.SPARKLE -> AvatarArtwork(Color(0xFFFF6FA5), Color(0xFFAF52DE), AvatarMark.SPARKLE)
    AvatarPreset.PETAL -> AvatarArtwork(Color(0xFFFF8FA3), Color(0xFFD6336C), AvatarMark.PETAL)
}

private fun DrawScope.drawAvatarMark(artwork: AvatarArtwork, unit: Float) {
    when (artwork.mark) {
        // 地平线上一轮太阳
        AvatarMark.SUN -> {
            drawCircle(
                color = MARK_COLOR,
                radius = 16f * unit,
                center = Offset(50f, 44f) * unit
            )
            drawLine(
                color = MARK_COLOR,
                start = Offset(26f, 68f) * unit,
                end = Offset(74f, 68f) * unit,
                strokeWidth = 6f * unit,
                cap = StrokeCap.Round
            )
        }

        // 两道浪：第二道顺着第一道的形状往下挪一格
        AvatarMark.WAVE -> {
            listOf(0f, 15f).forEach { offset ->
                val wave = Path().apply {
                    moveTo(22f * unit, (54f + offset) * unit)
                    quadraticTo(
                        36f * unit,
                        (40f + offset) * unit,
                        50f * unit,
                        (54f + offset) * unit
                    )
                    quadraticTo(
                        64f * unit,
                        (68f + offset) * unit,
                        78f * unit,
                        (54f + offset) * unit
                    )
                }
                drawPath(
                    path = wave,
                    color = MARK_COLOR,
                    style = Stroke(width = 6.5f * unit, cap = StrokeCap.Round)
                )
            }
        }

        // 两座山峰，右上角一个小太阳
        AvatarMark.MOUNTAIN -> {
            val peaks = Path().apply {
                moveTo(20f * unit, 72f * unit)
                lineTo(43f * unit, 36f * unit)
                lineTo(56f * unit, 54f * unit)
                lineTo(64f * unit, 42f * unit)
                lineTo(82f * unit, 72f * unit)
                close()
            }
            drawPath(path = peaks, color = MARK_COLOR)
            drawCircle(
                color = MARK_COLOR,
                radius = 6f * unit,
                center = Offset(74f, 28f) * unit
            )
        }

        // 一片叶子 + 中间的叶脉
        AvatarMark.LEAF -> {
            val leaf = Path().apply {
                moveTo(50f * unit, 24f * unit)
                cubicTo(
                    74f * unit,
                    40f * unit,
                    72f * unit,
                    64f * unit,
                    50f * unit,
                    78f * unit
                )
                cubicTo(
                    28f * unit,
                    64f * unit,
                    26f * unit,
                    40f * unit,
                    50f * unit,
                    24f * unit
                )
                close()
            }
            drawPath(path = leaf, color = MARK_COLOR)
            drawLine(
                color = VEIN_COLOR,
                start = Offset(50f, 34f) * unit,
                end = Offset(50f, 72f) * unit,
                strokeWidth = 4f * unit,
                cap = StrokeCap.Round
            )
        }

        // 月牙：一个大圆减掉一个错开的小圆
        AvatarMark.MOON -> {
            val disc = Path().apply {
                addOval(Rect(center = Offset(52f, 50f) * unit, radius = 26f * unit))
            }
            val bite = Path().apply {
                addOval(Rect(center = Offset(64f, 40f) * unit, radius = 24f * unit))
            }
            val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
            drawPath(path = crescent, color = MARK_COLOR)
        }

        // 三团云拼一起，中间那条底边把它们连成一块
        AvatarMark.CLOUD -> {
            val cloud = Path().apply {
                addOval(Rect(center = Offset(38f, 54f) * unit, radius = 14f * unit))
                addOval(Rect(center = Offset(56f, 46f) * unit, radius = 17f * unit))
                addOval(Rect(center = Offset(70f, 56f) * unit, radius = 12f * unit))
                addRoundRect(
                    RoundRect(
                        rect = Rect(Offset(30f, 52f) * unit, Size(42f * unit, 18f * unit)),
                        cornerRadius = CornerRadius(9f * unit)
                    )
                )
            }
            drawPath(path = cloud, color = MARK_COLOR)
        }

        // 一颗四角星 + 一颗小星
        AvatarMark.SPARKLE -> {
            val stars = Path().apply {
                moveTo(50f * unit, 17f * unit)
                quadraticTo(56f * unit, 45f * unit, 83f * unit, 50f * unit)
                quadraticTo(56f * unit, 55f * unit, 50f * unit, 83f * unit)
                quadraticTo(44f * unit, 55f * unit, 17f * unit, 50f * unit)
                quadraticTo(44f * unit, 45f * unit, 50f * unit, 17f * unit)
                close()

                moveTo(76f * unit, 64f * unit)
                quadraticTo(78f * unit, 72f * unit, 84f * unit, 74f * unit)
                quadraticTo(78f * unit, 76f * unit, 76f * unit, 84f * unit)
                quadraticTo(74f * unit, 76f * unit, 68f * unit, 74f * unit)
                quadraticTo(74f * unit, 72f * unit, 76f * unit, 64f * unit)
                close()
            }
            drawPath(path = stars, color = MARK_COLOR)
        }

        // 五瓣花：花瓣围一圈，花心用底色，白色花瓣中间才不是一坨白
        AvatarMark.PETAL -> {
            val petals = Path().apply {
                repeat(5) { index ->
                    val angle = (-90f + index * 72f) * PI.toFloat() / 180f
                    addOval(
                        Rect(
                            center = Offset(
                                50f + 17f * cos(angle),
                                50f + 17f * sin(angle)
                            ) * unit,
                            radius = 11f * unit
                        )
                    )
                }
            }
            drawPath(path = petals, color = MARK_COLOR)
            drawCircle(
                color = artwork.from,
                radius = 6.5f * unit,
                center = Offset(50f, 50f) * unit
            )
        }
    }
}
