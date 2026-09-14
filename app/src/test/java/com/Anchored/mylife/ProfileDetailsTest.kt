package com.Anchored.mylife

import com.Anchored.mylife.data.profile.Gender
import com.Anchored.mylife.data.profile.MbtiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 资料页「基础信息」两项的落盘约定。
 *
 * 和内置头像（见 [AvatarPresetTest]）一样：存的是枚举名，读回来认不出来就当作"没设过"，
 * 界面回落到「未设置」——手改坏偏好文件、或者以后删掉某个选项，都不能让这一页崩在
 * 一个认不出的值上。
 */
class ProfileDetailsTest {

    @Test
    fun everyGenderSurvivesARoundTrip() {
        Gender.entries.forEach { gender ->
            assertEquals(gender, Gender.fromName(gender.name))
        }
    }

    @Test
    fun everyMbtiTypeSurvivesARoundTrip() {
        MbtiType.entries.forEach { type ->
            assertEquals(type, MbtiType.fromName(type.name))
        }
    }

    @Test
    fun unknownAndMissingNamesFallBackToNothing() {
        assertNull(Gender.fromName(null))
        assertNull(Gender.fromName(""))
        assertNull(Gender.fromName("OTHER"))
        assertNull(MbtiType.fromName(null))
        assertNull(MbtiType.fromName(""))
        assertNull(MbtiType.fromName("XXXX"))
    }

    /**
     * 十六个类型一个不多一个不少。
     *
     * 这四个字母两两交叉正好是 16 种组合，所以这里把组合算出来对着比——
     * 抄错一个字母（比如把 ISTP 写成 ISTJ）或者漏掉一个，这条用例会直接失败。
     */
    @Test
    fun mbtiCoversAllSixteenCombinations() {
        val expected = buildList {
            listOf("I", "E").forEach { first ->
                listOf("S", "N").forEach { second ->
                    listOf("T", "F").forEach { third ->
                        listOf("J", "P").forEach { fourth ->
                            add("$first$second$third$fourth")
                        }
                    }
                }
            }
        }.toSet()

        val actual = MbtiType.entries.map { it.name }
        assertEquals(16, actual.size)
        assertEquals(expected, actual.toSet())
    }
}
