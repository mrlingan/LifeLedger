package com.Anchored.mylife.data.media

import java.io.File

/**
 * 实况照片（Motion Photo）解析。
 *
 * 原理：实况照片本质上是一张 JPEG，文件末尾直接拼了一段 MP4。
 * XMP 元数据里记录了这段视频的字节长度：
 * - 老格式（Pixel / 三星常见）：GCamera:MicroVideoOffset="123456"
 * - 新格式（Android 13+）：Container:Item 元素里带 Semantic="MotionPhoto" 与 Length="123456"
 *
 * 拿到长度后，从文件末尾往前切这么多字节就是视频本体。
 */
object MotionPhotoExtractor {

    private const val MAX_FILE_SIZE = 80L * 1024 * 1024
    private const val XMP_START = "<x:xmpmeta"
    private const val XMP_END = "</x:xmpmeta>"

    private val microVideoOffset = Regex("GCamera:MicroVideoOffset=\"(\\d+)\"")
    private val containerItem = Regex("<Container:Item[^>]*/>")
    private val motionPhotoSemantic = Regex("Semantic=\"MotionPhoto\"")
    private val lengthAttribute = Regex("Length=\"(\\d+)\"")

    /**
     * 从实况照片里抽出内嵌视频，存成同目录下的 `<原名>_motion.mp4`。
     * 不是实况照片、或者解析失败，都返回 null（调用方按普通图片处理即可）。
     */
    fun extractVideo(imageFile: File): File? = runCatching {
        if (!imageFile.isFile) return null
        val size = imageFile.length()
        if (size <= 0L || size > MAX_FILE_SIZE) return null

        val lowerName = imageFile.name.lowercase()
        if (!lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) return null

        val bytes = imageFile.readBytes()
        val videoLength = findEmbeddedVideoLength(bytes) ?: return null
        if (videoLength <= 0 || videoLength >= bytes.size) return null

        val videoBytes = bytes.copyOfRange(bytes.size - videoLength, bytes.size)
        if (!looksLikeMp4(videoBytes)) return null

        val videoFile = File(imageFile.parentFile, imageFile.nameWithoutExtension + "_motion.mp4")
        videoFile.writeBytes(videoBytes)
        videoFile
    }.getOrNull()

    private fun findEmbeddedVideoLength(bytes: ByteArray): Int? {
        val text = String(bytes, Charsets.ISO_8859_1)
        val start = text.indexOf(XMP_START)
        val end = if (start >= 0) text.indexOf(XMP_END, start) else -1
        val xmp = if (start >= 0 && end > start) text.substring(start, end) else text

        microVideoOffset.find(xmp)?.let { match ->
            return match.groupValues[1].toIntOrNull()
        }

        containerItem.findAll(xmp).forEach { item ->
            if (motionPhotoSemantic.containsMatchIn(item.value)) {
                lengthAttribute.find(item.value)?.let { match ->
                    return match.groupValues[1].toIntOrNull()
                }
            }
        }

        return null
    }

    private fun looksLikeMp4(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return bytes[4] == 'f'.code.toByte() &&
            bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() &&
            bytes[7] == 'p'.code.toByte()
    }
}
