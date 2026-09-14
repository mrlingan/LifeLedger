package com.Anchored.mylife.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

/** 输出图的宽边上限：卡片本身很窄，存太大的图没有意义 */
private const val CROP_OUTPUT_MAX_WIDTH = 1440

/** 最多放大到"刚好铺满"的 4 倍，再多就只剩马赛克了 */
private const val CROP_MAX_ZOOM = 4f

/**
 * 裁剪图片。
 *
 * 一个固定的裁剪框 + 一张可以拖、可以双指缩放的图：**框不动，图动**。
 * 这样"裁剪"的语义就是"这张图在卡片里露出哪一块"，不会出现"框和图各动一半"
 * 那种要靠想象才能对齐的交互。
 *
 * 框的比例由调用方给定（首页那张卡片的比例），而且预览和最终存盘的像素
 * 用的是**同一套变换**（见 [CropTransform]）：看到什么样，存下来就是什么样。
 */
@Composable
fun ImageCropDialog(
    source: Uri,
    aspect: Float,
    onCropped: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AppTheme.colors

    // 原图按"最多 1600px"采样读进来：几千万像素的原图整张读进来会直接 OOM
    val sourceBitmap by produceState<Bitmap?>(initialValue = null, source) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeSampled(context, source, 1600) }.getOrNull()
        }
    }

    // 用户的手势：缩放倍数 + 平移（裁剪框坐标系，px）
    var zoom by remember(source) { mutableStateOf(1f) }
    var pan by remember(source) { mutableStateOf(Offset.Zero) }

    AppDialog(
        title = stringResource(R.string.image_crop_title),
        onDismissRequest = onDismiss,
        onConfirm = {
            val bitmap = sourceBitmap
            if (bitmap != null) {
                onCropped(cropToBitmap(bitmap, zoom, pan, aspect))
            }
        },
        confirmText = stringResource(R.string.image_crop_apply),
        dismissText = stringResource(R.string.common_cancel),
        confirmEnabled = sourceBitmap != null,
        content = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspect)
                        .clip(RoundedCornerShape(Radius.md))
                        .background(colors.surfaceSunken)
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                val next = (zoom * zoomChange).coerceIn(1f, CROP_MAX_ZOOM)
                                val transformed = CropTransform(
                                    frame = androidx.compose.ui.geometry.Size(
                                        size.width.toFloat(),
                                        size.height.toFloat()
                                    ),
                                    image = sourceBitmap?.let {
                                        androidx.compose.ui.geometry.Size(
                                            it.width.toFloat(),
                                            it.height.toFloat()
                                        )
                                    } ?: androidx.compose.ui.geometry.Size.Zero,
                                    zoom = next,
                                    pan = pan + panChange
                                )
                                zoom = next
                                pan = transformed.clampedPan
                            }
                        }
                ) {
                    val bitmap = sourceBitmap
                    if (bitmap != null) {
                        val image = remember(bitmap) { bitmap.asImageBitmap() }
                        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                            val transform = CropTransform(
                                frame = size,
                                image = androidx.compose.ui.geometry.Size(
                                    bitmap.width.toFloat(),
                                    bitmap.height.toFloat()
                                ),
                                zoom = zoom,
                                pan = pan
                            )
                            withTransform({
                                translate(
                                    left = size.width / 2f + transform.clampedPan.x,
                                    top = size.height / 2f + transform.clampedPan.y
                                )
                                scale(transform.totalScale, transform.totalScale, pivot = Offset.Zero)
                                translate(
                                    left = -bitmap.width / 2f,
                                    top = -bitmap.height / 2f
                                )
                            }) {
                                drawImage(image)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = stringResource(R.string.image_crop_hint),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    )
}

/**
 * 预览与输出共用的那套变换。
 *
 * `totalScale` 是"原图 → 裁剪框"的总倍数：先按 cover 铺满（不留白边），再乘用户缩放的倍数。
 * `clampedPan` 把平移夹在"图始终盖住框"的范围里——不然会拖出灰边。
 */
private data class CropTransform(
    val frame: androidx.compose.ui.geometry.Size,
    val image: androidx.compose.ui.geometry.Size,
    val zoom: Float,
    val pan: Offset
) {
    private val coverScale: Float
        get() = if (image.width <= 0f || image.height <= 0f) 1f else {
            max(frame.width / image.width, frame.height / image.height)
        }

    val totalScale: Float get() = coverScale * zoom

    val clampedPan: Offset
        get() {
            val drawnWidth = image.width * totalScale
            val drawnHeight = image.height * totalScale
            val maxX = ((drawnWidth - frame.width) / 2f).coerceAtLeast(0f)
            val maxY = ((drawnHeight - frame.height) / 2f).coerceAtLeast(0f)
            return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
        }
}

/**
 * 按同样一套变换把裁剪框里的内容画进一张新图。
 *
 * 只要变换一致，"预览里是什么样，存下来就是什么样"；输出尺寸按框的比例来，
 * 所以卡片放大到任何宽度都不会糊。
 */
private fun cropToBitmap(
    source: Bitmap,
    zoom: Float,
    pan: Offset,
    aspect: Float
): Bitmap {
    val outWidth = minOf(source.width, CROP_OUTPUT_MAX_WIDTH).coerceAtLeast(1)
    val outHeight = (outWidth / aspect).toInt().coerceAtLeast(1)
    val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)

    // 裁剪框 = 输出图，所以这里的 frame 直接用输出尺寸
    val frame = androidx.compose.ui.geometry.Size(outWidth.toFloat(), outHeight.toFloat())
    val image = androidx.compose.ui.geometry.Size(
        source.width.toFloat(),
        source.height.toFloat()
    )
    val transform = CropTransform(frame = frame, image = image, zoom = zoom, pan = pan)

    val canvas = Canvas(output)
    canvas.translate(frame.width / 2f + transform.clampedPan.x, frame.height / 2f + transform.clampedPan.y)
    canvas.scale(transform.totalScale, transform.totalScale)
    canvas.translate(-image.width / 2f, -image.height / 2f)
    canvas.drawBitmap(source, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
    return output
}

/** 采样解码：先只读尺寸，再按目标宽度算采样率 */
internal fun decodeSampled(context: android.content.Context, uri: Uri, maxSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    var sample = 1
    while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) {
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}
