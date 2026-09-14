package com.Anchored.mylife

import com.Anchored.mylife.data.achievement.CategoryCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 分类清单的三源合并。
 *
 * 这几条规则错一个，界面上就会出现"分类重复出现""自建分类消失"
 * 或者"设置里选不到刚建的那个"——都是要用户自己发现的毛病。
 */
class CategoryCatalogTest {

    @Test
    fun presetOrderWinsAndNamesAreDeduped() {
        val merged = CategoryCatalog.merge(
            presetCategories = listOf("成长", "生活"),
            customCategories = listOf("生活", "阅读"),
            usedInAchievements = listOf("阅读", "跑步")
        )

        // 内置的排前面、保持内置的排序；后两类去重后补在后面
        assertEquals(listOf("成长", "生活", "阅读", "跑步"), merged)
    }

    @Test
    fun blankNamesAreIgnored() {
        val merged = CategoryCatalog.merge(
            presetCategories = listOf("成长", "  "),
            customCategories = listOf("", "阅读"),
            usedInAchievements = listOf("   ")
        )

        assertEquals(listOf("成长", "阅读"), merged)
    }

    @Test
    fun achievementOnlyCategoryStillShowsUp() {
        // 偏好文件丢了（换机、只恢复了数据库）时，只靠成就自带的分类也要能重建出清单
        val merged = CategoryCatalog.merge(
            presetCategories = emptyList(),
            customCategories = emptySet(),
            usedInAchievements = listOf("健身")
        )

        assertEquals(listOf("健身"), merged)
    }
}
