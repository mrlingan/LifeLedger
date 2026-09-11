package com.Anchored.mylife.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.Anchored.mylife.data.database.PresetAchievement

/** 图鉴条目的一条文案（已按当前系统语言解析） */
data class PresetText(
    val title: String,
    val description: String,
    val story: String
)

/**
 * 图鉴文案的本地化解析器。
 *
 * 数据库里存的仍然是中文原文——它是稳定的「标识」，用来做备份、导入导出和 id 对应。
 * 显示的时候才按系统语言去 string 资源里取：
 * - `values/` 是英文（App 的默认语言）
 * - `values-zh/` 是中文
 *
 * 所以切系统语言立刻生效，不需要迁移数据库，也不会动用户已有的数据。
 *
 * 资源里查不到的 id 返回 null，调用方回退到数据库原文——以后往图鉴里加新条目，
 * 就算忘了补翻译，也只是显示中文，不会崩。
 */
class PresetTextResolver(private val resources: Resources) {

    private val cache = HashMap<Long, PresetText?>()

    fun textOf(id: Long?): PresetText? {
        if (id == null) return null
        if (cache.containsKey(id)) return cache[id]

        val titleRes = PresetStringRes.titleOf(id)
        val descRes = PresetStringRes.descriptionOf(id)
        val storyRes = PresetStringRes.storyOf(id)

        val text = if (titleRes == null || descRes == null || storyRes == null) {
            null
        } else {
            PresetText(
                title = resources.getString(titleRes),
                description = resources.getString(descRes),
                story = resources.getString(storyRes)
            )
        }

        cache[id] = text
        return text
    }

    /** 图鉴条目本身：一定拿得到文案 */
    fun textOf(preset: PresetAchievement): PresetText =
        textOf(preset.id) ?: PresetText(preset.title, preset.description, preset.story)

    /** 分类名：数据库里存的是中文，显示时换成当前语言 */
    fun categoryOf(category: String): String =
        PresetStringRes.categoryOf(category)?.let(resources::getString) ?: category

    /** 首页 / 详情页里的标题，presetId 为空或查不到就用存下来的原文 */
    fun titleOf(presetId: Long?, fallback: String): String = textOf(presetId)?.title ?: fallback

    fun descriptionOf(presetId: Long?, fallback: String): String =
        textOf(presetId)?.description ?: fallback
}

/**
 * 取当前组合的解析器。
 *
 * 用 Configuration 当 remember 的 key：切换系统语言时会被重建，
 * 缓存里的旧语言文案跟着失效。
 */
@Composable
fun rememberPresetTexts(): PresetTextResolver {
    val resources = LocalContext.current.resources
    val configuration = LocalConfiguration.current
    return remember(resources, configuration) { PresetTextResolver(resources) }
}
