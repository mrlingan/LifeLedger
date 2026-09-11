package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(items: List<Note>): List<Long>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE achievementId = :achievementId ORDER BY createdDate DESC")
    suspend fun getNotesByAchievementId(achievementId: Long): List<Note>

    /** 详情页用：某条成就下的笔记列表，增删改都会自动刷新 */
    @Query("SELECT * FROM notes WHERE achievementId = :achievementId ORDER BY createdDate DESC")
    fun observeNotesByAchievementId(achievementId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY createdDate ASC")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun observeNoteById(noteId: Long): Flow<Note?>

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun countNotes(): Int

    @Query("DELETE FROM notes WHERE achievementId = :achievementId")
    suspend fun deleteNotesByAchievementId(achievementId: Long)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}
