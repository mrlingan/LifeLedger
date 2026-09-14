package com.Anchored.mylife.data.repository

import androidx.room.withTransaction
import com.Anchored.mylife.data.dao.GrowthDao
import com.Anchored.mylife.data.growth.SayingClient
import com.Anchored.mylife.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class PointService(private val database: AchievementDatabase, private val dao: GrowthDao) {
    fun observeTransactions(): Flow<List<PointTransaction>> = dao.observeTransactions()
    fun observeBalance(): Flow<Int> = dao.observeBalance()

    /**
     * 当前余额。给"要在事务里先看一眼够不够，再决定写不写"的场景用
     * （例如积分商城兑换），订阅式的 [observeBalance] 到不了这种地方。
     */
    suspend fun balance(): Int = dao.currentBalance()

    /** sourceId makes every user action idempotent, including after process recreation. */
    suspend fun record(amount: Int, type: String, sourceId: String, description: String, createdAt: Long = System.currentTimeMillis()) = database.withTransaction {
        if (dao.transactionForSource(sourceId) == null) dao.insertTransaction(PointTransaction(amount = amount, type = type, sourceId = sourceId, description = description, createdAt = createdAt))
    }
}

class GrowthRepository(private val database: AchievementDatabase, private val dao: GrowthDao, private val points: PointService) {
    fun observeGoals() = dao.observeGoals()
    fun observeTasks() = dao.observeTasks()
    fun observeTodayEvent() = dao.observeEvent(dayKey())
    suspend fun createGoal(title: String, description: String, start: Long, due: Long) = dao.insertGoal(Goal(title = title.trim(), description = description.trim(), startDate = start, dueDate = due))
    suspend fun addTask(goalId: Long, title: String, description: String, date: Long, reward: Int) = dao.insertTask(GoalTask(goalId = goalId, title = title.trim(), description = description.trim(), scheduledDate = startOfDay(date), reward = reward.coerceAtLeast(0)))
    suspend fun completeTask(id: Long) = database.withTransaction {
        val task = dao.getTask(id) ?: return@withTransaction
        if (task.isCompleted) return@withTransaction
        dao.updateTask(task.copy(isCompleted = true, completedAt = System.currentTimeMillis()))
        points.record(task.reward, PointType.DAILY_TASK, "task:${task.id}", task.title)
        resolveTodayEventInTransaction()
        refreshGoal(task.goalId)
    }
    suspend fun drawToday(): DailyEvent = database.withTransaction {
        dao.getEvent(dayKey()) ?: eventForToday().also { dao.insertEvent(it) }
    }
    /**
     * 取今天的每日一言。
     *
     * 接口本身是"每次随机一条"，所以"每天只变一次"由这里保证：取回后落库，
     * 当天再进来自动命中缓存直接返回，重开应用也不会换掉今天这一条。
     */
    suspend fun fetchTodaySaying(): DailyEvent? {
        val key = dayKey()
        val current = dao.getEvent(key)
        if (current?.effect == EFFECT_SAYING) return current
        val saying = withContext(Dispatchers.IO) { SayingClient.fetch() } ?: return current
        val event = DailyEvent(
            id = current?.id ?: 0,
            dateKey = key,
            title = saying.text,
            description = "",
            type = "special",
            rarity = "common",
            effect = EFFECT_SAYING,
            expiresAt = startOfDay(System.currentTimeMillis()) + DAY,
            isDrawn = true
        )
        database.withTransaction {
            if (current == null) dao.insertEvent(event) else dao.updateEvent(event)
        }
        return event
    }
    private suspend fun resolveTodayEventInTransaction() {
        val event = dao.getEvent(dayKey()) ?: return
        if (event.isCompleted || event.isFailed) return
        if (event.effect == "complete_tasks") {
            val start = startOfDay(System.currentTimeMillis())
            if (dao.completedTaskCount(start, start + DAY) >= event.condition) {
                dao.updateEvent(event.copy(isCompleted = true, resolvedAt = System.currentTimeMillis()))
                points.record(
                    event.reward,
                    PointType.RANDOM_EVENT,
                    "event:${event.id}:reward",
                    event.title
                )
            }
        }
    }
    /** Explicit resolution is idempotent; call after task changes and when event page opens. */
    suspend fun resolveTodayEvent(tasks: List<GoalTask>) = database.withTransaction {
        val event = dao.getEvent(dayKey()) ?: return@withTransaction
        if (event.isCompleted || event.isFailed) return@withTransaction
        if (event.effect == "complete_tasks" && tasks.count { it.isCompleted && sameDay(it.completedAt ?: 0, System.currentTimeMillis()) } >= event.condition) {
            dao.updateEvent(event.copy(isCompleted = true, resolvedAt = System.currentTimeMillis()))
            points.record(
                event.reward,
                PointType.RANDOM_EVENT,
                "event:${event.id}:reward",
                event.title
            )
        }
    }
    private suspend fun refreshGoal(goalId: Long) {
        val goal = dao.getGoal(goalId) ?: return
        val tasks = dao.getTasksForGoal(goalId)
        if (tasks.isNotEmpty() && tasks.all { it.isCompleted }) {
            dao.updateGoal(goal.copy(status = Goal.STATUS_COMPLETED))
        }
    }
    suspend fun resolveExpiredEvents(now: Long = System.currentTimeMillis()) = database.withTransaction {
        dao.getExpiredEvents(now).forEach { event ->
            dao.updateEvent(event.copy(isFailed = true, resolvedAt = now))
            // 描述里只留事件自己的名字：它是"错过"这件事的标识，
            // 那一行怎么写由界面按语言决定（见成长记录）。
            if (event.penalty > 0) {
                points.record(
                    -event.penalty,
                    PointType.RANDOM_EVENT,
                    "event:${event.id}:penalty",
                    event.title,
                    now
                )
            }
        }
    }
    private fun eventForToday(): DailyEvent {
        val key = dayKey(); val tomorrow = startOfDay(System.currentTimeMillis()) + DAY
        val variants = listOf(
            DailyEvent(dateKey = key, title = "Task sprint", description = "Complete 3 daily tasks today for an extra reward.", type = "challenge", rarity = "uncommon", effect = "complete_tasks", reward = 50, penalty = 20, condition = 3, expiresAt = tomorrow),
            DailyEvent(dateKey = key, title = "Fortune day", description = "Complete 1 daily task today for an extra reward.", type = "reward", rarity = "common", effect = "complete_tasks", reward = 30, condition = 1, expiresAt = tomorrow),
            DailyEvent(dateKey = key, title = "Night explorer", description = "Complete 2 daily tasks today for a special reward.", type = "special", rarity = "rare", effect = "complete_tasks", reward = 50, condition = 2, expiresAt = tomorrow)
        )
        return variants[Random(key.hashCode()).nextInt(variants.size)]
    }
    companion object {
        const val DAY = 86_400_000L

        /** 每日一言这类"远端取回"的事件标记，用来判断今天是不是已经取过了。 */
        const val EFFECT_SAYING = "saying_remote"
        fun startOfDay(millis: Long): Long = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        fun dayKey(millis: Long = System.currentTimeMillis()) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        fun sameDay(a: Long, b: Long) = startOfDay(a) == startOfDay(b)
    }
}
