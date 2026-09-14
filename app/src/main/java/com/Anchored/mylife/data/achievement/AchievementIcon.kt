package com.Anchored.mylife.data.achievement

import android.content.Context
import com.Anchored.mylife.data.image.ImageDirectoryStore

/**
 * 成就图标：要么是一个 emoji 字符，要么是用户上传的图片。
 *
 * 两种图标共用数据库里那一个 `iconEmoji` 字段：图片记成 `file:<绝对路径>`。
 * 这样做不用改表、不用写迁移——老数据是纯 emoji，天然不会以这个前缀开头。
 */
object AchievementIcon {

    private const val CUSTOM_PREFIX = "file:"

    /** 把一张已经复制进私有目录的图片包装成图标值 */
    fun custom(path: String): String = "$CUSTOM_PREFIX$path"

    /** 是自定义图片就返回它的绝对路径，emoji 返回 null */
    fun customPath(icon: String): String? =
        if (icon.startsWith(CUSTOM_PREFIX)) icon.removePrefix(CUSTOM_PREFIX) else null

    fun isCustom(icon: String): Boolean = icon.startsWith(CUSTOM_PREFIX)
}

/**
 * 自定义成就图标的副本目录（files/icons/）。
 *
 * 和头像分开：备份里它们的路径不一样，清空数据时也各清各的。
 */
class IconImageStore(context: Context) : ImageDirectoryStore(
    context = context,
    directoryName = DIRECTORY_NAME,
    filePrefix = "icon"
) {
    companion object {
        const val DIRECTORY_NAME = "icons"
    }
}
