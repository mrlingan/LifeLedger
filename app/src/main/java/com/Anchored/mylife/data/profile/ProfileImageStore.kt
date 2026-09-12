package com.Anchored.mylife.data.profile

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * 头像文件的存放处（files/profile/）。
 *
 * 和媒体一样必须复制一份进私有目录：相册给的是 content:// 读取凭证，
 * 用户清空相册或者换机之后就失效了，头像会变成空白。
 *
 * 单独用一个目录，是为了让头像和「成就下的媒体」各管各的：
 * 备份里的路径不一样，清空数据时也不会互相牵连。
 */
class ProfileImageStore(private val context: Context) {

    val directory: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }

    /** 换头像：先写新文件，成功了再删旧的，中途失败也不会把现有头像弄丢 */
    fun replace(uri: Uri, previousPath: String?): String {
        val destination = File(directory, buildFileName(uri))
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选图片：$uri")
        input.use { source ->
            destination.outputStream().use { target -> source.copyTo(target) }
        }
        if (previousPath != null && previousPath != destination.absolutePath) {
            delete(previousPath)
        }
        return destination.absolutePath
    }

    /** 只删这个目录里的文件，别的路径一律跳过，避免误删 */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists() && file.parentFile?.canonicalPath == directory.canonicalPath) {
                file.delete()
            }
        }
    }

    /** 清空整个头像目录（覆盖式恢复备份之前调用） */
    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun buildFileName(uri: Uri): String {
        val extension = runCatching {
            context.contentResolver.getType(uri)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.let { ".$it" }
        }.getOrNull() ?: ".jpg"
        return "avatar_${System.currentTimeMillis()}$extension"
    }

    companion object {
        const val DIRECTORY_NAME = "profile"
    }
}
