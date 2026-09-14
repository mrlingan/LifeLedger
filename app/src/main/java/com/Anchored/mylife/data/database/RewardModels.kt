package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 积分商城里的一个奖励。
 *
 * [isCustom] 区分「内置目录里的」和「用户自己加的」：内置那几条只是起手式，
 * 不喜欢可以直接删掉，删完也不会长回来（见 [com.Anchored.mylife.data.repository.RewardRepository]）。
 *
 * [category] 内置的存分类键（`life` / `fun` …），界面层翻成当前语言的标签；
 * 用户自己新建的分类直接存名字，认不出来的键原样显示——所以自建分类不用另外登记，
 * 只要有一条奖励挂着这个名字，这个分类就存在。
 */
@Entity(
    tableName = "reward_items",
    indices = [Index(value = ["category"])]
)
data class RewardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    /** emoji 图标，和成就图标一个路子 */
    val icon: String,
    val category: String,
    val price: Int,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 一次兑换记录。
 *
 * 标题 / 图标 / 分类 / 价格都是**快照**：奖励条目之后被改价、改名或删掉，
 * 这条记录仍然说得清当时换的是什么、花了多少积分。
 *
 * [refundedAt] 非空表示这次兑换被撤销、积分已经退回。记录本身留着不删——
 * 撤销是"退钱"，不是"没发生过"。
 */
@Entity(
    tableName = "reward_redemptions",
    indices = [Index(value = ["rewardId"]), Index(value = ["redeemedAt"])]
)
data class RewardRedemption(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val title: String,
    val icon: String,
    val category: String,
    val price: Int,
    val redeemedAt: Long = System.currentTimeMillis(),
    val refundedAt: Long? = null
)
