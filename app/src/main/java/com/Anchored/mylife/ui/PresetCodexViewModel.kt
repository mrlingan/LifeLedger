package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.PresetAchievementRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.theme.RarityTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 图鉴状态。
 *
 * 图鉴本身只存内容，**是否达成由用户自己的成就决定**：
 * 存在 presetId 相同、且 isCompleted 的成就，才算已达成。
 */
data class PresetCodexUiState(
    val allItems: List<PresetAchievement> = emptyList(),
    /** 图鉴条目 id -> 用户自己的那条成就 */
    val linkedAchievements: Map<Long, Achievement> = emptyMap(),
    /** 预先算好的稀有度档位，避免每次重组都重新推导 */
    val tierByPresetId: Map<Long, RarityTier> = emptyMap(),
    val categories: List<String> = emptyList(),
    /** null 表示「全部」 */
    val selectedCategory: String? = null,
    val query: String = "",
    val isLoaded: Boolean = false
) {
    /**
     * 列表里真正展示的条目（按分类 + 搜索词筛选）。
     *
     * [textOf] 由界面传进来：文案要按系统语言显示，而「当前是什么语言」只有界面层知道，
     * 所以搜索也按本地化之后的文案匹配。
     */
    fun visibleItems(textOf: (PresetAchievement) -> PresetText): List<PresetAchievement> {
        val keyword = query.trim()
        return allItems
            .filter { selectedCategory == null || it.category == selectedCategory }
            .filter {
                if (keyword.isEmpty()) return@filter true
                val text = textOf(it)
                text.title.contains(keyword, ignoreCase = true) ||
                    text.description.contains(keyword, ignoreCase = true) ||
                    text.story.contains(keyword, ignoreCase = true)
            }
    }

    val totalCount: Int get() = allItems.size

    val unlockedCount: Int get() = allItems.count { isCompleted(it) }

    val iconMissingCount: Int get() = allItems.count { it.iconEmoji.isBlank() }

    val progress: Float
        get() = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount

    /** 按稀有度分组：传奇在最前，组内按达成率升序（越难越靠前） */
    fun sections(textOf: (PresetAchievement) -> PresetText): List<Pair<RarityTier, List<PresetAchievement>>> {
        val visible = visibleItems(textOf)
        return RarityTier.entries
            .reversed()
            .map { tier -> tier to visible.filter { tierOf(it) == tier }.sortedBy { it.rate } }
            .filter { it.second.isNotEmpty() }
    }

    fun countOf(category: String): Int = allItems.count { it.category == category }

    fun tierOf(preset: PresetAchievement): RarityTier =
        tierByPresetId[preset.id] ?: RarityTier.fromRate(preset.rate)

    /** 这条图鉴成就在「我的成就」里对应的记录，还没添加过就是 null */
    fun achievementOf(preset: PresetAchievement): Achievement? = linkedAchievements[preset.id]

    fun isCompleted(preset: PresetAchievement): Boolean = achievementOf(preset)?.isCompleted == true
}

class PresetAchievementViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val presetRepository: PresetAchievementRepository = repositories.presetAchievementRepository
    private val achievementRepository: AchievementRepository = repositories.achievementRepository

    private val selectedCategory = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<PresetCodexUiState> = combine(
        presetRepository.observeAll(),
        achievementRepository.observeAllAchievements(),
        selectedCategory,
        query
    ) { presets, achievements, category, keyword ->
        PresetCodexUiState(
            allItems = presets,
            linkedAchievements = achievements
                .mapNotNull { achievement -> achievement.presetId?.let { it to achievement } }
                .toMap(),
            tierByPresetId = presets.associate { it.id to RarityTier.fromRate(it.rate) },
            categories = presets.groupingBy { it.category }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key },
            selectedCategory = category,
            query = keyword,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PresetCodexUiState()
    )

    fun setCategory(category: String?) {
        selectedCategory.value = category
    }

    fun setQuery(value: String) {
        query.value = value
    }

    /**
     * 标记达成 / 取消达成。
     *
     * 改的其实是成就表：
     * - 还没加进列表 → 新建一条并直接标记完成
     * - 已在列表且已完成 → 取消完成
     * - 已在列表但没完成 → 标记完成
     */
    fun toggleCompleted(preset: PresetAchievement, text: PresetText) {
        viewModelScope.launch {
            val linked = uiState.value.achievementOf(preset)
            when {
                linked == null -> {
                    val newId = achievementRepository.createAchievement(
                        title = text.title,
                        description = text.description,
                        iconEmoji = preset.iconEmoji.ifBlank { AchievementRepository.DEFAULT_ICON },
                        presetId = preset.id
                    )
                    achievementRepository.markCompleted(newId)
                }

                linked.isCompleted -> achievementRepository.markUncompleted(linked.id)

                else -> achievementRepository.markCompleted(linked.id)
            }
        }
    }

    /** 先记下来，之后再慢慢完成 */
    fun addToMyAchievements(preset: PresetAchievement, text: PresetText) {
        if (uiState.value.achievementOf(preset) != null) return
        viewModelScope.launch {
            achievementRepository.createAchievement(
                title = text.title,
                description = text.description,
                iconEmoji = preset.iconEmoji.ifBlank { AchievementRepository.DEFAULT_ICON },
                presetId = preset.id
            )
        }
    }
}
