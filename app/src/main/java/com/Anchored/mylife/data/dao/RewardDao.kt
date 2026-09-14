package com.Anchored.mylife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.database.RewardRedemption
import kotlinx.coroutines.flow.Flow

/**
 * 积分商城的数据访问。
 *
 * 只有两张表：奖励目录与兑换记录。余额不在这里——它永远由
 * `point_transactions` 这一本流水账加出来（见 [com.Anchored.mylife.data.repository.PointService]），
 * 商城不自己记一份"我花了多少"，免得两边的数对不上。
 */
@Dao
interface RewardDao {
    @Insert suspend fun insertItem(item: RewardItem): Long
    @Insert suspend fun insertItems(items: List<RewardItem>)
    @Update suspend fun updateItem(item: RewardItem)
    @Query("DELETE FROM reward_items WHERE id = :id") suspend fun deleteItem(id: Long)
    @Query("SELECT * FROM reward_items WHERE id = :id LIMIT 1") suspend fun getItem(id: Long): RewardItem?
    @Query("SELECT COUNT(*) FROM reward_items") suspend fun countItems(): Int

    /** 一次读完（换语言时要把内置那几行的文案对一遍，见 RewardRepository） */
    @Query("SELECT * FROM reward_items") suspend fun listItems(): List<RewardItem>

    /** 便宜的在前面：商城第一屏看到的是"马上换得起"的那几件 */
    @Query("SELECT * FROM reward_items ORDER BY price ASC, id ASC") fun observeItems(): Flow<List<RewardItem>>

    @Insert suspend fun insertRedemption(item: RewardRedemption): Long
    @Update suspend fun updateRedemption(item: RewardRedemption)
    @Query("SELECT * FROM reward_redemptions WHERE id = :id LIMIT 1") suspend fun getRedemption(id: Long): RewardRedemption?

    /** 最近换的在前面 */
    @Query("SELECT * FROM reward_redemptions ORDER BY redeemedAt DESC, id DESC") fun observeRedemptions(): Flow<List<RewardRedemption>>
}
