package com.Anchored.mylife.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.R
import com.Anchored.mylife.data.achievement.AchievementIcon
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.rememberUriThumbnail
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 成就图标：emoji 直接写字，用户上传的图片就画图。
 *
 * 两种图标在数据里是同一个字段（见 [AchievementIcon]），所以显示的地方只需要调这一个
 * 组件，"某个位置忘了支持图片"这种漏网不会出现。
 *
 * 图片按 [size] 采样解码——图标最大也就 96dp，原图可能有几千万像素，
 * 直接整张读进内存不值得。
 */
@Composable
fun AchievementIconView(
    icon: String,
    size: Dp,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = AppTheme.type.numberMedium,
    shape: Shape = RoundedCornerShape(Radius.md)
) {
    val path = AchievementIcon.customPath(icon)
    if (path == null) {
        // 普通 emoji：字怎么排由调用方那块地方决定，这里不额外套壳
        Text(text = icon, style = textStyle, modifier = modifier)
        return
    }

    val density = LocalDensity.current
    val thumbnail = rememberUriThumbnail(
        uri = Uri.fromFile(File(path)),
        sizePx = with(density) { size.toPx().toInt() }
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(AppTheme.colors.surfaceSunken),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 文件没了（被清理、从旧设备恢复了一半）：退回默认图标，不留一个空框
            Text(text = AchievementRepository.DEFAULT_ICON, style = textStyle)
        }
    }
}

/**
 * 图标选择器里的「上传」格子。
 *
 * 选了图之后**复制**进私有目录再记路径：相册给的是临时读取凭证，
 * 直接用它的 URI 的话，用户清空相册之后图标就空了。
 *
 * @param previousIcon 当前选中的图标；如果它本身就是一张自定义图，上传新图时旧文件会被删掉
 */
@Composable
fun IconUploadTile(
    previousIcon: String,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.avatarMd
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) {
        RepositoryProvider.get(context.applicationContext).iconImageStore
    }
    val shape = RoundedCornerShape(Radius.md)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    store.replace(uri, previousPath = AchievementIcon.customPath(previousIcon))
                }.getOrNull()
            }
            if (path != null) onPicked(AchievementIcon.custom(path))
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(colors.surfaceSunken)
            .border(Sizes.hairline, colors.border, shape)
            .clickable {
                picker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = stringResource(R.string.icon_upload),
            tint = colors.textSecondary,
            modifier = Modifier.size(Sizes.iconMd)
        )
    }
}
