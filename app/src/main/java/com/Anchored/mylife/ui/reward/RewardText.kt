package com.Anchored.mylife.ui.reward

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.data.reward.RewardCatalog

/**
 * 奖励分类键 -> 当前语言的标签。
 *
 * 认不出来的键原样返回：用户自己新建的分类就是这个名字，不需要另外登记
 * （只要有一条奖励挂着它，这个分类就存在，和成就分类是同一个做法）。
 */
@Composable
internal fun rewardCategoryLabel(key: String): String = when (key) {
    RewardCatalog.CATEGORY_LIFE -> stringResource(R.string.store_category_life)
    RewardCatalog.CATEGORY_FUN -> stringResource(R.string.store_category_fun)
    RewardCatalog.CATEGORY_TRAVEL -> stringResource(R.string.store_category_travel)
    RewardCatalog.CATEGORY_STUDY -> stringResource(R.string.store_category_study)
    RewardCatalog.CATEGORY_DIGITAL -> stringResource(R.string.store_category_digital)
    else -> key
}

/**
 * 当前界面语言，收敛成目录认的两个值（`zh` / `en`）。
 *
 * 用 Configuration 当 remember 的 key：切语言时会被重建，拿到的是新的那个。
 * 内置奖励的文案是落库的，商城拿这个值去把内置那几行对一遍（见 RewardStoreRoute）。
 */
@Composable
internal fun rememberRewardLanguage(): String {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        RewardCatalog.languageOf(configuration.locales[0].language)
    }
}

/**
 * 记账 / 兑换记录里那条标题，换成当前语言的写法。
 *
 * 流水与兑换记录存的都是记账当时的原文（那是对历史负责的快照），但内置奖励是
 * "目录里那一条"，按标题认出来就能显示成当前语言——和成长记录里图鉴成就标题的
 * 做法一致。认不出来（用户自己写的奖励）就原样返回。
 */
internal fun rewardTitleIn(title: String, language: String): String =
    RewardCatalog.entryOfTitle(title)?.title(language) ?: title

/** [rewardTitleIn] 的组合版：跟着当前语言变 */
@Composable
internal fun rememberRewardTitle(title: String): String {
    val language = rememberRewardLanguage()
    return remember(title, language) { rewardTitleIn(title, language) }
}

/**
 * 自定义奖励可以挑的图标。
 *
 * 用 emoji 而不是矢量图标：和成就图标是同一套语言（一个字符就是一个图标），
 * 不用为商城再引进一套图标资源，用户也一眼知道自己能挑什么。
 */
internal val RewardEmojiChoices = listOf(
    "🎁", "☕", "🍜", "🍰", "🛋️", "🌙", "💐", "🎬",
    "🎮", "🎧", "📚", "💻", "✈️", "🏝️", "👟", "📷"
)
