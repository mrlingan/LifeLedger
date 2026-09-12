package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private const val RECENT_LIMIT = 8

/**
 * 首页状态：只有总览，没有列表。
 *
 * 首页回答的是「我已经完成了多少」，所以这里只有完成数、完成度、
 * 最近解锁和人生数据。成就列表（全部 / 已完成 / 进行中）属于
 * 「全部成就」页，状态在 [AchievementListUiState]。
 *
 * 所有统计都在这里算好（数据变化时算一次），
 * 页面只负责显示，不在组合函数里做计算。
 */
data class HomeUiState(
    val isLoaded: Boolean = false,
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val completionRate: Float = 0f,
    val streakDays: Int = 0,
    val unlockedThisWeek: Int = 0,
    val recentlyUnlocked: List<Achievement> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val achievementRepository = RepositoryProvider.get(application).achievementRepository

    val uiState: StateFlow<HomeUiState> = achievementRepository.observeAllAchievements()
        .map { achievements -> buildHomeState(achievements) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}

private fun buildHomeState(achievements: List<Achievement>): HomeUiState {
    val completed = achievements.filter { it.isCompleted }

    val recentlyUnlocked = completed
        .filter { it.completedDate != null }
        .sortedByDescending { it.completedDate ?: 0L }
        .take(RECENT_LIMIT)

    val today = startOfDay(System.currentTimeMillis())
    val weekStart = today - 6 * DAY_MILLIS

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
        streakDays = calculateStreak(achievements),
        unlockedThisWeek = completed.count { (it.completedDate ?: 0L) >= weekStart },
        recentlyUnlocked = recentlyUnlocked
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
