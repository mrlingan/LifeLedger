package com.Anchored.mylife.data.background

import android.content.Context
import com.Anchored.mylife.data.image.ImageDirectoryStore

/**
 * 全局背景图的私有副本（files/background/）。
 *
 * 和头像、自定义图标、首页配图一样：相册给的是临时读取凭证——重启手机、清空相册、
 * 卸载那个图库之后都会失效——所以挑完就复制一份进来，偏好里只记绝对路径。
 */
class BackgroundImageStore(context: Context) : ImageDirectoryStore(
    context = context,
    directoryName = DIRECTORY_NAME,
    filePrefix = "background"
) {
    companion object {
        const val DIRECTORY_NAME = "background"
    }
}
