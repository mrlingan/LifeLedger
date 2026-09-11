package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.PresetAchievement
import kotlinx.coroutines.flow.Flow

/**
 * 图鉴表只负责「内容」：标题、描述、故事、分类、稀有度、达成率、图标。
 *
 * 「这条成就有没有达成」不在这里，而是看成就表里有没有 presetId 相同、
 * 且 isCompleted 为 true 的记录——所以这个 DAO 里没有任何解锁相关的写入方法。
 */
@Dao
interface PresetAchievementDao {

    // ---------- 查询 ----------

    /** 全部预设成就，按达成率升序（越稀有的越靠前） */
    @Query("SELECT * FROM preset_achievements ORDER BY rate ASC, id ASC")
    fun observeAll(): Flow<List<PresetAchievement>>

    @Query("SELECT * FROM preset_achievements WHERE category = :category ORDER BY rate ASC, id ASC")
    fun observeByCategory(category: String): Flow<List<PresetAchievement>>

    @Query(
        """
        SELECT * FROM preset_achievements
        WHERE title LIKE :keyword OR description LIKE :keyword OR story LIKE :keyword
        ORDER BY rate ASC, id ASC
        """
    )
    fun search(keyword: String): Flow<List<PresetAchievement>>

    @Query("SELECT * FROM preset_achievements ORDER BY id ASC")
    suspend fun getAll(): List<PresetAchievement>

    @Query("SELECT * FROM preset_achievements WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PresetAchievement?

    @Query("SELECT DISTINCT category FROM preset_achievements ORDER BY category ASC")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT COUNT(*) FROM preset_achievements")
    suspend fun count(): Int

    // ---------- 图标填写进度 ----------

    @Query("SELECT COUNT(*) FROM preset_achievements WHERE iconEmoji = ''")
    suspend fun countWithoutIcon(): Int

    @Query("SELECT * FROM preset_achievements WHERE iconEmoji = '' ORDER BY id ASC")
    suspend fun getWithoutIcon(): List<PresetAchievement>

    // ---------- 写入 ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PresetAchievement>)

    @Update
    suspend fun update(item: PresetAchievement)

    @Query("UPDATE preset_achievements SET iconEmoji = :iconEmoji WHERE id = :id")
    suspend fun updateIcon(id: Long, iconEmoji: String)
}
