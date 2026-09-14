package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.DailyEvent
import com.Anchored.mylife.data.database.Goal
import com.Anchored.mylife.data.database.GoalTask
import com.Anchored.mylife.data.database.PointTransaction
import com.Anchored.mylife.data.database.PointType
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.repository.GrowthRepository
import com.Anchored.mylife.data.repository.PointService
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 一个「人生属性」。
 *
 * 它不是一个存下来的东西，而是某个分类下**已经完成**的经历的另一种读法：
 * 3 条「知识」类的成就 = 150 XP = Lv.1 的一半。等级、进度都是现算的，
 * 所以给一条成就改了分类，这里的分布立刻跟着变。
 */
data class GrowthAttribute(
    /** 数据库里存的分类名（中文原文），显示前要过 PresetTextResolver.categoryOf */
    val category: String,
    val completedCount: Int,
    /** 这个属性攒下的经验值：完成条数 × 每条给的积分 */
    val xp: Int,
    val level: Int,
    /** 当前等级已经走完的比例 */
    val levelProgress: Float,
    /** 当前等级里已经攒了多少 XP */
    val xpIntoLevel: Int
)

/**
 * 流水账里的一笔，收拾成成长记录那一行要的样子。
 *
 * [title] 是记账当时存下的原文（成就标题 / 任务名 / 奖励名）；
 * [presetId] 是这笔对应图鉴里的哪一条——有它的话，界面用**当前语言**的标题覆盖
 * [title]，所以切语言之后这一页也跟着变，和「我的」页的图鉴判定是同一个做法。
 */
data class GrowthRecord(
    val id: Long,
    val type: String,
    val amount: Int,
    val createdAt: Long,
    val title: String,
    val presetId: Long?
)

/**
 * 成长页状态。
 *
 * 这一页回答"我攒下了什么"：走到哪一阶段（[level] / [toNextLevel]）、
 * 账上有多少积分（[balance]）、哪些方向长出了属性（[attributes]）、
 * 最近攒下的每一笔（[transactions]），以及接着要做什么（[goals] / [tasks] / [event]）。
 */
data class GrowthUiState(
    /** 当前积分余额（由 point_transactions 加出来） */
    val balance: Int = 0,
    /** 流水账，按时间正序；成长记录那一栏自己倒过来显示 */
    val transactions: List<GrowthRecord> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val tasks: List<GoalTask> = emptyList(),
    val event: DailyEvent? = null,
    /** 人生阶段：由累计完成数推导，和首页 / 我的 / 商城同一套口径 */
    val level: Int = 1,
    val toNextLevel: Int = LEVEL_STEP,
    /** 累计完成了几条经历（阶段进度用它，不是属性的和：没分类的也算数） */
    val completedCount: Int = 0,
    val attributes: List<GrowthAttribute> = emptyList()
) {
    /** 当前阶段已经走完多少条（进度条那一段） */
    val stageProgress: Int get() = levelStepProgress(completedCount)
}

/**
 * 成长页。
 *
 * 余额不单独订阅一条流：`point_transactions` 整串流水已经在手里了，
 * 余额就是它的和——账只有一本，两处数字不可能对不上。
 */
class GrowthViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val repo: GrowthRepository = repositories.growthRepository
    private val points: PointService = repositories.pointService
    private val achievements = repositories.achievementRepository
    private val presets = repositories.presetAchievementRepository
    private val settings = repositories.settings

    /** 成就列表 + 图鉴目录：属性那一栏要两边一起看（见 [attributesOf]） */
    private val achievementList = combine(
        achievements.observeAllAchievements(),
        presets.observeAll()
    ) { list, catalog -> list to catalog }

    val state: StateFlow<GrowthUiState> = combine(
        points.observeTransactions(),
        repo.observeGoals(),
        repo.observeTasks(),
        repo.observeTodayEvent(),
        achievementList
    ) { transactions, goals, tasks, event, (all, catalog) ->
        val completed = all.filter { it.isCompleted }
        GrowthUiState(
            balance = transactions.sumOf { it.amount },
            transactions = transactions.map { transaction ->
                val presetId = transaction.achievementId()
                    ?.let { id -> all.firstOrNull { it.id == id } }
                    ?.presetId
                GrowthRecord(
                    id = transaction.id,
                    type = transaction.type,
                    amount = transaction.amount,
                    createdAt = transaction.createdAt,
                    title = transaction.description,
                    presetId = presetId
                )
            },
            goals = goals,
            tasks = tasks,
            event = event,
            level = levelOf(completed.size),
            toNextLevel = toNextLevelCount(completed.size),
            completedCount = completed.size,
            attributes = attributesOf(completed, catalog)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GrowthUiState()
    )

    init {
        // 过期的事件要在进页面时结算掉：罚分会记成流水里的一笔负数
        viewModelScope.launch { repo.resolveExpiredEvents() }
        // 每日一言是可选的联网功能（默认关闭）：开着才去取今天的这一条
        viewModelScope.launch {
            settings.networkEnabled.collect { enabled -> if (enabled) repo.fetchTodaySaying() }
        }
    }

    fun refreshSaying() = viewModelScope.launch { repo.fetchTodaySaying() }

    fun createGoal(title: String, description: String, days: Int) = viewModelScope.launch {
        val start = System.currentTimeMillis()
        val span = days.coerceAtLeast(1) * 86_400_000L
        repo.createGoal(title, description, start, start + span)
    }

    fun addTask(goalId: Long, title: String, reward: Int) = viewModelScope.launch {
        repo.addTask(goalId, title, "", System.currentTimeMillis(), reward)
    }

    fun completeTask(id: Long) = viewModelScope.launch {
        repo.completeTask(id)
    }
}

/**
 * 已完成的经历按分类收成「人生属性」。
 *
 * - 分类优先用成就自己的（用户挑过、或者从图鉴带过来的）；
 *   老数据里成就没有分类、但图鉴那一条有，就用图鉴的分类补上——
 *   库里 109 条图鉴条目本来就各自带分类，没有理由让同一件事在这两处对不上
 * - 两边都没有分类的（自己写、也没挑过）不进任何属性
 * - 每条按 [ATTRIBUTE_XP_PER_ACHIEVEMENT] 折算成 XP，再换算成等级与进度
 * - 攒得多的排前面，一样多时按分类名——顺序是稳定的，不会每次进页面就换位置
 */
internal fun attributesOf(
    completed: List<Achievement>,
    presets: List<PresetAchievement> = emptyList()
): List<GrowthAttribute> {
    val categoryByPresetId = presets.associate { it.id to it.category }
    return completed
        .mapNotNull { achievement ->
            val category = achievement.category.takeIf { it.isNotBlank() }
                ?: achievement.presetId?.let { categoryByPresetId[it] }
                    ?.takeIf { it.isNotBlank() }
            category
        }
        .groupingBy { it }
        .eachCount()
        .entries
        .map { (category, count) ->
            val xp = count * ATTRIBUTE_XP_PER_ACHIEVEMENT
            GrowthAttribute(
                category = category,
                completedCount = count,
                xp = xp,
                level = attributeLevelOf(xp),
                levelProgress = attributeXpIntoLevel(xp).toFloat() / ATTRIBUTE_XP_PER_LEVEL,
                xpIntoLevel = attributeXpIntoLevel(xp)
            )
        }
        .sortedWith(
            compareByDescending<GrowthAttribute> { it.completedCount }.thenBy { it.category }
        )
}

/**
 * 这一笔流水是不是某条成就的账。是的话给出成就 id。
 *
 * sourceId 的样子是 `achievement:12`（完成）和 `achievement:12:reversal`（撤销），
 * 两种都取到 `12`。不是成就的账（任务、事件、兑换）返回 null。
 */
private fun PointTransaction.achievementId(): Long? {
    if (type != PointType.ACHIEVEMENT && type != PointType.ACHIEVEMENT_REVERSAL) return null
    val source = sourceId ?: return null
    if (!source.startsWith(SOURCE_ACHIEVEMENT)) return null
    return source.removePrefix(SOURCE_ACHIEVEMENT).substringBefore(':').toLongOrNull()
}

private const val SOURCE_ACHIEVEMENT = "achievement:"
