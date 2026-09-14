package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.data.achievement.CategoryCatalog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 首页那一行最多放五个圈 */
internal const val HOME_CATEGORY_LIMIT = 5

/**
 * 分类这一摊子的几个来源：颜色、用户新建的、挑过要显示的几个、成就上用过的。
 *
 * 打包成一个中间对象，是因为 combine 一次最多接五个流——真正要算的是它们的并集。
 */
private data class CategorySources(
    val colors: Map<String, String>,
    val custom: Set<String>,
    val home: List<String>,
    val used: List<String>
)

/**
 * 首页板块定制的状态。
 *
 * [visible] 的顺序就是首页的显示顺序；[hidden] 只用于在设置页里列出"关掉的那些"，
 * 顺序固定为枚举顺序——重新打开时它们会追加到首页末尾，而不是插回原位。
 *
 * 注意 [hidden] 要按**全部板块**去减，而不是按 [HomeSection.DEFAULT_ORDER]：
 * 默认只开三段，剩下两段（最近解锁 / 记录起点）也得在设置页里列出来才有地方打开。
 */
data class HomeLayoutUiState(
    val visible: List<HomeSection> = HomeSection.DEFAULT_ORDER,
    val hidden: List<HomeSection> = emptyList(),
    /** 当前是否就是默认布局：用来决定「恢复默认」要不要露出来 */
    val isDefault: Boolean = true,
    /** 所有可用的分类：图鉴内置 ∪ 用户新建 ∪ 成就上用过的（见 CategoryCatalog） */
    val categories: List<String> = emptyList(),
    /** 用户新建的分类：只有这些能删掉 */
    val customCategories: Set<String> = emptySet(),
    /** 首页分类进度显示哪几个；顺序就是显示顺序，空 = 还没挑过（按自动排序取前五） */
    val homeCategories: List<String> = emptyList(),
    /** 分类名 → "%08X"，只包含用户改过颜色的那些 */
    val categoryColors: Map<String, String> = emptyMap()
)

/**
 * 首页板块定制。
 *
 * 所有改动都是即时生效的：动一下开关或箭头，回首页就已经是新的样子，
 * 不需要"保存"按钮——这类偏好没有中途取消的语义。
 */
class HomeLayoutViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = RepositoryProvider.get(application).settings

    private val presetRepository = RepositoryProvider.get(application).presetAchievementRepository

    private val repositories = RepositoryProvider.get(application)

    /** 首页自定义图片；null = 还没上传过 */
    val homeImagePath: StateFlow<String?> = settings.homeImagePath

    /** 存下裁剪好的图：先写新文件，写成功了再删旧的（中途失败也不会把现有的图弄丢） */
    fun setHomeImage(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    repositories.homeImageStore.save(
                        bitmap = bitmap,
                        previousPath = settings.homeImagePath.value
                    )
                }.getOrNull()
            }
            if (path != null) settings.setHomeImagePath(path)
        }
    }

    /** 移除：先清偏好（界面立刻不再显示），再删磁盘上的文件 */
    fun clearHomeImage() {
        val previous = settings.homeImagePath.value
        settings.setHomeImagePath(null)
        viewModelScope.launch(Dispatchers.IO) { repositories.homeImageStore.delete(previous) }
    }

    private val categorySources = combine(
        settings.categoryColors,
        settings.customCategories,
        settings.homeCategories,
        RepositoryProvider.get(application).achievementRepository
            .observeAllAchievements()
    ) { colors, custom, home, achievements ->
        CategorySources(
            colors = colors,
            custom = custom,
            home = home,
            used = achievements.map { it.category }.filter { it.isNotBlank() }
        )
    }

    val uiState: StateFlow<HomeLayoutUiState> = combine(
        settings.homeSections,
        categorySources,
        presetRepository.observeAll()
    ) { visible, categories, presets ->
        HomeLayoutUiState(
            visible = visible,
            hidden = HomeSection.entries.filterNot { it in visible },
            isDefault = visible == HomeSection.DEFAULT_ORDER,
            categories = CategoryCatalog.merge(
                presetCategories = presets.groupingBy { it.category }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key },
                customCategories = categories.custom,
                usedInAchievements = categories.used
            ),
            customCategories = categories.custom,
            homeCategories = categories.home,
            categoryColors = categories.colors
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeLayoutUiState()
        )

    /** 给某个分类的圆环挑颜色；传 null 表示回到主题色 */
    fun setCategoryColor(category: String, color: String?) {
        settings.setCategoryColor(category, color)
    }

    /** 新建一个分类：只有名字，用不用得上由用户自己挂到成就上 */
    fun addCategory(name: String) {
        settings.addCustomCategory(name)
    }

    /** 删掉一个自建分类：同时把它从"首页显示"里摘掉，免得留下一个不存在的名字 */
    fun removeCategory(name: String) {
        settings.removeCustomCategory(name)
        if (name in settings.homeCategories.value) {
            settings.setHomeCategories(settings.homeCategories.value - name)
        }
        settings.setCategoryColor(name, null)
    }

    /** 切一个分类要不要出现在首页；超过了 [HOME_CATEGORY_LIMIT] 就不动（界面会把按钮置灰） */
    fun toggleHomeCategory(category: String) {
        val current = settings.homeCategories.value
        settings.setHomeCategories(
            when {
                category in current -> current - category
                current.size >= HOME_CATEGORY_LIMIT -> current
                else -> current + category
            }
        )
    }

    /** 开关一个板块：开着的关掉，关着的排到末尾 */
    fun toggle(section: HomeSection) {
        update { current ->
            if (section in current) current - section else current + section
        }
    }

    /**
     * 拖拽排序：把第 [from] 条挪到第 [to] 条的位置。
     *
     * 只认下标不认板块本身——排序是在"当前这份列表"上做的一次搬运，
     * 拿下标说话最直接，也不用担心同一个板块在列表里出现两次。
     */
    fun move(from: Int, to: Int) {
        update { current ->
            if (from == to || from !in current.indices || to !in current.indices) {
                current
            } else {
                current.toMutableList().apply { add(to, removeAt(from)) }
            }
        }
    }

    /** 恢复默认：回到默认那三段、默认顺序（最近解锁与记录起点重新收起） */
    fun reset() = settings.setHomeSections(HomeSection.DEFAULT_ORDER)

    /** 全部显示：把藏起来的都放回来（含默认关着的那两段），已有的顺序不动 */
    fun showAllHidden() {
        update { current -> current + HomeSection.entries.filterNot { it in current } }
    }

    private fun update(transform: (List<HomeSection>) -> List<HomeSection>) {
        settings.setHomeSections(transform(settings.homeSections.value).distinct())
    }
}
