package com.Anchored.mylife.data.repository

import com.Anchored.mylife.data.dao.NoteDao
import com.Anchored.mylife.data.database.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao
) {

    suspend fun getNotesByAchievementId(achievementId: Long): List<Note> =
        noteDao.getNotesByAchievementId(achievementId)

    /** 某条成就下的笔记列表（响应式） */
    fun observeNotesByAchievementId(achievementId: Long): Flow<List<Note>> =
        noteDao.observeNotesByAchievementId(achievementId)

    fun observeNoteById(noteId: Long): Flow<Note?> =
        noteDao.observeNoteById(noteId)

    suspend fun getNoteById(noteId: Long): Note? =
        noteDao.getNoteById(noteId)

    /** 笔记总数（响应式）：「我的」页显示数据量用 */
    fun observeCount(): Flow<Int> =
        noteDao.observeNoteCount()

    suspend fun insertNote(note: Note): Long =
        noteDao.insertNote(note)

    suspend fun createNote(
        achievementId: Long,
        content: String = "",
        createdDate: Long = System.currentTimeMillis()
    ): Long = noteDao.insertNote(
        Note(
            achievementId = achievementId,
            content = content,
            createdDate = createdDate,
            updatedDate = createdDate
        )
    )

    suspend fun updateNote(note: Note) =
        noteDao.updateNote(note)

    /** 只改正文，同时刷新 updatedDate */
    suspend fun updateNoteContent(
        noteId: Long,
        content: String,
        updatedDate: Long = System.currentTimeMillis()
    ) {
        val existing = noteDao.getNoteById(noteId) ?: return
        noteDao.updateNote(existing.copy(content = content, updatedDate = updatedDate))
    }

    suspend fun deleteNote(note: Note) =
        noteDao.deleteNote(note)

    suspend fun deleteNoteById(noteId: Long) =
        noteDao.deleteNoteById(noteId)

    suspend fun deleteNotesByAchievementId(achievementId: Long) =
        noteDao.deleteNotesByAchievementId(achievementId)
}
