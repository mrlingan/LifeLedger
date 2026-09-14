package com.Anchored.mylife.data.profile

import android.content.Context
import com.Anchored.mylife.data.image.ImageDirectoryStore

/**
 * 头像文件的存放处（files/profile/）。
 *
 * 复制、替换、清理这套动作和自定义成就图标完全一样，所以放在
 * [ImageDirectoryStore] 里；这里只声明"头像用哪个目录、文件名长什么样"。
 */
class ProfileImageStore(context: Context) : ImageDirectoryStore(
    context = context,
    directoryName = DIRECTORY_NAME,
    filePrefix = "avatar"
) {
    companion object {
        const val DIRECTORY_NAME = "profile"
    }
}
