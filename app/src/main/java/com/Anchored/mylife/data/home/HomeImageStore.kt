package com.Anchored.mylife.data.home

import android.content.Context
import com.Anchored.mylife.data.image.ImageDirectoryStore

/**
 * 首页那张自定义图片的副本目录（files/home/）。
 *
 * 和头像、成就图标一样：相册给的是临时读取凭证，必须复制一份进私有目录，
 * 库里（这里是偏好）只记绝对路径。三个目录分开，备份里路径不同，清空时也各清各的。
 */
class HomeImageStore(context: Context) : ImageDirectoryStore(
    context = context,
    directoryName = DIRECTORY_NAME,
    filePrefix = "home"
) {
    companion object {
        const val DIRECTORY_NAME = "home"
    }
}
