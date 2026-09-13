package com.Anchored.mylife

import com.Anchored.mylife.data.crypto.AppPin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 应用密码的纯 JVM 测试（AppPin 不碰 Android 框架，跑得很快）。
 *
 * 几件事必须成立：长度规则、正确密码能通过、错误密码必须失败、
 * 同一个密码两次落盘结果不同（盐是随机的），以及落盘值里不含明文。
 */
class AppPinTest {

    @Test
    fun lengthRuleIsFourToNineDigits() {
        assertFalse(AppPin.isWellFormed("123"))
        assertFalse(AppPin.isWellFormed("1234567890"))
        assertFalse(AppPin.isWellFormed("12a4"))
        assertFalse(AppPin.isWellFormed("    "))
        assertFalse(AppPin.isWellFormed(""))

        assertTrue(AppPin.isWellFormed("1234"))
        assertTrue(AppPin.isWellFormed("0000"))
        assertTrue(AppPin.isWellFormed("123456789"))
    }

    @Test
    fun correctPinVerifies() {
        val stored = AppPin.hash("2468")
        assertTrue(AppPin.verify("2468", stored))
        assertFalse(AppPin.verify("2469", stored))
        assertFalse(AppPin.verify("", stored))
    }

    @Test
    fun storedValueHidesThePin() {
        val stored = AppPin.hash("135790")
        assertTrue(stored.startsWith("v1:"))
        assertFalse(stored.contains("135790"))
    }

    @Test
    fun samePinHashesDifferently() {
        // 随机盐：同一个人重设一次密码，落盘值也不该一样
        assertNotEquals(AppPin.hash("1234"), AppPin.hash("1234"))
    }

    @Test
    fun malformedStoredValueIsRejectedInsteadOfCrashing() {
        assertFalse(AppPin.verify("1234", null))
        assertFalse(AppPin.verify("1234", ""))
        assertFalse(AppPin.verify("1234", "1234"))
        assertFalse(AppPin.verify("1234", "v1:abc:zz:zz"))
        assertFalse(AppPin.verify("1234", "v2:120000:00:00"))
        // 迭代次数被改大：直接拒绝，不去跑两百万次
        assertFalse(AppPin.verify("1234", "v1:9999999:00ff:00ff"))
    }

    @Test
    fun matchesAnIndependentlyComputedVector() {
        // 盐固定、迭代次数固定，派生值是用 Python 的
        // hashlib.pbkdf2_hmac("sha256", b"2468", salt, 120000) 算出来的：
        // 确认我们对的是标准 PBKDF2，而不是"自己和自己对得上"
        val stored = "v1:120000:9b309bccc37036b8fa02ce46f807e5fd:" +
            "1b70d54693cb0759e703d22f97a5e569469313bc9c16ce549bb34baf201e7774"
        assertTrue(AppPin.verify("2468", stored))
        assertFalse(AppPin.verify("2469", stored))
    }
}
