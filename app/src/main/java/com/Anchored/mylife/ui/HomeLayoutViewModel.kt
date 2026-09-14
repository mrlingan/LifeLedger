package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.data.settings.HomeSectionEntry
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val visible: List<HomeSectionEntry> = HomeSection.DEFAULT_ENTRIES,
    /** 首页上一个实例都没有的类型（列在设置页下面，⊕ / 开关能放回来） */
    val hidden: List<HomeSection> = emptyList(),
    /** 当前是否就是默认布局：用来决定「恢复默认」要不要露出来 */
    val isDefault: Boolean = true,
    /** 板块实例 id → 配图绝对路径（自定义图片用） */
    val images: Map<String, String> = emptyMap()
)

/**
 * 首页板块定制。
 *
 * 所有改动都是即时生效的：动一下开关或箭头，回首页就已经是新的样子，
 * 不需要"保存"按钮——这类偏好没有中途取消的语义。
 */
class HomeLayoutViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = RepositoryProvider.get(application).settings

    private val repositories = RepositoryProvider.get(application)

    /**
     * 给某一份实例配图：相册挑的那张**原图**直接复制进私有目录，中间不过裁剪。
     *
     * 存的是私有目录里的绝对路径，不是相册给的 content:// 地址——那个读取凭证
     * 在用户清空相册、卸载图库、换手机之后就失效了（见 ImageDirectoryStore）。
     * 换图时先写新文件、成功了再删旧的，中途失败也不会把现有的图弄丢。
     */
    fun setImage(id: String, uri: android.net.Uri) {
        viewModelScope.launch {
            val previous = settings.homeSectionImages.value[id]
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    repositories.homeImageStore.replace(uri = uri, previousPath = previous)
                }.getOrNull()
            }
            if (path != null) settings.setHomeSectionImage(id, path)
        }
    }

    /**
     * 摘掉某一份的图：先清偏好（界面立刻不再显示），再删磁盘上的文件。
     *
     * 注意这跟"把这块从首页拿走"不是一回事：拿走（开关关掉 / 编辑态 ⊖）**不动配图**，
     * 放回来时那张图还在——和以前单张图片时的行为一致。只有这个明确的「移除图片」
     * 才会真的删文件。
     */
    fun clearImage(id: String) {
        val previous = settings.homeSectionImages.value[id]
        settings.setHomeSectionImage(id, null)
        viewModelScope.launch(Dispatchers.IO) { repositories.homeImageStore.delete(previous) }
    }

    val uiState: StateFlow<HomeLayoutUiState> = combine(
        settings.homeSections,
        settings.homeSectionImages
    ) { visible, images ->
        HomeLayoutUiState(
            visible = visible,
            hidden = HomeSection.entries.filterNot { type -> visible.any { it.type == type } },
            isDefault = visible == HomeSection.DEFAULT_ENTRIES,
            images = images
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeLayoutUiState()
        )

    /**
     * 开关一个板块类型：这类在首页还有实例就全摘掉，一个都没有就放回第一份。
     *
     * 配图不动：关掉再打开，那张图还在（见 [clearImage] 的说明）。
     */
    fun toggle(section: HomeSection) {
        update { current ->
            if (current.any { it.type == section }) {
                current.filterNot { it.type == section }
            } else {
                current + HomeSectionEntry.primary(section)
            }
        }
    }

    /**
     * ＋ 添加一份板块。
     *
     * 只接受白名单里的类型（[HomeSection.REPEATABLE]，目前只有自定义图片）：
     * 「人生进度」这类全局汇总摆两份只是同样的数字画两遍，界面也不会把它们列出来。
     */
    fun addSection(section: HomeSection) {
        if (section !in HomeSection.REPEATABLE) return
        update { current -> current + HomeSectionEntry.next(section, current) }
    }

    /** 拿走某一份实例（编辑态的 ⊖ / 非编辑态的开关）：配图保留，放回来还在 */
    fun removeInstance(id: String) {
        update { current -> current.filterNot { it.id == id } }
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
    fun reset() = settings.setHomeSections(HomeSection.DEFAULT_ENTRIES)

    /** 全部显示：把藏起来的都放回来（含默认关着的那两段），已有的顺序不动 */
    fun showAllHidden() {
        update { current ->
            current + HomeSection.entries
                .filterNot { type -> current.any { it.type == type } }
                .map { HomeSectionEntry.primary(it) }
        }
    }

    private fun update(transform: (List<HomeSectionEntry>) -> List<HomeSectionEntry>) {
        settings.setHomeSections(transform(settings.homeSections.value).distinctBy { it.id })
    }
}
