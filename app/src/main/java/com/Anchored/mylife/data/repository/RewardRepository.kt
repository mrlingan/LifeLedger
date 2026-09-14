package com.Anchored.mylife.data.repository

import androidx.room.withTransaction
import com.Anchored.mylife.data.dao.RewardDao
import com.Anchored.mylife.data.database.AchievementDatabase
import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.database.RewardRedemption
import com.Anchored.mylife.data.database.PointType
import com.Anchored.mylife.data.reward.RewardCatalog
import com.Anchored.mylife.data.settings.AppSettings
import kotlinx.coroutines.flow.Flow

/** 一次兑换的结果。界面只需要知道"成了没有、为什么没成"。 */
sealed interface RedeemResult {
    /** 换到了：[title] 与 [price] 用来给用户回一句话 */
    data class Success(val title: String, val price: Int) : RedeemResult

    /** 积分不够，还差 [short] */
    data class NotEnough(val short: Int) : RedeemResult

    /** 这件奖励已经不在了（另一个页面刚删掉） */
    data object NotFound : RedeemResult
}

/**
 * 积分商城。
 *
 * 三条规则决定了这一层的写法：
 *
 * 1. **余额只认流水账**。花积分不是"改一个余额字段"，而是往
 *    `point_transactions` 里记一笔负数（[PointService.record]），余额永远由它加出来。
 *    所以撤销兑换记的是一笔正数，而不是把原来那笔删掉——账本只追加，不涂改。
 * 2. **每一步都幂等**。流水账的 sourceId 带上兑换记录自己的 id
 *    （`reward:redeem:12`），重复点、进程被杀之后重来，都只记一笔。
 * 3. **读余额要在同一个事务里**。判断"够不够"和写兑换记录必须一起成功或一起失败，
 *    否则连点两下就能用同一笔积分换两件东西。
 */
class RewardRepository(
    private val database: AchievementDatabase,
    private val dao: RewardDao,
    private val points: PointService,
    private val settings: AppSettings
) {

    fun observeItems(): Flow<List<RewardItem>> = dao.observeItems()

    fun observeRedemptions(): Flow<List<RewardRedemption>> = dao.observeRedemptions()

    fun observeBalance(): Flow<Int> = points.observeBalance()

    /**
     * 第一次进商城时把内置目录摆进来，**只做一次**。
     *
     * 靠偏好里的一个标记而不是"表是空的"来判断：用户把内置的那几条全删了，
     * 那是他的选择，下次进来不该又给他长回来。
     *
     * @param language 当前界面语言（`zh` / `en`，别的标签按英文算）；
     *   文案按这个落库，之后由 [syncCatalogLanguage] 跟着语言走
     */
    suspend fun seedCatalogOnce(language: String) {
        if (settings.rewardCatalogSeeded.value) return
        val normalized = RewardCatalog.languageOf(language)
        database.withTransaction {
            if (dao.countItems() == 0) {
                dao.insertItems(RewardCatalog.defaultItems(normalized))
            }
            settings.setRewardCatalogSeeded(true)
            settings.setRewardCatalogLanguage(normalized)
        }
    }

    /**
     * 让内置目录的标题与描述跟上当前语言。
     *
     * 内置那几行是**落库**的，所以切了语言它不会自己变——以前的表现就是
     * "App 切成英文，商城里还是六条中文"。这里在进商城时对一遍：只动内置条目
     * （`isCustom = false`，认法见 [RewardCatalog.entryOf]），用户自己写的奖励、
     * 以及「我的奖励」里已经换过的那些快照一律不碰——那是历史，不该被改写。
     *
     * 语言没变就直接返回，不进事务也不查库，所以每次进商城几乎不花什么。
     */
    suspend fun syncCatalogLanguage(language: String) {
        val normalized = RewardCatalog.languageOf(language)
        if (settings.rewardCatalogLanguage.value == normalized) return
        database.withTransaction {
            dao.listItems()
                .filterNot { it.isCustom }
                .forEach { item ->
                    val text = RewardCatalog.textIn(item, normalized) ?: return@forEach
                    if (text.title != item.title || text.description != item.description) {
                        dao.updateItem(item.copy(title = text.title, description = text.description))
                    }
                }
            settings.setRewardCatalogLanguage(normalized)
        }
    }

    /** 加一条自定义奖励，返回它的 id */
    suspend fun addCustomReward(
        title: String,
        description: String,
        icon: String,
        category: String,
        price: Int
    ): Long = dao.insertItem(
        RewardItem(
            title = title.trim(),
            description = description.trim(),
            icon = icon.ifBlank { RewardCatalog.FALLBACK_ICON },
            category = category.trim(),
            price = price.coerceIn(1, RewardCatalog.PRICE_MAX),
            isCustom = true
        )
    )

    /**
     * 改一条奖励。
     *
     * 只给自定义奖励用：内置目录是"照着这个来"的样板，改它没有意义，
     * 想要不一样的自己加一条就是了（删掉内置的也可以）。
     */
    suspend fun updateCustomReward(
        id: Long,
        title: String,
        description: String,
        icon: String,
        category: String,
        price: Int
    ) {
        val current = dao.getItem(id) ?: return
        dao.updateItem(
            current.copy(
                title = title.trim(),
                description = description.trim(),
                icon = icon.ifBlank { RewardCatalog.FALLBACK_ICON },
                category = category.trim(),
                price = price.coerceIn(1, RewardCatalog.PRICE_MAX)
            )
        )
    }

    /** 删一条奖励。已经换过的那些记录不受影响——它们存的是快照。 */
    suspend fun deleteReward(id: Long) = dao.deleteItem(id)

    /**
     * 兑换。
     *
     * 余额是在事务里现读的：读到的就是上一步写完之后的账，所以连点两下时
     * 第二下会看到扣完的余额，不够就自然失败。
     */
    suspend fun redeem(rewardId: Long): RedeemResult = database.withTransaction {
        val item = dao.getItem(rewardId) ?: return@withTransaction RedeemResult.NotFound
        val balance = points.balance()
        if (balance < item.price) {
            return@withTransaction RedeemResult.NotEnough(short = item.price - balance)
        }

        val redemptionId = dao.insertRedemption(
            RewardRedemption(
                rewardId = item.id,
                title = item.title,
                icon = item.icon,
                category = item.category,
                price = item.price
            )
        )
        points.record(
            amount = -item.price,
                type = PointType.REWARD_REDEEM,
            sourceId = "$SOURCE_REDEEM$redemptionId",
            description = item.title
        )
        RedeemResult.Success(title = item.title, price = item.price)
    }

    /**
     * 撤销一次兑换，把积分退回去。
     *
     * 记录留着、只标 [RewardRedemption.refundedAt]：这样"换过又退掉"这件事在
     * 历史里看得见，重复点撤销也不会退两次（第二遍直接返回 null，不再记账）。
     *
     * 返回被撤销的那条记录，调用方拿它组提示文案；没撤销成（记录不存在、
     * 或者早就退过了）返回 null。
     */
    suspend fun undoRedemption(redemptionId: Long): RewardRedemption? = database.withTransaction {
        val record = dao.getRedemption(redemptionId) ?: return@withTransaction null
        if (record.refundedAt != null) return@withTransaction null

        dao.updateRedemption(record.copy(refundedAt = System.currentTimeMillis()))
        points.record(
            amount = record.price,
            type = PointType.REWARD_REFUND,
            sourceId = "$SOURCE_REFUND$redemptionId",
            description = record.title
        )
        record
    }

    companion object {
        private const val SOURCE_REDEEM = "reward:redeem:"
        private const val SOURCE_REFUND = "reward:refund:"
    }
}
