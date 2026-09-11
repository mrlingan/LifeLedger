package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val RECENT_LIMIT = 8
private const val IN_PROGRESS_LIMIT = 3

enum class AchievementFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.home_filter_all),
    COMPLETED(R.string.home_filter_completed),
    IN_PROGRESS(R.string.home_filter_in_progress)
}

/**
 * 首页状态。
 *
 * 所有统计都在这里算好（数据变化时算一次），
 * 页面只负责显示，不在组合函数里做计算。
 */
data class HomeUiState(
    val all: List<Achievement> = emptyList(),
    val filter: AchievementFilter = AchievementFilter.ALL,
    val isLoaded: Boolean = false,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completionRate: Float = 0f,
    val streakDays: Int = 0,
    val unlockedThisWeek: Int = 0,
    val recentlyUnlocked: List<Achievement> = emptyList(),
    val inProgress: List<Achievement> = emptyList()
) {
    /** 列表里真正展示的条目（按筛选条件） */
    val visibleAchievements: List<Achievement>
        get() = when (filter) {
            AchievementFilter.ALL -> all
            AchievementFilter.COMPLETED -> all.filter { it.isCompleted }
            AchievementFilter.IN_PROGRESS -> all.filterNot { it.isCompleted }
        }

    /** 首页「继续完成」只展示前几条，避免首页变成完整列表 */
    val inProgressPreview: List<Achievement> get() = inProgress.take(IN_PROGRESS_LIMIT)

    fun countOf(filter: AchievementFilter): Int = when (filter) {
        AchievementFilter.ALL -> totalCount
        AchievementFilter.COMPLETED -> completedCount
        AchievementFilter.IN_PROGRESS -> inProgressCount
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val achievementRepository = RepositoryProvider.get(application).achievementRepository
    private val filter = MutableStateFlow(AchievementFilter.ALL)

    val uiState: StateFlow<HomeUiState> = combine(
        achievementRepository.observeAllAchievements(),
        filter
    ) { achievements, currentFilter ->
        buildHomeState(achievements = achievements, filter = currentFilter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun setFilter(value: AchievementFilter) {
        filter.value = value
    }

    /** 首页上直接勾选完成，和详情页共用同一套逻辑 */
    fun toggleCompleted(achievementId: Long) {
        viewModelScope.launch {
            achievementRepository.toggleCompletion(achievementId)
        }
    }
}

private fun buildHomeState(
    achievements: List<Achievement>,
    filter: AchievementFilter
): HomeUiState {
    val completed = achievements.filter { it.isCompleted }
    val inProgress = achievements
        .filterNot { it.isCompleted }
        .sortedByDescending { it.createdDate }

    val recentlyUnlocked = completed
        .filter { it.completedDate != null }
        .sortedByDescending { it.completedDate ?: 0L }
        .take(RECENT_LIMIT)

    val today = startOfDay(System.currentTimeMillis())
    val weekStart = today - 6 * DAY_MILLIS

    return HomeUiState(
        all = achievements,
        filter = filter,
        isLoaded = true,
        totalCount = achievements.size,
        completedCount = completed.size,
        inProgressCount = inProgress.size,
        completionRate = if (achievements.isEmpty()) {
            0f
        } else {
            completed.size.toFloat() / achievements.size
        },
        streakDays = calculateStreak(achievements),
        unlockedThisWeek = completed.count { (it.completedDate ?: 0L) >= weekStart },
        recentlyUnlocked = recentlyUnlocked,
        inProgress = inProgress
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
