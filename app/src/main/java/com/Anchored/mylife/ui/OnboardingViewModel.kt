package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.StartChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OnboardingStage {
    /** 还在判断要不要问（老用户直接跳过） */
    CHECKING,
    CHOOSING,
    APPLYING
}

data class OnboardingUiState(
    val stage: OnboardingStage = OnboardingStage.CHECKING,
    /** 正在执行的那个选择，用来只在那张卡上显示进度 */
    val applying: StartChoice? = null
)

/**
 * 首次启动的起点选择。
 *
 * 两种选择的区别只在于「我的成就」列表一开始是空的还是装满 109 条预设：
 * 图鉴本身永远有那 109 条内容，随时可以浏览和挑选，所以这个选择不会让人后悔。
 *
 * 升级上来的老用户（库里已经有成就）不会看到这一页，直接记为「从空白开始」。
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings: AppSettings = repositories.settings
    private val achievementRepository: AchievementRepository = repositories.achievementRepository
    private val presetRepository = repositories.presetAchievementRepository

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = withContext(Dispatchers.IO) {
                runCatching { achievementRepository.countAchievements() }.getOrDefault(0)
            }
            if (existing > 0) {
                // 已经在用了，别再问一次
                settings.setStartChoice(StartChoice.BLANK)
            } else {
                _uiState.value = OnboardingUiState(stage = OnboardingStage.CHOOSING)
            }
        }
    }

    fun choose(choice: StartChoice) {
        if (_uiState.value.stage != OnboardingStage.CHOOSING) return
        _uiState.value = OnboardingUiState(stage = OnboardingStage.APPLYING, applying = choice)

        viewModelScope.launch {
            if (choice == StartChoice.PRESETS) {
                withContext(Dispatchers.IO) { addAllPresets() }
            }
            settings.setStartChoice(choice)
        }
    }

    /**
     * 把图鉴里的 109 条写进「我的成就」，状态是未完成。
     *
     * 文案按当前语言取，和「从图鉴挑选」产生的记录保持一致；
     * createdDate 按顺序递减，保证列表里的排列和图鉴顺序一致。
     */
    private suspend fun addAllPresets() {
        val presets = runCatching { presetRepository.getAll() }.getOrDefault(emptyList())
        if (presets.isEmpty()) return

        val linked = achievementRepository.getAllAchievements()
            .mapNotNull { it.presetId }
            .toSet()

        val resolver = PresetTextResolver(getApplication<Application>().resources)
        val now = System.currentTimeMillis()

        val rows = presets
            .filterNot { it.id in linked }
            .mapIndexed { index, preset ->
                val text = resolver.textOf(preset)
                Achievement(
                    title = text.title,
                    description = text.description,
                    createdDate = now - index * 1000L,
                    completedDate = null,
                    isCompleted = false,
                    iconEmoji = preset.iconEmoji.ifBlank { AchievementRepository.DEFAULT_ICON },
                    presetId = preset.id
                )
            }

        achievementRepository.insertAchievements(rows)
    }
}
