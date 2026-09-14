package com.Anchored.mylife.data.repository

import com.Anchored.mylife.data.dao.MediaDao
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.database.AchievementMedia
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * 媒体仓库：封装 MediaDao，一条媒体属于一条笔记。
 *
 * 存储约定：filePath 保存应用私有目录中的绝对路径（由 MediaFileStore 复制而来），
 * 不要直接保存相册的 content:// Uri，否则用户删除原图后 App 里会变成空白。
 */
class MediaRepository(
    private val mediaDao: MediaDao
) {

    /** 媒体类型常量，写入 Media.fileType 时使用。 */
    object FileType {
        const val IMAGE = "image"
        const val VIDEO = "video"

        /** 实况照片：filePath 是静态图，motionVideoPath 是同目录下抽出来的那段视频 */
        const val LIVE_PHOTO = "live_photo"
    }

    // ---------- 查询 ----------

    suspend fun getMediaByNoteId(noteId: Long): List<Media> =
        mediaDao.getMediaByNoteId(noteId)

    fun observeMediaByNoteId(noteId: Long): Flow<List<Media>> =
        mediaDao.observeMediaByNoteId(noteId)

    fun observeMediaByAchievementId(achievementId: Long): Flow<List<Media>> =
        mediaDao.observeMediaByAchievementId(achievementId)

    /** 首页最近解锁卡片的封面图，按时间正序（每条成就在界面层取第一张） */
    fun observeAchievementImages(): Flow<List<AchievementMedia>> =
        mediaDao.observeAchievementImages()

    suspend fun getMediaById(mediaId: Long): Media? =
        mediaDao.getMediaById(mediaId)

    /** 图片 / 视频总数（响应式）：「我的」页显示数据量用 */
    fun observeCount(): Flow<Int> =
        mediaDao.observeMediaCount()

    // ---------- 写入 ----------

    suspend fun insertMedia(media: Media): Long =
        mediaDao.insertMedia(media)

    suspend fun insertMediaList(mediaList: List<Media>) =
        mediaDao.insertMediaList(mediaList)

    suspend fun addMedia(
        noteId: Long,
        filePath: String,
        fileType: String = FileType.IMAGE,
        motionVideoPath: String? = null,
        createdDate: Long = System.currentTimeMillis()
    ): Long = mediaDao.insertMedia(
        Media(
            noteId = noteId,
            filePath = filePath,
            fileType = fileType,
            createdDate = createdDate,
            motionVideoPath = motionVideoPath
        )
    )

    suspend fun addMediaList(
        noteId: Long,
        files: List<Triple<String, String, String?>>,
        createdDate: Long = System.currentTimeMillis()
    ) {
        if (files.isEmpty()) return
        mediaDao.insertMediaList(
            files.map { (path, type, motionPath) ->
                Media(
                    noteId = noteId,
                    filePath = path,
                    fileType = type,
                    createdDate = createdDate,
                    motionVideoPath = motionPath
                )
            }
        )
    }

    suspend fun updateMedia(media: Media) =
        mediaDao.updateMedia(media)

    // ---------- 删除 ----------

    /** 只删数据库记录，磁盘文件保留 */
    suspend fun deleteMedia(media: Media) =
        mediaDao.deleteMedia(media)

    suspend fun deleteMediaById(mediaId: Long) =
        mediaDao.deleteMediaById(mediaId)

    suspend fun deleteMediaByNoteId(noteId: Long) =
        mediaDao.deleteMediaByNoteId(noteId)

    /** 删记录的同时删磁盘文件（含实况照片抽出来的视频） */
    suspend fun deleteMediaWithFiles(media: Media) {
        mediaDao.deleteMedia(media)
        deleteFileQuietly(media.filePath)
        deleteFileQuietly(media.motionVideoPath)
    }

    /** 批量删记录并清理磁盘文件 */
    suspend fun deleteMediaListWithFiles(mediaList: List<Media>) {
        mediaList.forEach { media ->
            mediaDao.deleteMedia(media)
            deleteFileQuietly(media.filePath)
            deleteFileQuietly(media.motionVideoPath)
        }
    }

    /** 根据 MIME 类型或文件名推断媒体类型，供插入前判断 */
    fun resolveFileType(mimeType: String?, fileName: String? = null): String {
        val mime = mimeType?.lowercase().orEmpty()
        val name = fileName?.lowercase().orEmpty()
        return when {
            mime.startsWith("video/") -> FileType.VIDEO
            mime.startsWith("image/") -> FileType.IMAGE
            VIDEO_EXTENSIONS.any { name.endsWith(it) } -> FileType.VIDEO
            else -> FileType.IMAGE
        }
    }

    private fun deleteFileQuietly(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        runCatching {
            val file = File(filePath)
            if (file.exists()) file.delete()
        }
    }

    private companion object {
        val VIDEO_EXTENSIONS = listOf(".mp4", ".mov", ".mkv", ".webm", ".3gp", ".avi")
    }
}
