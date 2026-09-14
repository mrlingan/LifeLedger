package com.Anchored.mylife.data.reward

import com.Anchored.mylife.data.database.RewardItem

/**
 * 内置奖励目录。
 *
 * 和成就图鉴是同一个思路：先摆一份「不知道写什么就先照着这个来」的清单，
 * 用户不高兴就删掉——商城不是图鉴，没有"必须收齐"的说法。
 *
 * 分类固定五个（生活 / 娱乐 / 旅行 / 学习 / 数码），键与标签分开：
 * 键进数据库，标签在界面层按当前语言取，所以换语言时历史记录里的分类也跟着变。
 * 用户自建的分类不走这张表，直接以名字当键。
 *
 * 标题与描述是**落库**的（商城列的是表里的行，兑换记录还存了一份快照），
 * 所以换语言不会自己生效：进商城时拿 [textIn] 对一遍，只把内置那几行改成当前语言
 * （见 [com.Anchored.mylife.data.repository.RewardRepository.syncCatalogLanguage]）。
 * 用户自己写的、以及已经换过的那些快照一律不碰。
 */
object RewardCatalog {

    const val CATEGORY_LIFE = "life"
    const val CATEGORY_FUN = "fun"
    const val CATEGORY_TRAVEL = "travel"
    const val CATEGORY_STUDY = "study"
    const val CATEGORY_DIGITAL = "digital"

    /** 标签页的固定顺序：这几个先出现，用户自建的排后面 */
    val CATEGORY_ORDER = listOf(
        CATEGORY_LIFE,
        CATEGORY_FUN,
        CATEGORY_TRAVEL,
        CATEGORY_STUDY,
        CATEGORY_DIGITAL
    )

    /** 自定义奖励没挑图标时用它 */
    const val FALLBACK_ICON = "🎁"

    /** 自定义奖励的价格上限：再奢侈也够用了，同时挡住误输入的长数字 */
    const val PRICE_MAX = 1_000_000

    /** 文案只有中英两套：非中文一律走英文（和预设成就的判断口径一样） */
    const val LANGUAGE_ZH = "zh"
    const val LANGUAGE_EN = "en"

    /** 语言标签 -> 目录认的语言。`zh-Hans-CN` 这种也认，认不出来的一律当英文 */
    fun languageOf(tag: String?): String =
        if (tag?.startsWith(LANGUAGE_ZH) == true) LANGUAGE_ZH else LANGUAGE_EN

    /**
     * 一条内置奖励。
     *
     * 分类 / 图标 / 价格是固定的（用户改不了内置条目，想要不一样的自己加一条），
     * 只有标题与描述分中英两份。
     */
    data class Entry(
        val category: String,
        val icon: String,
        val price: Int,
        val zhTitle: String,
        val zhDescription: String,
        val enTitle: String,
        val enDescription: String
    ) {
        fun title(language: String): String =
            if (language == LANGUAGE_ZH) zhTitle else enTitle

        fun description(language: String): String =
            if (language == LANGUAGE_ZH) zhDescription else enDescription
    }

    /** 一条内置奖励在某种语言下的文案 */
    data class Text(val title: String, val description: String)

    /** 内置目录，价格升序：商城第一屏看到的是"马上换得起"的那几件 */
    val ENTRIES: List<Entry> = listOf(
        Entry(
            category = CATEGORY_LIFE,
            icon = "☕",
            price = 100,
            zhTitle = "买一杯喜欢的咖啡",
            zhDescription = "奖励一下努力的自己。",
            enTitle = "A coffee you actually like",
            enDescription = "Reward yourself for the effort."
        ),
        Entry(
            category = CATEGORY_FUN,
            icon = "🎮",
            price = 500,
            zhTitle = "玩一晚上游戏",
            zhDescription = "让自己放松一下。",
            enTitle = "A whole evening of gaming",
            enDescription = "Let yourself unwind."
        ),
        Entry(
            category = CATEGORY_LIFE,
            icon = "🍜",
            price = 1000,
            zhTitle = "吃一顿想吃很久的东西",
            zhDescription = "是时候犒劳自己了。",
            enTitle = "That meal you've been craving",
            enDescription = "Time to treat yourself."
        ),
        Entry(
            category = CATEGORY_TRAVEL,
            icon = "✈️",
            price = 3000,
            zhTitle = "去一次短途旅行",
            zhDescription = "看看不一样的风景。",
            enTitle = "A short trip somewhere new",
            enDescription = "Go see a different view."
        ),
        Entry(
            category = CATEGORY_DIGITAL,
            icon = "🎧",
            price = 5000,
            zhTitle = "买一副新耳机",
            zhDescription = "用更好的声音，记录生活。",
            enTitle = "A new pair of headphones",
            enDescription = "Better sound for the life you record."
        ),
        Entry(
            category = CATEGORY_STUDY,
            icon = "💻",
            price = 10000,
            zhTitle = "升级一台电脑",
            zhDescription = "更高效地完成自己的目标。",
            enTitle = "Upgrade your computer",
            enDescription = "Get to your goals faster."
        )
    )

    /**
     * 内置奖励，按 [language] 取文案。
     *
     * 只用在"第一次进商城把目录摆进去"这一步；之后的行都是数据库里的，
     * 换语言靠 [textIn] 逐行改（[com.Anchored.mylife.data.repository.RewardRepository.syncCatalogLanguage]）。
     */
    fun defaultItems(language: String): List<RewardItem> = ENTRIES.map { entry ->
        RewardItem(
            title = entry.title(language),
            description = entry.description(language),
            icon = entry.icon,
            category = entry.category,
            price = entry.price,
            isCustom = false
        )
    }

    /**
     * 某一行是不是内置目录里的那条；认不出来返回 null（那一行原样不动）。
     *
     * 认法是 (分类, 价格)：内置六条各占一个组合，而内置条目的价格只由目录决定——
     * 用户改不了内置条目的价格，想要不一样的得自己新建一条，那行 `isCustom = true`，
     * 这里根本不看。先按组合认，认不到再退回按标题原文认：这样目录哪天调了价，
     * 用户库里那几行老数据照样认得出来。
     */
    fun entryOf(item: RewardItem): Entry? {
        if (item.isCustom) return null
        ENTRIES.firstOrNull { it.category == item.category && it.price == item.price }
            ?.let { return it }
        return ENTRIES.firstOrNull {
            it.category == item.category && (it.zhTitle == item.title || it.enTitle == item.title)
        }
    }

    /**
     * 按标题原文认内置条目。
     *
     * 积分流水与兑换记录存的是**记账当时的原文**，没有 id 可查，只能按标题认——
     * 认出来界面就换成当前语言的写法，认不出来（用户自己写的奖励）就原样显示。
     */
    fun entryOfTitle(title: String): Entry? =
        ENTRIES.firstOrNull { it.zhTitle == title || it.enTitle == title }

    /**
     * 一行内置奖励在 [language] 下该有的文案；不是内置的（或者认不出来）返回 null。
     */
    fun textIn(item: RewardItem, language: String): Text? {
        val entry = entryOf(item) ?: return null
        return Text(entry.title(language), entry.description(language))
    }
}
