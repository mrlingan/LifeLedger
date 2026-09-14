package com.Anchored.mylife.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * 首页的自定义图片：整张图，按它自己的比例显示。
 *
 * 尺寸由图片自己决定：宽度铺满卡片，高度按原图比例算出来——**不裁、不拉伸、不留白边**。
 * 位置（排在第几段）由用户在「设置 → 首页板块」里决定。
 *
 * 早先是"固定高度 + 上传时裁剪"，结果传上来的图总有一部分看不到；现在看到的就是
 * 传进来那张图的全部。**没有图就什么都不画**——开关开着也一样：一个空框比没有这一段更难看。
 */
@Composable
internal fun HomeImageSection(
    path: String?,
    modifier: Modifier = Modifier
) {
    if (path.isNullOrBlank()) return

    val thumbnail = rememberMediaThumbnail(path = path, isVideo = false, sizePx = 1200) ?: return
    val aspect = thumbnail.width.toFloat() / thumbnail.height.toFloat()
    if (aspect <= 0f) return

    AppCard(
        modifier = modifier.padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(Radius.lg)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
