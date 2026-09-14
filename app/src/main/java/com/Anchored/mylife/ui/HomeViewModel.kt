package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.AchievementMedia
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.data.settings.HomeSectionEntry
import com.Anchored.mylife.ui.theme.RarityTier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val RECENT_LIMIT = 3

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
    /** 配了头像就显示在首页左上角：上传的图片 */
    val avatarPath: String? = null,
    /** 或者他挑的那个内置头像 */
    val avatarPreset: AvatarPreset? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val achievementRepository = repositories.achievementRepository
    private val presetRepository = repositories.presetAchievementRepository
    private val mediaRepository = repositories.mediaRepository
    private val settings = repositories.settings

    /**
     * 首页板块的显示顺序（由用户在「设置 → 首页板块」里决定）。
     *
     * 单独一条流，不并进 [uiState]：板块顺序和统计数据没有关系，
     * 改顺序不该让整页统计重算一遍。
     */
    val homeSections: StateFlow<List<HomeSectionEntry>> = settings.homeSections

    /**
     * 分类圆环的颜色（历史偏好：挑过颜色的分类沿用当时那个色）。
     *
     * 和板块顺序一样单独一条流：颜色只影响那一行圆环怎么画，
     * 变更时没有任何统计需要重算。
     */
    val categoryColors: StateFlow<Map<String, String>> = settings.categoryColors

    /** 首页各份自定义图片的配图：板块实例 id → 绝对路径；没配图的那份不在表里 */
    val homeSectionImages: StateFlow<Map<String, String>> = settings.homeSectionImages


    /** 个人资料打包成一个流：combine 直接接的上限是 5 个 */
    private val profile = combine(
        settings.nickname,
        settings.signature,
        settings.avatarPath,
        settings.avatarPreset
    ) { nickname, signature, avatarPath, avatarPreset ->
        HomeProfile(nickname, signature, avatarPath, avatarPreset)
    }

    /** 分类相关的两个偏好也打包：关注（影响排序）+ 挑过要显示的那几个（影响顺序） */
    private val categoryPrefs = combine(
        settings.favoriteCategories,
        settings.homeCategories
    ) { favorites, chosen -> favorites to chosen }

    val uiState: StateFlow<HomeUiState> = combine(
        achievementRepository.observeAllAchievements(),
        presetRepository.observeAll(),
        mediaRepository.observeAchievementImages(),
        profile,
        categoryPrefs
    ) { achievements, presets, images, profileState, categories ->
        buildHomeState(
            achievements = achievements,
            presets = presets,
            images = images,
            nickname = profileState.nickname,
            signature = profileState.signature,
            avatarPath = profileState.avatarPath,
            avatarPreset = profileState.avatarPreset,
            favoriteCategories = categories.first,
            chosenCategories = categories.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

/** 首页要知道的个人资料：问候语用昵称和签名，左上角用头像（图片或内置头像） */
private data class HomeProfile(
    val nickname: String,
    val signature: String,
    val avatarPath: String?,
    val avatarPreset: AvatarPreset?
)

private fun buildHomeState(
    achievements: List<Achievement>,
    presets: List<PresetAchievement>,
    images: List<AchievementMedia>,
    nickname: String,
    signature: String,
    avatarPath: String?,
    avatarPreset: AvatarPreset?,
    favoriteCategories: Set<String>,
    chosenCategories: List<String> = emptyList()
): HomeUiState {
    val completed = achievements.filter { it.isCompleted }

    // 每条成就取最早的一张图当封面（查询已按时间正序，先到先得）
    val coverByAchievement = HashMap<Long, String>()
    for (image in images) {
        coverByAchievement.putIfAbsent(image.achievementId, image.filePath)
    }

    // 图鉴的「已达成」= 存在一条 presetId 相同、且已完成的成就（和图鉴页同一套判定）
    val unlockedIds = unlockedPresetIds(achievements)

    // 分类进度：图鉴的分类按"图鉴条目"统计；用户自己写、又挑了分类的成就，
    // 按"自己的完成情况"补上——自建分类因此也能出现在首页那一行里
    val statsByCategory = LinkedHashMap<String, Pair<Int, Int>>() // 分类 -> (已完成, 总数)
    presets.groupBy { it.category }.forEach { (category, items) ->
        statsByCategory[category] = items.count { it.id in unlockedIds } to items.size
    }
    achievements
        .filter { it.presetId == null && it.category.isNotBlank() }
        .groupBy { it.category }
        .forEach { (category, items) ->
            val current = statsByCategory[category] ?: (0 to 0)
            statsByCategory[category] = (current.first + items.count { it.isCompleted }) to
                (current.second + items.size)
        }

    val sortedCategories = statsByCategory
        .map { (category, counts) ->
            CategoryProgress(
                category = category,
                unlockedCount = counts.first,
                totalCount = counts.second
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

    // 偏好里存着挑过的顺序（老版本挑的、或者从备份恢复来的）就按它排在最前面；
    // 没有就还是上面那套自动排序（首页取前五个）
    val categories = if (chosenCategories.isEmpty()) {
        sortedCategories
    } else {
        val byName = sortedCategories.associateBy { it.category }
        chosenCategories.mapNotNull { byName[it] } +
            sortedCategories.filterNot { it.category in chosenCategories }
    }

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
        level = levelOf(completed.size),
        toNextLevel = toNextLevelCount(completed.size),
        streakDays = calculateStreak(achievements),
        recordedDays = recordedDays(achievements),
        codexUnlocked = unlockedIds.size,
        codexTotal = presets.size,
        categories = categories,
        recent = recent,
        firstRecordDate = firstRecordDate,
        nickname = nickname,
        signature = signature,
        avatarPath = avatarPath,
        avatarPreset = avatarPreset
    )
}
