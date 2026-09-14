package com.Anchored.mylife.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.rememberMediaThumbnail
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes

/**
 * 首页的自定义图片：一条窄卡片。
 *
 * 高度只有人生进度那张卡的一半不到（[Sizes.homeBanner]），位置由用户在
 * 「设置 → 首页板块」里决定。**没有图片就什么都不画**——开关开着也一样：
 * 一个空框比没有这一段更难看，用户也说不清自己看到的是什么。
 *
 * 图片是按卡片比例裁剪过的（见 ImageCropDialog），这里再 Crop 一次只是兜底：
 * 万一比例对不上，也是"裁掉一点"，不会被拉变形。
 */
@Composable
internal fun HomeImageSection(
    path: String?,
    modifier: Modifier = Modifier
) {
    if (path.isNullOrBlank()) return

    val thumbnail = rememberMediaThumbnail(path = path, isVideo = false, sizePx = 1200)

    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.homeBanner)
                .clip(RoundedCornerShape(Radius.lg)),
            contentAlignment = Alignment.Center
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
    }
}
