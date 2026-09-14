package com.Anchored.mylife

import com.Anchored.mylife.data.profile.AvatarPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 内置头像的落盘约定。
 *
 * 存的是枚举名，读回来时认不出来要当成"没挑过"——手改坏偏好文件、
 * 或者以后删掉某个头像，都不能让界面崩在一个空圈上。
 */
class AvatarPresetTest {

    @Test
    fun everyPresetSurvivesARoundTrip() {
        AvatarPreset.entries.forEach { preset ->
            assertEquals(preset, AvatarPreset.fromName(preset.name))
        }
    }

    @Test
    fun unknownAndMissingNamesFallBackToNothing() {
        assertNull(AvatarPreset.fromName(null))
        assertNull(AvatarPreset.fromName(""))
        assertNull(AvatarPreset.fromName("AVATAR_9"))
    }

    @Test
    fun presetNamesAreUniqueAndStable() {
        val names = AvatarPreset.entries.map { it.name }
        assertEquals(names.size, names.toSet().size)
        // 选择器按声明顺序排，所以顺序本身也是对外的一部分
        assertEquals("SUNRISE", names.first())
        assertEquals("PETAL", names.last())
    }
}
