package com.Anchored.mylife.data.repository

import com.Anchored.mylife.data.dao.PresetAchievementDao
import com.Anchored.mylife.data.database.PresetAchievement
import kotlinx.coroutines.flow.Flow

/**
 * 图鉴仓库：只负责图鉴的「内容」。
 *
 * 达成状态不在这里——它由 AchievementRepository 里 presetId 相同的成就决定，
 * 所以这个仓库里找不到 unlock / toggle 之类的方法。
 */
class PresetAchievementRepository(
    private val presetDao: PresetAchievementDao
) {

    // ---------- 查询 ----------

    fun observeAll(): Flow<List<PresetAchievement>> = presetDao.observeAll()

    fun observeByCategory(category: String): Flow<List<PresetAchievement>> =
        presetDao.observeByCategory(category)

    /** 关键词搜索，空关键词时按标题模糊匹配全部 */
    fun search(keyword: String): Flow<List<PresetAchievement>> =
        presetDao.search("%${keyword.trim()}%")

    suspend fun getAll(): List<PresetAchievement> = presetDao.getAll()

    suspend fun getById(id: Long): PresetAchievement? = presetDao.getById(id)

    /** 图鉴里的全部分类，用于分类标签 */
    suspend fun getCategories(): List<String> = presetDao.getAllCategories()

    suspend fun count(): Int = presetDao.count()

    // ---------- 图标填写 ----------

    /** 还有多少条没配图标 */
    suspend fun countWithoutIcon(): Int = presetDao.countWithoutIcon()

    /** 取出所有还没配图标的，方便逐条补 */
    suspend fun getWithoutIcon(): List<PresetAchievement> = presetDao.getWithoutIcon()

    suspend fun setIcon(id: Long, iconEmoji: String) = presetDao.updateIcon(id, iconEmoji)

    suspend fun update(item: PresetAchievement) = presetDao.update(item)
}
