package com.Anchored.mylife.data.repository

import com.Anchored.mylife.data.dao.AchievementDao
import com.Anchored.mylife.data.database.Achievement
import kotlinx.coroutines.flow.Flow

class AchievementRepository(
    private val achievementDao: AchievementDao
) {

    // 列表用 Flow，数据库一变 UI 自动刷新
    fun observeAllAchievements(): Flow<List<Achievement>> =
        achievementDao.observeAllAchievements()

    /** 单条成就的实时数据，详情页用；被删除时会收到 null */
    fun observeAchievementById(achievementId: Long): Flow<Achievement?> =
        achievementDao.observeAchievementById(achievementId)

    suspend fun getAllAchievements(): List<Achievement> =
        achievementDao.getAllAchievements()

    suspend fun countAchievements(): Int = achievementDao.countAchievements()

    suspend fun getAchievementById(achievementId: Long): Achievement? =
        achievementDao.getAchievementById(achievementId)

    suspend fun getCompletedAchievements(): List<Achievement> =
        achievementDao.getAchievementsByCompletionStatus(true)

    suspend fun getUncompletedAchievements(): List<Achievement> =
        achievementDao.getAchievementsByCompletionStatus(false)

    suspend fun insertAchievement(achievement: Achievement): Long =
        achievementDao.insertAchievement(achievement)

    /**
     * 批量写入（首次选择「用预设成就开始」时一次写 109 条）。
     * 和图鉴里的单条挑选走同一个入口，所以两边产生的数据结构完全一致。
     */
    suspend fun insertAchievements(items: List<Achievement>) {
        if (items.isEmpty()) return
        achievementDao.insertAchievements(items)
    }

    /**
     * 新建成就。
     *
     * @param presetId 如果这条是从图鉴里带过来的，填图鉴条目的 id，
     *                 之后图鉴就能通过它知道"这条我已经达成了"。
     */
    suspend fun createAchievement(
        title: String,
        description: String = "",
        iconEmoji: String = DEFAULT_ICON,
        presetId: Long? = null,
        createdDate: Long = System.currentTimeMillis()
    ): Long = achievementDao.insertAchievement(
        Achievement(
            title = title.trim(),
            description = description.trim(),
            createdDate = createdDate,
            completedDate = null,
            isCompleted = false,
            iconEmoji = iconEmoji,
            presetId = presetId
        )
    )

    suspend fun updateAchievement(achievement: Achievement) =
        achievementDao.updateAchievement(achievement)

    suspend fun updateAchievementInfo(
        achievementId: Long,
        title: String,
        description: String,
        iconEmoji: String
    ) {
        val existing = achievementDao.getAchievementById(achievementId) ?: return
        achievementDao.updateAchievement(
            existing.copy(
                title = title.trim(),
                description = description.trim(),
                iconEmoji = iconEmoji
            )
        )
    }

    /** 标记完成，默认用当前时间作为完成时间 */
    suspend fun markCompleted(
        achievementId: Long,
        completedDate: Long = System.currentTimeMillis()
    ) = achievementDao.updateCompletionStatus(
        achievementId = achievementId,
        isCompleted = true,
        completedDate = completedDate
    )

    suspend fun markUncompleted(achievementId: Long) =
        achievementDao.updateCompletionStatus(
            achievementId = achievementId,
            isCompleted = false,
            completedDate = null
        )

    suspend fun toggleCompletion(achievementId: Long) {
        val achievement = achievementDao.getAchievementById(achievementId) ?: return
        if (achievement.isCompleted) {
            markUncompleted(achievementId)
        } else {
            markCompleted(achievementId)
        }
    }

    /** 删除成就，Note / Media 记录由外键 CASCADE 自动清理 */
    suspend fun deleteAchievement(achievement: Achievement) =
        achievementDao.deleteAchievement(achievement)

    suspend fun deleteAchievementById(achievementId: Long) {
        val achievement = achievementDao.getAchievementById(achievementId) ?: return
        achievementDao.deleteAchievement(achievement)
    }

    companion object {
        const val DEFAULT_ICON = "🏆"
    }
}
