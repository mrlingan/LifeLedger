package com.Anchored.mylife

import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.ui.ATTRIBUTE_XP_PER_LEVEL
import com.Anchored.mylife.ui.attributeLevelOf
import com.Anchored.mylife.ui.attributesOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 成长页「人生属性」的口径。
 *
 * 这一栏是同一批经历的另一种读法（不是另存一份数据），所以折算规则一旦错了，
 * 用户看到的等级、进度、每条折算多少 XP 就全是错的——这几条测试盯的就是那些数。
 */
class GrowthAttributesTest {

    private fun completed(
        id: Long,
        category: String = "",
        presetId: Long? = null
    ) = Achievement(
        id = id,
        title = "achievement-$id",
        description = "",
        createdDate = 0L,
        completedDate = 1_700_000_000_000L,
        isCompleted = true,
        iconEmoji = "🏆",
        category = category,
        presetId = presetId
    )

    private fun preset(id: Long, category: String) = PresetAchievement(
        id = id,
        title = "preset-$id",
        description = "",
        story = "",
        category = category,
        rarity = PresetAchievement.RARITY_COMMON,
        rate = 10.0
    )

    @Test
    fun achievementsAreGroupedByTheirOwnCategoryMostFirst() {
        val attributes = attributesOf(
            listOf(
                completed(1, category = "技能"),
                completed(2, category = "技能"),
                completed(3, category = "旅行")
            )
        )

        assertEquals(listOf("技能", "旅行"), attributes.map { it.category })
        assertEquals(listOf(2, 1), attributes.map { it.completedCount })
    }

    @Test
    fun missingCategoryFallsBackToTheCodexEntry() {
        // 老数据：成就本身没写分类，但它对应图鉴里的某一条
        val attributes = attributesOf(
            completed = listOf(completed(1, presetId = 7L)),
            presets = listOf(preset(7L, "健康"))
        )

        assertEquals(listOf("健康"), attributes.map { it.category })
    }

    @Test
    fun ownCategoryWinsOverTheCodexEntry() {
        val attributes = attributesOf(
            completed = listOf(completed(1, category = "跑步", presetId = 7L)),
            presets = listOf(preset(7L, "健康"))
        )

        assertEquals(listOf("跑步"), attributes.map { it.category })
    }

    @Test
    fun achievementsWithoutAnyCategoryAreSkipped() {
        val attributes = attributesOf(
            listOf(
                completed(1, presetId = 999L),
                completed(2)
            )
        )

        assertEquals(emptyList<String>(), attributes.map { it.category })
    }

    @Test
    fun xpAndLevelFollowTheSharedRule() {
        // 5 条同分类 = 250 XP：Lv.2 的第 50 点经验
        val attribute = attributesOf(
            (1L..5L).map { completed(it, category = "技能") }
        ).single()

        assertEquals(5 * 50, attribute.xp)
        assertEquals(2, attribute.level)
        assertEquals(50, attribute.xpIntoLevel)
        assertEquals(50f / ATTRIBUTE_XP_PER_LEVEL, attribute.levelProgress, 0.0001f)
        assertEquals(2, attributeLevelOf(attribute.xp))
    }
}
