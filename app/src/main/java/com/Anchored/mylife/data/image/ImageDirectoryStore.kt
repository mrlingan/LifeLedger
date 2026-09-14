package com.Anchored.mylife.data.image

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

/**
 * 应用私有目录里的一份图片副本。
 *
 * 相册给的是 content:// 读取凭证：用户清空相册、卸载那个图库、或者换台手机，
 * 凭证就失效了，界面上会只剩一个空框。所以「用户自己挑的图」一律复制一份进私有目录，
 * 库里只记绝对路径。
 *
 * 头像和自定义成就图标各用一个目录（[directoryName] 不同）：备份里它们的路径不一样，
 * 清空数据时也能各清各的，不会互相牵连。
 *
 * @param filePrefix 生成的文件名前缀，便于在私有目录里一眼看出这张图是干什么用的
 */
open class ImageDirectoryStore(
    private val context: Context,
    private val directoryName: String,
    private val filePrefix: String
) {

    val directory: File
        get() = File(context.filesDir, directoryName).apply { if (!exists()) mkdirs() }

    /** 换一张：先写新文件，成功了再删旧的，中途失败也不会把现有的图弄丢 */
    fun replace(uri: Uri, previousPath: String?): String {
        val destination = File(directory, buildFileName(uri))
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选图片：$uri")
        input.use { source ->
            destination.outputStream().use { target -> source.copyTo(target) }
        }
        delete(previousPath?.takeIf { it != destination.absolutePath })
        return destination.absolutePath
    }

    /**
     * 直接写一张现成的图（演示数据用），返回绝对路径。
     *
     * 和 [replace] 一样：先写新文件，成功了再删旧的。
     */
    fun save(bitmap: Bitmap, previousPath: String?, quality: Int = 92): String {
        val destination = File(directory, "${filePrefix}_${System.currentTimeMillis()}.jpg")
        destination.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        delete(previousPath?.takeIf { it != destination.absolutePath })
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

    /** 清空整个目录（覆盖式恢复备份、清空数据之前调用） */
    fun clear() {
        directory.listFiles()?.forEach { it.delete() }
    }

    private fun buildFileName(uri: Uri): String {
        val extension = runCatching {
            context.contentResolver.getType(uri)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.let { ".$it" }
        }.getOrNull() ?: ".jpg"
        return "${filePrefix}_${System.currentTimeMillis()}$extension"
    }
}
