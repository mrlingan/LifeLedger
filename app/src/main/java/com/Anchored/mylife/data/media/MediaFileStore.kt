package com.Anchored.mylife.data.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/**
 * 媒体文件仓库：负责把相册里选中的内容复制到应用私有目录。
 *
 * 为什么必须复制一份：
 * 相册返回的是 content:// 地址，它只是"读取凭证"，用户清空相册、卸载相册 App
 * 或者换机之后这个地址就失效了，App 里的图片会变成一片空白。
 */
class MediaFileStore(private val context: Context) {

    private val mediaDir: File
        get() = File(context.filesDir, DIRECTORY_NAME).apply { if (!exists()) mkdirs() }

    /** 复制进私有目录，返回副本的绝对路径 */
    fun copyToPrivateStorage(uri: Uri, fallbackExtension: String = ".jpg"): String {
        val destination = File(mediaDir, buildFileName(uri, fallbackExtension))
        val input = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件：$uri")
        input.use { source ->
            FileOutputStream(destination).use { target -> source.copyTo(target) }
        }
        return destination.absolutePath
    }

    /**
     * 写一张现成的图进私有目录，返回绝对路径。
     *
     * 相册之外还要有这条路，是因为演示数据里的配图是程序画出来的，
     * 没有 content:// 来源可复制。
     */
    fun saveBitmap(bitmap: Bitmap, quality: Int = 90): String {
        val destination = File(
            mediaDir,
            "media_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}.jpg"
        )
        FileOutputStream(destination).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return destination.absolutePath
    }

    /** 删除私有目录里的文件；路径不在私有目录内的一律跳过，避免误删 */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists() && file.parentFile?.canonicalPath == mediaDir.canonicalPath) {
                file.delete()
            }
        }
    }

    /**
     * 清空整个媒体目录。
     *
     * 只该在**数据库里的媒体行已经全部删掉**之后调用：
     * 这个方法不检查引用，会让没删掉的行指向不存在的文件。
     */
    fun clear() {
        mediaDir.listFiles()?.forEach { it.delete() }
    }

    private fun buildFileName(uri: Uri, fallbackExtension: String): String {
        val extension = mimeExtension(uri) ?: fallbackExtension
        return "media_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}$extension"
    }

    private fun mimeExtension(uri: Uri): String? = runCatching {
        val mimeType = context.contentResolver.getType(uri) ?: return@runCatching null
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { ".$it" }
    }.getOrNull()

    private companion object {
        const val DIRECTORY_NAME = "media"
    }
}
