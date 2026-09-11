package com.Anchored.mylife.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 预设成就（成就图鉴），内容来自「地球Online · 人生成就」清单，共 109 条。
 *
 * 为什么单独建一张表，而不是塞进 Achievement：
 * - achievements 表只放用户自己真正记录的人生成就，保持干净；
 * - preset_achievements 是公共的"图鉴"，用户可以浏览、挑选、解锁；
 * - 两条线互不干扰，之后做导入导出也简单。
 *
 * id 直接沿用原数据的 1..109，方便之后逐个补图标时对号入座。
 */
@Entity(tableName = "preset_achievements")
data class PresetAchievement(
    @PrimaryKey
    val id: Long,
    val title: String,
    val description: String,
    val story: String,
    val category: String,
    val rarity: String,
    /** 达成率百分比，例如 4 表示 4% 的人达成 */
    val rate: Double,
    /** 图标：目前留空，由你之后逐个补上 */
    val iconEmoji: String = "",
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null
) {
    companion object {
        const val RARITY_LEGENDARY = "legendary"
        const val RARITY_EPIC = "epic"
        const val RARITY_RARE = "rare"
        const val RARITY_COMMON = "common"

        /** 从稀有到普通，用于排序 */
        val RARITY_ORDER = listOf(RARITY_LEGENDARY, RARITY_EPIC, RARITY_RARE, RARITY_COMMON)

        fun rarityLabel(rarity: String): String = when (rarity) {
            RARITY_LEGENDARY -> "传说"
            RARITY_EPIC -> "史诗"
            RARITY_RARE -> "稀有"
            else -> "普通"
        }

        /** 越小越稀有 */
        fun rarityRank(rarity: String): Int =
            RARITY_ORDER.indexOf(rarity).let { if (it < 0) RARITY_ORDER.size else it }
    }
}
