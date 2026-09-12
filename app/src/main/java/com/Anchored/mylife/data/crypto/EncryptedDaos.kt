package com.Anchored.mylife.data.crypto

import com.Anchored.mylife.data.dao.AchievementDao
import com.Anchored.mylife.data.dao.NoteDao
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 在 DAO 上套一层加解密。
 *
 * 放在这一层的原因：仓库、备份、迁移都从这里过，
 * 只要装饰一层，所有读路径自动解密、所有写路径按开关决定加不加密，
 * 上层业务代码一行都不用改，也不会漏掉某个新增的调用点。
 *
 * [encryptionEnabled] 是一段读开关的代码（不是快照值）：
 * 用户在设置里一开一关，不用重建 Repository 就立刻生效。
 * 读路径故意不看它——关掉开关之后，已经加密的行仍然要能读出来。
 */
class EncryptedAchievementDao(
    private val delegate: AchievementDao,
    private val encryptionEnabled: () -> Boolean
) : AchievementDao by delegate {

    override suspend fun insertAchievement(achievement: Achievement): Long =
        delegate.insertAchievement(achievement.encrypted(encryptionEnabled()))

    override suspend fun insertAchievements(items: List<Achievement>): List<Long> =
        delegate.insertAchievements(items.map { it.encrypted(encryptionEnabled()) })

    override suspend fun updateAchievement(achievement: Achievement) =
        delegate.updateAchievement(achievement.encrypted(encryptionEnabled()))

    override suspend fun getAllAchievements(): List<Achievement> =
        delegate.getAllAchievements().map { it.decrypted() }

    override fun observeAllAchievements(): Flow<List<Achievement>> =
        delegate.observeAllAchievements().map { list -> list.map { it.decrypted() } }

    override suspend fun getAchievementById(achievementId: Long): Achievement? =
        delegate.getAchievementById(achievementId)?.decrypted()

    override fun observeAchievementById(achievementId: Long): Flow<Achievement?> =
        delegate.observeAchievementById(achievementId).map { it?.decrypted() }

    override suspend fun getAchievementsByCompletionStatus(isCompleted: Boolean): List<Achievement> =
        delegate.getAchievementsByCompletionStatus(isCompleted).map { it.decrypted() }
}

class EncryptedNoteDao(
    private val delegate: NoteDao,
    private val encryptionEnabled: () -> Boolean
) : NoteDao by delegate {

    override suspend fun insertNote(note: Note): Long =
        delegate.insertNote(note.encrypted(encryptionEnabled()))

    override suspend fun insertNotes(items: List<Note>): List<Long> =
        delegate.insertNotes(items.map { it.encrypted(encryptionEnabled()) })

    override suspend fun updateNote(note: Note) =
        delegate.updateNote(note.encrypted(encryptionEnabled()))

    override suspend fun getNotesByAchievementId(achievementId: Long): List<Note> =
        delegate.getNotesByAchievementId(achievementId).map { it.decrypted() }

    override fun observeNotesByAchievementId(achievementId: Long): Flow<List<Note>> =
        delegate.observeNotesByAchievementId(achievementId)
            .map { list -> list.map { it.decrypted() } }

    override suspend fun getAllNotes(): List<Note> =
        delegate.getAllNotes().map { it.decrypted() }

    override suspend fun getNoteById(noteId: Long): Note? =
        delegate.getNoteById(noteId)?.decrypted()

    override fun observeNoteById(noteId: Long): Flow<Note?> =
        delegate.observeNoteById(noteId).map { it?.decrypted() }
}
