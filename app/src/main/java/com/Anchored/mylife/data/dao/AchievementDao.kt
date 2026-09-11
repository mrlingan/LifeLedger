package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.Achievement
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(items: List<Achievement>): List<Long>

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Delete
    suspend fun deleteAchievement(achievement: Achievement)

    @Query("SELECT * FROM achievements ORDER BY createdDate DESC")
    suspend fun getAllAchievements(): List<Achievement>

    @Query("SELECT * FROM achievements ORDER BY createdDate DESC")
    fun observeAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE id = :achievementId LIMIT 1")
    suspend fun getAchievementById(achievementId: Long): Achievement?

    /** 详情页用：这条成就被修改 / 删除时自动推送最新值（删除后为 null） */
    @Query("SELECT * FROM achievements WHERE id = :achievementId LIMIT 1")
    fun observeAchievementById(achievementId: Long): Flow<Achievement?>

    @Query("SELECT * FROM achievements WHERE isCompleted = :isCompleted ORDER BY createdDate DESC")
    suspend fun getAchievementsByCompletionStatus(isCompleted: Boolean): List<Achievement>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun countAchievements(): Int

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()

    @Query(
        """
        UPDATE achievements
        SET isCompleted = :isCompleted,
            completedDate = :completedDate
        WHERE id = :achievementId
        """
    )
    suspend fun updateCompletionStatus(
        achievementId: Long,
        isCompleted: Boolean,
        completedDate: Long?
    )
}
