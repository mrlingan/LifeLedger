package com.Anchored.mylife.ui

import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.repository.AchievementRepository
import java.util.Calendar

/**
 * 人生数据的共同算法。
 *
 * 首页和「我的」回答的是同一批问题：走到哪一阶段、坚持了多久、图鉴解锁了多少。
 * 这些判断只写这一份——两个页面的数字必须永远对得上，将来改口径也只改一处。
 */

/** 每完成这么多条成就，向上一个阶段 */
internal const val LEVEL_STEP = 5

internal const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

/** 阶段：由累计完成数推导，不是游戏等级 */
internal fun levelOf(completedCount: Int): Int = 1 + completedCount / LEVEL_STEP

/** 距离下一阶段还差几条 */
internal fun toNextLevelCount(completedCount: Int): Int = LEVEL_STEP - completedCount % LEVEL_STEP

/**
 * 当前阶段已经走了多少条。
 *
 * `LEVEL_STEP - toNextLevelCount()`：刚好升到新阶段的那一刻是 0，
 * 走满整个阶段回到 0，和进度条的读法一致。
 */
internal fun levelStepProgress(completedCount: Int): Int =
    LEVEL_STEP - toNextLevelCount(completedCount)

/**
 * 一条属性（分类）的经验值。
 *
 * 每条完成的经历都会记进流水账一笔 [AchievementRepository.COMPLETION_XP]，
 * 属性这一层用的是同一个数——同一条成就，账上是 +50 XP，
 * 它落到的那个属性也是 +50 XP，两处对得上。
 */
internal const val ATTRIBUTE_XP_PER_ACHIEVEMENT = AchievementRepository.COMPLETION_XP

/**
 * 属性升一级需要的经验值。
 *
 * 属性是"分类"这一层的东西，比人生的阶段细一档：阶段是每 5 条一大步，
 * 属性是每 200 XP（也就是 4 条）一小步。这个数是显示口径，不是存下来的等级——
 * 所有属性等级都在 [attributeLevelOf] 里现算，改口径只改这一处。
 */
internal const val ATTRIBUTE_XP_PER_LEVEL = 200

/** 属性等级：每 [ATTRIBUTE_XP_PER_LEVEL] 点经验一级，从 Lv.1 起 */
internal fun attributeLevelOf(xp: Int): Int =
    1 + xp.coerceAtLeast(0) / ATTRIBUTE_XP_PER_LEVEL

/** 属性在当前等级里已经攒了多少经验（0 ..< [ATTRIBUTE_XP_PER_LEVEL]） */
internal fun attributeXpIntoLevel(xp: Int): Int =
    xp.coerceAtLeast(0) % ATTRIBUTE_XP_PER_LEVEL

/**
 * 图鉴已解锁的条目：存在一条 presetId 相同、且已完成的成就。
 *
 * 图鉴页、首页的分类进度、我的页的收集情况都用这一条判定，所以它必须只有一个出处。
 */
internal fun unlockedPresetIds(achievements: List<Achievement>): Set<Long> =
    achievements.filter { it.isCompleted }.mapNotNull { it.presetId }.toSet()

/** 有完成记录的不同天数（累计，只增不减） */
internal fun recordedDays(achievements: List<Achievement>): Int = achievements
    .mapNotNull { it.completedDate }
    .map { startOfDay(it) }
    .toHashSet()
    .size

/**
 * 连续记录天数：把所有完成日期去重后，从今天往前数。
 * 今天还没完成不算断——从昨天开始数，符合"坚持"的真实语义。
 */
internal fun calculateStreak(achievements: List<Achievement>): Int {
    val days = achievements
        .mapNotNull { it.completedDate }
        .map { startOfDay(it) }
        .toHashSet()

    if (days.isEmpty()) return 0

    var cursor = startOfDay(System.currentTimeMillis())
    if (!days.contains(cursor)) {
        cursor -= MILLIS_PER_DAY
    }

    var streak = 0
    while (days.contains(cursor)) {
        streak++
        cursor -= MILLIS_PER_DAY
    }
    return streak
}

/** 当天零点。跨天判断一律按本机时区算。 */
internal fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
