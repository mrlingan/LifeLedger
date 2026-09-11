package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.Media
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: Media): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<Media>)

    @Update
    suspend fun updateMedia(media: Media)

    @Delete
    suspend fun deleteMedia(media: Media)

    @Query("SELECT * FROM media WHERE noteId = :noteId ORDER BY createdDate ASC")
    suspend fun getMediaByNoteId(noteId: Long): List<Media>

    /** 笔记卡片用：某条笔记下的媒体，增删自动刷新 */
    @Query("SELECT * FROM media WHERE noteId = :noteId ORDER BY createdDate ASC")
    fun observeMediaByNoteId(noteId: Long): Flow<List<Media>>

    /** 详情页用：一条成就下所有笔记的媒体，一次取全 */
    @Query(
        """
        SELECT media.* FROM media
        INNER JOIN notes ON notes.id = media.noteId
        WHERE notes.achievementId = :achievementId
        ORDER BY media.createdDate ASC
        """
    )
    fun observeMediaByAchievementId(achievementId: Long): Flow<List<Media>>

    @Query("SELECT * FROM media ORDER BY createdDate ASC")
    suspend fun getAllMedia(): List<Media>

    @Query("SELECT * FROM media WHERE id = :mediaId LIMIT 1")
    suspend fun getMediaById(mediaId: Long): Media?

    @Query("SELECT COUNT(*) FROM media")
    suspend fun countMedia(): Int

    @Query("DELETE FROM media WHERE noteId = :noteId")
    suspend fun deleteMediaByNoteId(noteId: Long)

    @Query("DELETE FROM media WHERE id = :mediaId")
    suspend fun deleteMediaById(mediaId: Long)

    @Query("DELETE FROM media")
    suspend fun deleteAllMedia()
}
