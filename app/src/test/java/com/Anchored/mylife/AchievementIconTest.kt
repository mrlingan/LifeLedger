package com.Anchored.mylife

import com.Anchored.mylife.data.achievement.AchievementIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 图标编码的纯 JVM 测试。
 *
 * 这一层之所以值得单测：数据库里只有一列 `iconEmoji`，emoji 和"用户上传的图片"
 * 共用它，全靠 `file:` 前缀区分。前缀一旦对不上，老数据的 emoji 会被当成文件路径，
 * 界面上就只剩一个空框——而且是所有历史记录一起坏。
 */
class AchievementIconTest {

    @Test
    fun emojiStaysEmoji() {
        assertFalse(AchievementIcon.isCustom("🏆"))
        assertNull(AchievementIcon.customPath("🏆"))
        assertNull(AchievementIcon.customPath(""))
    }

    @Test
    fun customValueWrapsAndUnwrapsTheSamePath() {
        val path = "/data/user/0/com.Anchored.mylife/files/icons/icon_1.jpg"
        val icon = AchievementIcon.custom(path)

        assertTrue(AchievementIcon.isCustom(icon))
        assertEquals(path, AchievementIcon.customPath(icon))
    }

    @Test
    fun prefixAloneIsNotEmptyPath() {
        // 前缀后面什么都没有：当成空路径，显示层会回落到默认图标，而不是崩
        assertEquals("", AchievementIcon.customPath("file:"))
    }
}
