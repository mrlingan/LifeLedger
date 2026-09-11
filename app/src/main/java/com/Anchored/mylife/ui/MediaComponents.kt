package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.repository.MediaRepository
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 笔记卡片里的媒体条：已有媒体 + 一个「＋」用来继续添加 */
@Composable
fun MediaStrip(
    mediaList: List<Media>,
    onMediaClick: (Media) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = AppTheme.colors

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items = mediaList, key = { it.id }) { media ->
            MediaThumbnailTile(media = media, onClick = { onMediaClick(media) })
        }

        item(key = "add_media_tile") {
            Box(
                modifier = Modifier
                    .size(Sizes.avatarLg)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(scheme.surfaceSunken)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.media_add_cd),
                    tint = scheme.textSecondary
                )
            }
        }
    }
}

@Composable
fun MediaThumbnailTile(
    media: Media,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = AppTheme.colors
    val isVideo = media.fileType == MediaRepository.FileType.VIDEO
    val thumbnail = rememberMediaThumbnail(
        path = media.filePath,
        isVideo = isVideo,
        sizePx = 240
    )

    Box(
        modifier = modifier
            .size(Sizes.avatarLg)
            .clip(RoundedCornerShape(Radius.md))
            .background(scheme.surfaceSunken)
            .clickable(onClick = onClick)
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "…",
                style = AppTheme.type.body,
                color = scheme.textSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (isVideo || media.fileType == MediaRepository.FileType.LIVE_PHOTO) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xs)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(Spacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(Sizes.iconSm)
                )
            }
        }

        if (media.fileType == MediaRepository.FileType.LIVE_PHOTO) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.xs)
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
            ) {
                Text(text = "LIVE", color = Color.White, fontSize = 8.sp)
            }
        }
    }
}

/** 笔记编辑框里待保存的媒体（还没入库，只是选中的 Uri） */
@Composable
fun PendingMediaRow(
    uris: List<Uri>,
    onRemove: (Uri) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = AppTheme.colors

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items = uris, key = { it.toString() }) { uri ->
            val thumbnail = rememberUriThumbnail(uri = uri, sizePx = 200)

            Box(modifier = Modifier.size(Sizes.avatarLg)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Radius.md))
                        .background(scheme.surfaceSunken)
                ) {
                    if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(Sizes.iconMd)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { onRemove(uri) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_remove),
                        tint = Color.White,
                        modifier = Modifier.size(Sizes.iconSm)
                    )
                }
            }
        }

        item(key = "pick_media_tile") {
            Box(
                modifier = Modifier
                    .size(Sizes.avatarLg)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(scheme.surfaceSunken)
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.media_pick_cd),
                    tint = scheme.textSecondary
                )
            }
        }
    }
}

/** 全屏查看：图片、视频、实况照片都在这里看 */
@Composable
fun MediaViewerDialog(
    media: Media,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var playMotion by remember(media.id) { mutableStateOf(false) }

    val isLivePhoto = media.fileType == MediaRepository.FileType.LIVE_PHOTO
    val hasMotion = isLivePhoto && !media.motionVideoPath.isNullOrBlank()
    val showVideo = media.fileType == MediaRepository.FileType.VIDEO || (hasMotion && playMotion)
    val videoPath = if (isLivePhoto) media.motionVideoPath else media.filePath
    val image = rememberMediaThumbnail(path = media.filePath, isVideo = false, sizePx = 1440)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (showVideo && !videoPath.isNullOrBlank()) {
                VideoPlayer(
                    path = videoPath,
                    // 实况照片只播一遍，播完自动回到静态照片；普通视频保持循环
                    loop = !isLivePhoto,
                    showControls = !isLivePhoto,
                    onCompleted = { playMotion = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            } else if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        // 长按播放实况，播一次就停
                        .pointerInput(media.id) {
                            detectTapGestures(
                                onLongPress = { if (hasMotion) playMotion = true }
                            )
                        }
                )
            } else {
                Text(
                    text = stringResource(R.string.media_cant_open),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color.White
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = Color.White
                    )
                }
            }

            if (hasMotion && !playMotion) {
                Text(
                    text = stringResource(R.string.media_live_hint),
                    style = AppTheme.type.caption,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Spacing.xxxl)
                )
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    path: String,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    showControls: Boolean = true,
    onCompleted: () -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                if (showControls) {
                    val controller = MediaController(context)
                    controller.setAnchorView(this)
                    setMediaController(controller)
                }
                setVideoPath(path)
                setOnPreparedListener { player ->
                    player.isLooping = loop
                    start()
                }
                setOnCompletionListener {
                    if (!loop) onCompleted()
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// 缩略图加载
// ---------------------------------------------------------------------------

@Composable
fun rememberMediaThumbnail(
    path: String,
    isVideo: Boolean,
    sizePx: Int
): ImageBitmap? {
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = null,
        path,
        isVideo,
        sizePx
    ) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(path, isVideo, sizePx) }
    }
    return thumbnail
}

@Composable
fun rememberUriThumbnail(
    uri: Uri,
    sizePx: Int
): ImageBitmap? {
    val context = LocalContext.current
    val thumbnail by produceState<ImageBitmap?>(
        initialValue = null,
        uri,
        sizePx
    ) {
        value = withContext(Dispatchers.IO) { decodeUriThumbnail(context, uri, sizePx) }
    }
    return thumbnail
}

private fun decodeThumbnail(path: String, isVideo: Boolean, sizePx: Int): ImageBitmap? {
    if (!File(path).exists()) return null
    val bitmap = if (isVideo) decodeVideoFrame(path) else decodeSampledFile(path, sizePx)
    return bitmap?.asImageBitmap()
}

private fun decodeSampledFile(path: String, sizePx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, sizePx)
    }
    BitmapFactory.decodeFile(path, options)
}.getOrNull()

private fun decodeVideoFrame(path: String): Bitmap? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(path)
        retriever.getFrameAtTime(0)
    } finally {
        retriever.release()
    }
}.getOrNull()

private fun decodeUriThumbnail(context: Context, uri: Uri, sizePx: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, sizePx)
    }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}.getOrNull()?.asImageBitmap()

private fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
    if (width <= 0 || height <= 0 || targetSize <= 0) return 1
    var sampleSize = 1
    while (width / (sampleSize * 2) >= targetSize && height / (sampleSize * 2) >= targetSize) {
        sampleSize *= 2
    }
    return sampleSize
}
