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

enum class AchievementFilter(@param:StringRes val labelRes: Int) {
    ALL(R.string.home_filter_all),
    COMPLETED(R.string.home_filter_completed),
    IN_PROGRESS(R.string.home_filter_in_progress)
}

/**
 * 「全部成就」页状态。
 *
 * 这些字段原先挂在 HomeUiState 上——因为完整列表就长在首页底部。
 * 现在列表独立成页，状态也跟着独立：首页只算总览，这里只管列表。
 */
data class AchievementListUiState(
    val all: List<Achievement> = emptyList(),
    val filter: AchievementFilter = AchievementFilter.ALL,
    /** 设置里打开「完成前二次确认」时才需要弹一次确认 */
    val confirmCompletion: Boolean = false,
    val isLoaded: Boolean = false
) {
    val totalCount: Int get() = all.size
    val completedCount: Int get() = all.count { it.isCompleted }
    val inProgressCount: Int get() = totalCount - completedCount

    val visibleAchievements: List<Achievement>
        get() = when (filter) {
            AchievementFilter.ALL -> all
            AchievementFilter.COMPLETED -> all.filter { it.isCompleted }
            AchievementFilter.IN_PROGRESS -> all.filterNot { it.isCompleted }
        }

    fun countOf(filter: AchievementFilter): Int = when (filter) {
        AchievementFilter.ALL -> totalCount
        AchievementFilter.COMPLETED -> completedCount
        AchievementFilter.IN_PROGRESS -> inProgressCount
    }
}

class AchievementListViewModel(application: Application) : AndroidViewModel(application) {

    private val achievementRepository = RepositoryProvider.get(application).achievementRepository
    private val settings = RepositoryProvider.get(application).settings
    private val filter = MutableStateFlow(AchievementFilter.ALL)

    val uiState: StateFlow<AchievementListUiState> = combine(
        achievementRepository.observeAllAchievements(),
        filter,
        settings.confirmCompletion
    ) { achievements, currentFilter, confirm ->
        AchievementListUiState(
            all = achievements,
            filter = currentFilter,
            confirmCompletion = confirm,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AchievementListUiState()
    )

    fun setFilter(value: AchievementFilter) {
        filter.value = value
    }

    /** 列表里直接勾选完成，和详情页共用同一套逻辑 */
    fun toggleCompleted(achievementId: Long) {
        viewModelScope.launch {
            achievementRepository.toggleCompletion(achievementId)
        }
    }
}
