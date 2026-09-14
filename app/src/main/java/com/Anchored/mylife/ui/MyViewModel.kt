package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.theme.RarityTier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 图鉴里一个稀有度档位的收集情况 */
data class TierProgress(
    val tier: RarityTier,
    val unlockedCount: Int,
    val totalCount: Int
) {
    val progress: Float
        get() = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
}

/**
 * 「我的」页状态：个人资料 + 人生数据 + 收集情况。
 *
 * 阶段、坚持天数、图鉴解锁这些口径和首页完全一致（共同算法在 LifeStats.kt 里），
 * 所以两个页面的数字永远对得上。这一页多出来的是两样别处没有的东西：
 * 图鉴按稀有度分开看的收集情况（tiers），以及库里的数据量（notesCount / mediaCount）。
 */
data class MyUiState(
    val isLoaded: Boolean = false,

    // 个人资料
    val nickname: String = "",
    val signature: String = "",
    val avatarPath: String? = null,
    val avatarPreset: AvatarPreset? = null,

    // 人生数据
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

    // 图鉴收集
    val codexUnlocked: Int = 0,
    val codexTotal: Int = 0,
    /** 按稀有度分的五档，从常见到稀有 */
    val tiers: List<TierProgress> = emptyList(),

    // 数据量
    val notesCount: Int = 0,
    val mediaCount: Int = 0,

    /** 第一条记录的时间，用来显示「从 X 开始记录」 */
    val firstRecordDate: Long? = null
)

/**
 * 「我的」页的数据源。
 *
 * 统计一律走 LifeStats 那套共同算法，这里只负责把它和资料、数据量拼起来；
 * 稀有度怎么算（RarityTier.fromRate）也在数据层做完，页面只负责显示。
 */
class MyViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val achievementRepository = repositories.achievementRepository
    private val presetRepository = repositories.presetAchievementRepository
    private val noteRepository = repositories.noteRepository
    private val mediaRepository = repositories.mediaRepository
    private val settings = repositories.settings

    /** 个人资料打包成一个流：combine 直接接的上限是 5 个 */
    private val profile = combine(
        settings.nickname,
        settings.signature,
        settings.avatarPath,
        settings.avatarPreset
    ) { nickname, signature, avatarPath, avatarPreset ->
        MyProfile(nickname, signature, avatarPath, avatarPreset)
    }

    /** 库里的数据量：笔记 + 图片视频，写一条笔记就立刻反映到「我的人生」那张卡上 */
    private val contentCounts = combine(
        noteRepository.observeCount(),
        mediaRepository.observeCount()
    ) { notes, media -> notes to media }

    val uiState: StateFlow<MyUiState> = combine(
        achievementRepository.observeAllAchievements(),
        presetRepository.observeAll(),
        profile,
        contentCounts
    ) { achievements, presets, profileState, counts ->
        buildMyState(
            achievements = achievements,
            presets = presets,
            profile = profileState,
            notesCount = counts.first,
            mediaCount = counts.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MyUiState()
    )
}

/** 「我的」页要知道的个人资料：页头那块牌子，以及点一下进资料页 */
private data class MyProfile(
    val nickname: String,
    val signature: String,
    val avatarPath: String?,
    val avatarPreset: AvatarPreset?
)

private fun buildMyState(
    achievements: List<Achievement>,
    presets: List<PresetAchievement>,
    profile: MyProfile,
    notesCount: Int,
    mediaCount: Int
): MyUiState {
    val completedCount = achievements.count { it.isCompleted }
    val unlockedIds = unlockedPresetIds(achievements)

    // 图鉴按稀有度分档。换算在循环外做一次，
    // 否则 109 条预设会被反复算好几遍
    val tierOfPreset = presets.map { it to RarityTier.fromRate(it.rate) }
    val tiers = RarityTier.entries.map { tier ->
        val inTier = tierOfPreset.filter { (_, presetTier) -> presetTier == tier }
        TierProgress(
            tier = tier,
            unlockedCount = inTier.count { (preset, _) -> preset.id in unlockedIds },
            totalCount = inTier.size
        )
    }

    return MyUiState(
        isLoaded = true,
        nickname = profile.nickname,
        signature = profile.signature,
        avatarPath = profile.avatarPath,
        avatarPreset = profile.avatarPreset,
        totalCount = achievements.size,
        completedCount = completedCount,
        inProgressCount = achievements.size - completedCount,
        completionRate = if (achievements.isEmpty()) {
            0f
        } else {
            completedCount.toFloat() / achievements.size
        },
        level = levelOf(completedCount),
        toNextLevel = toNextLevelCount(completedCount),
        streakDays = calculateStreak(achievements),
        recordedDays = recordedDays(achievements),
        codexUnlocked = unlockedIds.size,
        codexTotal = presets.size,
        tiers = tiers,
        notesCount = notesCount,
        mediaCount = mediaCount,
        firstRecordDate = achievements.minOfOrNull { it.createdDate }
    )
}
