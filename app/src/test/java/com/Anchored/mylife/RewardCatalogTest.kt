package com.Anchored.mylife

import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.reward.RewardCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 内置奖励目录。
 *
 * 这六条是"第一次进商城看到什么"，错一个都会表现成很具体的毛病：
 * 分类键拼错 → 某个标签页里空着；价格写成 0 → 白送；
 * 某一条漏了英文 → 英文界面里夹一句中文；
 * 认不出自己摆进去的那行 → 切了语言商城里还是老语言（这个 bug 修过了，见下面几个用例）。
 */
class RewardCatalogTest {

    @Test
    fun defaultsArePricedAndUseBuiltInCategories() {
        val items = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_ZH)

        assertEquals(6, items.size)
        items.forEach { item ->
            assertTrue("价格必须为正：${item.title}", item.price > 0)
            assertTrue("图标不能为空：${item.title}", item.icon.isNotBlank())
            assertTrue("标题不能为空", item.title.isNotBlank())
            assertTrue(
                "分类必须是内置分类键：${item.category}",
                item.category in RewardCatalog.CATEGORY_ORDER
            )
            assertTrue("内置条目不是自定义奖励", !item.isCustom)
        }
    }

    @Test
    fun pricesAscendSoCheapestComesFirst() {
        val prices = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_EN).map { it.price }

        // 商城的排序就是价格升序，目录本身也得是这个顺序
        assertEquals(prices.sorted(), prices)
        assertEquals(prices.size, prices.toSet().size)
    }

    @Test
    fun bothLanguagesCoverEveryItem() {
        val chinese = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_ZH)
        val english = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_EN)

        assertEquals(chinese.map { it.category }, english.map { it.category })
        assertEquals(chinese.map { it.price }, english.map { it.price })
        assertTrue(chinese.none { it.title.isBlank() || it.description.isBlank() })
        assertTrue(english.none { it.title.isBlank() || it.description.isBlank() })
        // 漏翻的话中英会一模一样，这里直接把它挡下来
        assertNotEquals(chinese.map { it.title }, english.map { it.title })
    }

    @Test
    fun languageTagsNormaliseToChineseOrEnglish() {
        assertEquals(RewardCatalog.LANGUAGE_ZH, RewardCatalog.languageOf("zh"))
        // 带地区的标签也得认（`zh-Hans-CN` 这种）
        assertEquals(RewardCatalog.LANGUAGE_ZH, RewardCatalog.languageOf("zh-Hans-CN"))
        assertEquals(RewardCatalog.LANGUAGE_EN, RewardCatalog.languageOf("en"))
        assertEquals(RewardCatalog.LANGUAGE_EN, RewardCatalog.languageOf("ja"))
        // 没设过语言（跟随系统但读不到）时按英文算，和目录里"非中文一律英文"一致
        assertEquals(RewardCatalog.LANGUAGE_EN, RewardCatalog.languageOf(null))
    }

    /**
     * 修的就是这个：中文落的库，切到英文之后那六行得认出来、换成英文文案。
     */
    @Test
    fun builtInRowsAreRecognisedInEitherLanguage() {
        val seededInChinese = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_ZH)
        val english = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_EN)

        seededInChinese.zip(english).forEach { (chineseRow, englishRow) ->
            val text = RewardCatalog.textIn(chineseRow, RewardCatalog.LANGUAGE_EN)
            assertEquals(englishRow.title, text?.title)
            assertEquals(englishRow.description, text?.description)

            // 反过来也得认：英文落的库切到中文
            val backToChinese = RewardCatalog.textIn(englishRow, RewardCatalog.LANGUAGE_ZH)
            assertEquals(chineseRow.title, backToChinese?.title)
            assertEquals(chineseRow.description, backToChinese?.description)
        }
    }

    /**
     * 目录调过价之后的老数据：按标题原文也认得出是哪一条。
     */
    @Test
    fun rowsWithAnOldPriceAreStillRecognisedByTheirTitle() {
        val old = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_ZH)
            .first { it.price == 100 }
            .copy(price = 120)

        assertEquals(
            "A coffee you actually like",
            RewardCatalog.textIn(old, RewardCatalog.LANGUAGE_EN)?.title
        )
    }

    @Test
    fun customAndUnknownRowsAreLeftAlone() {
        // 用户自己写的奖励：价格恰好撞上内置那条也不许动
        val custom = RewardItem(
            title = "给自己放半天假",
            description = "什么都不干。",
            icon = "🛋️",
            category = RewardCatalog.CATEGORY_LIFE,
            price = 100,
            isCustom = true
        )
        assertNull(RewardCatalog.textIn(custom, RewardCatalog.LANGUAGE_EN))

        // 认不出来的（比如以后目录里删掉的那条）：原样留着，不猜
        val unknown = RewardItem(
            title = "以前目录里的一条",
            description = "",
            icon = "🎁",
            category = RewardCatalog.CATEGORY_LIFE,
            price = 700,
            isCustom = false
        )
        assertNull(RewardCatalog.textIn(unknown, RewardCatalog.LANGUAGE_EN))
    }

    @Test
    fun entryLookupIsUnambiguous() {
        // 认行的两个依据都不能有歧义：(分类, 价格) 与标题，各自在目录里唯一
        val keys = RewardCatalog.ENTRIES.map { it.category to it.price }
        assertEquals(keys.size, keys.toSet().size)

        val titles = RewardCatalog.ENTRIES.map { it.zhTitle } +
            RewardCatalog.ENTRIES.map { it.enTitle }
        assertEquals(titles.size, titles.toSet().size)
    }

    /**
     * 积分流水与兑换记录里存的是记账当时的标题原文，没有 id 可查——
     * 按标题也要认得出是哪一条，界面才能把它换成当前语言的写法。
     */
    @Test
    fun titlesFromTheLedgerAreRecognisedToo() {
        RewardCatalog.ENTRIES.forEach { entry ->
            assertEquals(entry, RewardCatalog.entryOfTitle(entry.zhTitle))
            assertEquals(entry, RewardCatalog.entryOfTitle(entry.enTitle))
        }

        // 用户自己写的奖励：认不出来，原样显示
        assertNull(RewardCatalog.entryOfTitle("给自己放半天假"))
    }
}
