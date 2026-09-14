package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.DailyEvent
import com.Anchored.mylife.data.database.Goal
import com.Anchored.mylife.data.database.GoalTask
import com.Anchored.mylife.data.database.PointTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface GrowthDao {
    @Insert suspend fun insertTransaction(item: PointTransaction): Long
    @Query("SELECT * FROM point_transactions ORDER BY createdAt ASC, id ASC") fun observeTransactions(): Flow<List<PointTransaction>>
    @Query("SELECT COALESCE(SUM(amount), 0) FROM point_transactions") fun observeBalance(): Flow<Int>

    /**
     * 余额的一次性读取。
     *
     * 兑换要在同一个事务里判断"够不够"，那里不能用 Flow（它吐的是订阅那一刻的值）。
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM point_transactions") suspend fun currentBalance(): Int
    @Query("SELECT * FROM point_transactions WHERE sourceId = :sourceId LIMIT 1") suspend fun transactionForSource(sourceId: String): PointTransaction?

    @Insert suspend fun insertGoal(goal: Goal): Long
    @Update suspend fun updateGoal(goal: Goal)
    @Query("SELECT * FROM goals ORDER BY dueDate ASC") fun observeGoals(): Flow<List<Goal>>
    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1") suspend fun getGoal(id: Long): Goal?

    @Insert suspend fun insertTask(task: GoalTask): Long
    @Update suspend fun updateTask(task: GoalTask)
    @Query("SELECT * FROM goal_tasks ORDER BY scheduledDate ASC, id ASC") fun observeTasks(): Flow<List<GoalTask>>
    @Query("SELECT * FROM goal_tasks WHERE id = :id LIMIT 1") suspend fun getTask(id: Long): GoalTask?
    @Query("SELECT * FROM goal_tasks WHERE goalId = :goalId") suspend fun getTasksForGoal(goalId: Long): List<GoalTask>
    @Query("SELECT COUNT(*) FROM goal_tasks WHERE isCompleted = 1 AND completedAt >= :start AND completedAt < :end") suspend fun completedTaskCount(start: Long, end: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertEvent(event: DailyEvent): Long
    @Update suspend fun updateEvent(event: DailyEvent)
    @Query("SELECT * FROM daily_events WHERE dateKey = :key LIMIT 1") fun observeEvent(key: String): Flow<DailyEvent?>
    @Query("SELECT * FROM daily_events WHERE dateKey = :key LIMIT 1") suspend fun getEvent(key: String): DailyEvent?
    @Query("SELECT * FROM daily_events WHERE expiresAt <= :now AND isCompleted = 0 AND isFailed = 0") suspend fun getExpiredEvents(now: Long): List<DailyEvent>
}
