package com.Anchored.mylife.ui.reward

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.database.RewardRedemption
import com.Anchored.mylife.data.repository.RedeemResult
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.reward.RewardCatalog
import com.Anchored.mylife.ui.LEVEL_STEP
import com.Anchored.mylife.ui.levelOf
import com.Anchored.mylife.ui.toNextLevelCount
import com.Anchored.mylife.ui.xpText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 积分商城的状态。
 *
 * [level] / [toNextLevel] 走的是全 App 同一套「阶段」算法（[levelOf]，来源是
 * 已完成的成就数），不是商城自己另立一个等级：首页、我的、商城三处看到的
 * Lv. 永远是同一个数。积分是钱包，阶段是走到哪了，两件事分开讲。
 */
data class RewardStoreUiState(
    val isLoaded: Boolean = false,

    /** 当前积分余额（由 point_transactions 加出来） */
    val balance: Int = 0,

    val items: List<RewardItem> = emptyList(),
    val redemptions: List<RewardRedemption> = emptyList(),

    // 人生阶段
    val level: Int = 1,
    val toNextLevel: Int = LEVEL_STEP,

    /**
     * 分类标签页的来源：奖励里出现过的分类，固定五类排在前面，自建的按名字排在后面。
     * 没有奖励的分类不会占一个空标签页。
     */
    val categories: List<String> = emptyList()
) {
    /** 还有效的兑换记录（撤销掉的不算） */
    val activeRedemptions: List<RewardRedemption>
        get() = redemptions.filter { it.refundedAt == null }

    /** 累计兑换了多少次 */
    val redeemedCount: Int get() = activeRedemptions.size

    /** 累计花掉多少积分（撤销过的已经退回来，不在这里） */
    val spentTotal: Int get() = activeRedemptions.sumOf { it.price }
}

/**
 * 积分商城。
 *
 * 一次性的提示（换到了 / 还差多少 / 已撤销）走 [message]，由页面弹一条提示条；
 * 用完调 [consumeMessage] 清掉，重进这一页不会把上次那句话再弹一遍。
 */
class RewardStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val rewards = repositories.rewardRepository
    private val achievements = repositories.achievementRepository
    private val appContext = application.applicationContext

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<RewardStoreUiState> = combine(
        rewards.observeItems(),
        rewards.observeRedemptions(),
        rewards.observeBalance(),
        achievements.observeAllAchievements()
    ) { items, redemptions, balance, achievementList ->
        val completed = achievementList.count { it.isCompleted }
        RewardStoreUiState(
            isLoaded = true,
            balance = balance,
            items = items,
            redemptions = redemptions,
            level = levelOf(completed),
            toNextLevel = toNextLevelCount(completed),
            categories = categoriesOf(items)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RewardStoreUiState()
    )

    init {
        // 第一次进来把内置奖励摆上；之后无论用户删没删都不再动
        viewModelScope.launch {
            rewards.seedCatalogOnce(language = currentLanguage())
        }
    }

    /**
     * 内置奖励的标题与描述跟着当前语言走。
     *
     * 它们是落库的，切了语言不会自己变，所以由页面在每次进来时（以及切语言之后）
     * 把这个语言交给 [RewardRepository.syncCatalogLanguage] 对一遍——
     * 语言没变时那边直接返回，不查库。
     */
    fun syncCatalogLanguage(language: String) {
        viewModelScope.launch { rewards.syncCatalogLanguage(language) }
    }

    /**
     * 当前界面语言。
     *
     * 取的是**应用的**资源配置，不是 [java.util.Locale.getDefault]：语言是在设置页
     * 用 AppCompatDelegate 切的应用内语言，系统语言可能完全是另一回事
     * （系统中文 + App 英文就是以前商城里那六条一直是中文的原因）。
     */
    private fun currentLanguage(): String =
        appContext.resources.configuration.locales[0].language

    fun redeem(item: RewardItem) = viewModelScope.launch {
        _message.value = when (val result = rewards.redeem(item.id)) {
            is RedeemResult.Success -> appContext.getString(
                R.string.store_message_redeemed,
                result.title,
                xpText(result.price)
            )

            is RedeemResult.NotEnough -> appContext.getString(
                R.string.store_message_not_enough,
                xpText(result.short)
            )

            RedeemResult.NotFound -> appContext.getString(R.string.store_message_gone)
        }
    }

    fun undoRedemption(redemptionId: Long) = viewModelScope.launch {
        // 退回多少只有那条记录自己知道，所以让仓库把撤掉的那条带回来
        rewards.undoRedemption(redemptionId)?.let { record ->
            _message.value = appContext.getString(
                R.string.store_message_undone,
                record.title,
                xpText(record.price)
            )
        }
    }

    /** 新建或编辑一条自定义奖励；[id] 为 null 表示新建 */
    fun saveCustomReward(
        id: Long?,
        title: String,
        description: String,
        icon: String,
        category: String,
        price: Int
    ) = viewModelScope.launch {
        if (id == null) {
            rewards.addCustomReward(title, description, icon, category, price)
        } else {
            rewards.updateCustomReward(id, title, description, icon, category, price)
        }
        _message.value = appContext.getString(R.string.store_message_saved, title.trim())
    }

    fun deleteReward(id: Long) = viewModelScope.launch {
        rewards.deleteReward(id)
        _message.value = appContext.getString(R.string.store_message_deleted)
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun categoriesOf(items: List<RewardItem>): List<String> {
        val present = items.map { it.category }.filter { it.isNotBlank() }.toSet()
        val builtIn = RewardCatalog.CATEGORY_ORDER.filter { it in present }
        val custom = present.filterNot { it in RewardCatalog.CATEGORY_ORDER }.sorted()
        return builtIn + custom
    }
}
