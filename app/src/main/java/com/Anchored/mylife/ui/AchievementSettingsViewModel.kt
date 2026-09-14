package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.AppSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AchievementSettingsUiState(
    val defaultIcon: String = "",
    val confirmCompletion: Boolean = false,
    val favoriteCategories: Set<String> = emptySet(),
    /** 图鉴里实际存在的分类，按条目数排序 */
    val allCategories: List<String> = emptyList()
)

class AchievementSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings: AppSettings = repositories.settings
    private val presetRepository = repositories.presetAchievementRepository

    val uiState: StateFlow<AchievementSettingsUiState> = combine(
        settings.defaultIcon,
        settings.confirmCompletion,
        settings.favoriteCategories,
        presetRepository.observeAll()
    ) { icon, confirm, favorites, presets ->
        AchievementSettingsUiState(
            defaultIcon = icon,
            confirmCompletion = confirm,
            favoriteCategories = favorites,
            allCategories = presets.groupingBy { it.category }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AchievementSettingsUiState()
    )

    fun setDefaultIcon(icon: String) = settings.setDefaultIcon(icon)

    fun setConfirmCompletion(value: Boolean) = settings.setConfirmCompletion(value)

    fun toggleCategory(category: String) {
        val current = settings.favoriteCategories.value
        settings.setFavoriteCategories(
            if (category in current) current - category else current + category
        )
    }
}
