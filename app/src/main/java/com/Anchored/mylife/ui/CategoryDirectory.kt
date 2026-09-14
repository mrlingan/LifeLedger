package com.Anchored.mylife.ui

import com.Anchored.mylife.data.achievement.CategoryCatalog
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.PresetAchievementRepository
import com.Anchored.mylife.data.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * 可选的分类清单：图鉴内置 + 用户新建 + 自己成就上用过的。
 *
 * 三个来源都是 Flow，这里合成一条给界面订阅。放在一层函数里而不是三个 ViewModel
 * 各写一遍：新建成就、编辑成就、首页板块三处看到的分类必须永远是同一份，
 * 各写一份迟早会对不上（比如新建了一个分类，另一个页面里选不到）。
 */
internal fun categoryDirectoryFlow(
    settings: AppSettings,
    presetRepository: PresetAchievementRepository,
    achievementRepository: AchievementRepository
): Flow<List<String>> =
    combine(
        presetRepository.observeAll(),
        settings.customCategories,
        achievementRepository.observeAllAchievements()
    ) { presets, custom, achievements ->
        CategoryCatalog.merge(
            presetCategories = presets.groupingBy { it.category }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key },
            customCategories = custom,
            usedInAchievements = achievements.map { it.category }.filter { it.isNotBlank() }
        )
    }
