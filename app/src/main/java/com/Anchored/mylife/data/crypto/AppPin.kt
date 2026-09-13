package com.Anchored.mylife.data.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 应用密码：用户自己设的 4–9 位数字，用来锁住"进应用"这一步。
 *
 * 落盘只保存 PBKDF2 派生值，不保存明文：
 *   v1:<迭代次数>:<盐的十六进制>:<派生值的十六进制>
 *
 * 迭代次数跟备份口令一致（见 [com.Anchored.mylife.data.backup.BackupCrypto]）：
 * 每次解锁慢 100ms 是值得的，它挡的是"拿到 prefs 文件之后离线跑一遍所有组合"。
 * 盐每次重新设置都会换，所以两个人设了同一个密码，落盘值也不一样。
 *
 * 边界要说清楚：这是"进应用"的锁，不是数据加密 ——
 * 它拦不住能在这台设备上跑代码的人（root、调试版），也不参与数据库字段加密，
 * 那件事归 [DataCipher] 管。
 */
object AppPin {

    /** 允许的长度范围，界面上的提示文案要跟这里保持一致 */
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 9

    private const val VERSION = "v1"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    /** 解析时允许的最大迭代数：防止被改坏的落盘值把设备卡死 */
    private const val MAX_ITERATIONS = 2_000_000

    private const val HEX = "0123456789abcdef"

    /** 4–9 位，且每一位都是数字 */
    fun isWellFormed(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    /** 生成落盘值；每次调用都会换一个新盐，所以同一个密码两次结果不同 */
    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val derived = derive(pin, salt, ITERATIONS)
        return "$VERSION:$ITERATIONS:${salt.toHex()}:${derived.toHex()}"
    }

    /**
     * 校验密码。
     *
     * 没设过、格式不认识、值被改过、密码不对 —— 全部返回 false。
     * 比较走 [MessageDigest.isEqual]（定长时间），不按字节提前返回。
     */
    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrEmpty()) return false

        val parts = stored.split(':')
        if (parts.size != 4 || parts[0] != VERSION) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        if (iterations !in 1..MAX_ITERATIONS) return false

        val salt = parts[2].hexToBytes() ?: return false
        val expected = parts[3].hexToBytes() ?: return false
        if (salt.isEmpty() || expected.isEmpty()) return false

        return MessageDigest.isEqual(expected, derive(pin, salt, iterations))
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM)
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (byte in this) {
            val value = byte.toInt() and 0xFF
            out.append(HEX[value ushr 4])
            out.append(HEX[value and 0x0F])
        }
        return out.toString()
    }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        val out = ByteArray(length / 2)
        for (index in out.indices) {
            val high = HEX.indexOf(lowercaseCharAt(index * 2))
            val low = HEX.indexOf(lowercaseCharAt(index * 2 + 1))
            if (high < 0 || low < 0) return null
            out[index] = ((high shl 4) or low).toByte()
        }
        return out
    }

    private fun String.lowercaseCharAt(index: Int): Char = this[index].lowercaseChar()
}
