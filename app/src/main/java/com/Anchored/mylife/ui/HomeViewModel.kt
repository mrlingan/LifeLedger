package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.AchievementMedia
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.theme.RarityTier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val RECENT_LIMIT = 3

/** 每完成这么多条成就，向上一个阶段 */
private const val LEVEL_STEP = 5

/** 图鉴某个分类的收集进度 */
data class CategoryProgress(
    /** 数据库里存的中文分类名，显示前要过 PresetTextResolver.categoryOf */
    val category: String,
    val unlockedCount: Int,
    val totalCount: Int
) {
    val progress: Float
        get() = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
}

/** 最近完成的一条成就，附带图鉴稀有度（自己手写的成就没有稀有度，为 null） */
data class RecentAchievement(
    val achievement: Achievement,
    val tier: RarityTier?,
    /** 卡片封面：这条成就下最早的一张图片，没有就是 null */
    val photoPath: String? = null
)

/**
 * 首页状态：人生仪表盘。
 *
 * 回答三个问题：
 *   - 我走到哪了 → [level] / [completionRate] / [categories]
 *   - 我完成了什么 → [completedCount] / [recent]
 *   - 我用它记录了多久 → [recordingDays] / [streakDays]
 *
 * 列表（全部 / 已完成 / 进行中）不在这里，见 [AchievementListUiState]。
 * 所有统计都在这里算好（数据变化时算一次），页面只负责显示。
 */
data class HomeUiState(
    val isLoaded: Boolean = false,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completionRate: Float = 0f,
    /** 阶段：由累计完成数推导，不是游戏等级 */
    val level: Int = 1,
    /** 距离下一阶段还差几条 */
    val toNextLevel: Int = LEVEL_STEP,
    val streakDays: Int = 0,
    /** 有完成记录的不同天数（累计，只增不减） */
    val recordedDays: Int = 0,
    /** 图鉴：已达成条目数 / 总条目数 */
    val codexUnlocked: Int = 0,
    val codexTotal: Int = 0,
    val categories: List<CategoryProgress> = emptyList(),
    val recent: List<RecentAchievement> = emptyList(),
    /** 第一条记录的时间，用来显示「从 X 开始记录」 */
    val firstRecordDate: Long? = null,
    /** 个人资料：昵称会出现在问候语里，签名会出现在问候语下面 */
    val nickname: String = "",
    val signature: String = "",
    /** 配了头像就显示在首页左上角 */
    val avatarPath: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val achievementRepository = repositories.achievementRepository
    private val presetRepository = repositories.presetAchievementRepository
    private val mediaRepository = repositories.mediaRepository
    private val settings = repositories.settings

    /** 个人资料打包成一个流：combine 直接接的上限是 5 个 */
    private val profile = combine(
        settings.nickname,
        settings.signature,
        settings.avatarPath
    ) { nickname, signature, avatarPath ->
        Triple(nickname, signature, avatarPath)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        achievementRepository.observeAllAchievements(),
        presetRepository.observeAll(),
        mediaRepository.observeAchievementImages(),
        profile,
        settings.favoriteCategories
    ) { achievements, presets, images, profileState, favorites ->
        buildHomeState(
            achievements = achievements,
            presets = presets,
            images = images,
            nickname = profileState.first,
            signature = profileState.second,
            avatarPath = profileState.third,
            favoriteCategories = favorites
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

private fun buildHomeState(
    achievements: List<Achievement>,
    presets: List<PresetAchievement>,
    images: List<AchievementMedia>,
    nickname: String,
    signature: String,
    avatarPath: String?,
    favoriteCategories: Set<String>
): HomeUiState {
    val completed = achievements.filter { it.isCompleted }

    // 每条成就取最早的一张图当封面（查询已按时间正序，先到先得）
    val coverByAchievement = HashMap<Long, String>()
    for (image in images) {
        coverByAchievement.putIfAbsent(image.achievementId, image.filePath)
    }

    // 图鉴的「已达成」= 存在一条 presetId 相同、且已完成的成就（和图鉴页同一套判定）
    val unlockedPresetIds = completed.mapNotNull { it.presetId }.toSet()

    // 分类进度：按图鉴分类统计，没有关联图鉴的自建成就自然不计入
    val categories = presets
        .groupBy { it.category }
        .map { (category, items) ->
            CategoryProgress(
                category = category,
                unlockedCount = items.count { it.id in unlockedPresetIds },
                totalCount = items.size
            )
        }
        // 关注过的分类永远排最前，然后是「有进展的」，
        // 都没进展时回落到按条目数排序，顺序是稳定的
        .sortedWith(
            compareByDescending<CategoryProgress> { it.category in favoriteCategories }
                .thenByDescending { it.unlockedCount }
                .thenByDescending { it.progress }
                .thenByDescending { it.totalCount }
                .thenBy { it.category }
        )

    val presetById = presets.associateBy { it.id }
    val recent = completed
        .filter { it.completedDate != null }
        .sortedByDescending { it.completedDate ?: 0L }
        .take(RECENT_LIMIT)
        .map { achievement ->
            RecentAchievement(
                achievement = achievement,
                tier = achievement.presetId
                    ?.let { presetById[it] }
                    ?.let { RarityTier.fromRate(it.rate) },
                photoPath = coverByAchievement[achievement.id]
            )
        }

    val today = startOfDay(System.currentTimeMillis())
    val firstRecordDate = achievements.minOfOrNull { it.createdDate }

    return HomeUiState(
        isLoaded = true,
        totalCount = achievements.size,
        completedCount = completed.size,
        inProgressCount = achievements.size - completed.size,
        completionRate = if (achievements.isEmpty()) {
            0f
        } else {
            completed.size.toFloat() / achievements.size
        },
        level = 1 + completed.size / LEVEL_STEP,
        toNextLevel = LEVEL_STEP - completed.size % LEVEL_STEP,
        streakDays = calculateStreak(achievements),
        recordedDays = completed
            .mapNotNull { it.completedDate }
            .map { startOfDay(it) }
            .toHashSet()
            .size,
        codexUnlocked = unlockedPresetIds.size,
        codexTotal = presets.size,
        categories = categories,
        recent = recent,
        firstRecordDate = firstRecordDate,
        nickname = nickname,
        signature = signature,
        avatarPath = avatarPath
    )
}

/**
 * 连续记录天数：把所有完成日期去重后，从今天往前数。
 * 今天还没完成不算断——从昨天开始数，符合"坚持"的真实语义。
 */
private fun calculateStreak(achievements: List<Achievement>): Int {
    val days = achievements
        .mapNotNull { it.completedDate }
        .map { startOfDay(it) }
        .toHashSet()

    if (days.isEmpty()) return 0

    var cursor = startOfDay(System.currentTimeMillis())
    if (!days.contains(cursor)) {
        cursor -= DAY_MILLIS
    }

    var streak = 0
    while (days.contains(cursor)) {
        streak++
        cursor -= DAY_MILLIS
    }
    return streak
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
