package com.Anchored.mylife.data.crypto

import com.Anchored.mylife.data.dao.AchievementDao
import com.Anchored.mylife.data.dao.NoteDao

/**
 * 库里数据加密情况的统计，用于界面上的状态说明。
 *
 * [pending] 是还需要改写成密文的行数，[encrypted] 是已经有密文的行数；
 * 两边都不含字段全空的行（那些行没有可加密的内容）。
 */
data class EncryptionScan(val pending: Int, val encrypted: Int)

/** 一轮迁移的结果：这次真正改写了几行，以及失败了几行 */
data class EncryptionMigrationResult(val encrypted: Int, val failed: Int)

/**
 * 把库里已经写下的明文改写成密文。
 *
 * 只在用户打开「加密本地数据」开关的那一刻跑，不再是开机无条件执行：
 * - 逐行读写：读一行、写一行，中途退出不会留下半个损坏的库；
 * - 已经是密文的行直接跳过，所以重试是幂等的，被打断后接着跑也不会重复劳动；
 * - 单行失败不中断整轮，最后把剩下的条数交给界面，让用户决定要不要重试。
 *
 * 这里刻意直接用 Room 的原始 DAO（读出来就是落盘的原文），而不是带加解密的装饰器：
 * 迁移要看的正是"落盘到底长什么样"。
 */
class DataEncryptionMigration(
    private val achievementDao: AchievementDao,
    private val noteDao: NoteDao
) {

    suspend fun scan(): EncryptionScan {
        val achievements = achievementDao.getAllAchievements()
        val notes = noteDao.getAllNotes()

        val pending = achievements.count { it.needsReencoding() } + notes.count { it.needsReencoding() }
        val withContent = achievements.count { it.hasContent() } + notes.count { it.hasContent() }
        return EncryptionScan(pending = pending, encrypted = withContent - pending)
    }

    suspend fun run(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): EncryptionMigrationResult {
        val achievements = achievementDao.getAllAchievements()
        val notes = noteDao.getAllNotes()
        val total = achievements.size + notes.size

        var done = 0
        var encrypted = 0
        var failed = 0
        onProgress(done, total)

        for (achievement in achievements) {
            if (achievement.needsReencoding()) {
                val ok = runCatching {
                    achievementDao.updateAchievement(achievement.reencoded())
                }.isSuccess
                if (ok) encrypted++ else failed++
            }
            done++
            onProgress(done, total)
        }

        for (note in notes) {
            if (note.needsReencoding()) {
                val ok = runCatching {
                    noteDao.updateNote(note.reencoded())
                }.isSuccess
                if (ok) encrypted++ else failed++
            }
            done++
            onProgress(done, total)
        }

        return EncryptionMigrationResult(encrypted = encrypted, failed = failed)
    }
}
