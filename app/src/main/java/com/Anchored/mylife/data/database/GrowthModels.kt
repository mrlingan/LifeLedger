package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Immutable ledger entry. The current balance is always derived from this table. */
@Entity(tableName = "point_transactions", indices = [Index(value = ["createdAt"]), Index(value = ["sourceId"])])
data class PointTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val type: String,
    val sourceId: String? = null,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 流水账上这一笔属于哪一类经历（[PointTransaction.type]）。
 *
 * 存的是标识、不是文案：成长记录里那行字（"完成成就" / "每日任务"）按当前语言
 * 现取，所以换语言不需要迁移任何历史数据。
 */
object PointType {
    /** 完成一条成就 */
    const val ACHIEVEMENT = "achievement"

    /** 取消完成，把上面那笔退回去 */
    const val ACHIEVEMENT_REVERSAL = "achievement_reversal"

    /** 完成一个每日任务 */
    const val DAILY_TASK = "daily_task"

    /** 每日事件的奖励 */
    const val RANDOM_EVENT = "random_event"

    /** 在积分商城兑换了一件奖励 */
    const val REWARD_REDEEM = "reward_redeem"

    /** 撤销一次兑换，把积分退回来 */
    const val REWARD_REFUND = "reward_refund"
}

@Entity(tableName = "goals", indices = [Index(value = ["status"])])
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val startDate: Long,
    val dueDate: Long,
    val status: String = STATUS_ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object { const val STATUS_ACTIVE = "active"; const val STATUS_COMPLETED = "completed"; const val STATUS_OVERDUE = "overdue"; const val STATUS_PAUSED = "paused" }
}

@Entity(tableName = "goal_tasks", indices = [Index(value = ["goalId"]), Index(value = ["scheduledDate"])])
data class GoalTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val description: String = "",
    val scheduledDate: Long,
    val reward: Int = 10,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null
)

@Entity(tableName = "daily_events", indices = [Index(value = ["dateKey"], unique = true)])
data class DailyEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val title: String,
    val description: String,
    val type: String,
    val rarity: String,
    /** Resolver key, deliberately separate from UI copy. */
    val effect: String,
    val reward: Int = 0,
    val penalty: Int = 0,
    val condition: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val isDrawn: Boolean = true,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val resolvedAt: Long? = null
)
