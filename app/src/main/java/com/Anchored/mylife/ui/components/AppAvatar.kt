package com.Anchored.mylife.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import com.Anchored.mylife.ui.rememberMediaThumbnail
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes

/**
 * 圆形头像。
 *
 * 显示优先级：外部传入的预览图（刚选中还没保存的）→ 私有目录里的照片 → 昵称首字 → 人形图标。
 * 最后那个兜底是有必要的：新用户既没头像也没昵称，白框比图标更像"坏了"。
 */
@Composable
fun AppAvatar(
    name: String,
    modifier: Modifier = Modifier,
    path: String? = null,
    size: Dp = Sizes.avatarMd,
    imageOverride: ImageBitmap? = null
) {
    val colors = AppTheme.colors
    val bitmap = imageOverride
        ?: path?.let { rememberMediaThumbnail(path = it, isVideo = false, sizePx = (size.value * 3).toInt()) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceSunken)
            .border(Sizes.hairline, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            name.isNotBlank() -> Text(
                text = name.firstGlyph(),
                style = if (size >= Sizes.avatarLg) AppTheme.type.h2 else AppTheme.type.bodyLarge,
                color = colors.textSecondary
            )

            else -> Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

/**
 * 取第一个字形。
 *
 * 用 codePointAt 而不是 take(1)：名字以 emoji 开头时，take(1) 会把代理对截成
 * 半个字符，渲染出一个方块。
 */
internal fun String.firstGlyph(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    return String(Character.toChars(trimmed.codePointAt(0)))
}
