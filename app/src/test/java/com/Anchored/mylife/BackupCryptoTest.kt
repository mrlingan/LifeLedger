package com.Anchored.mylife

import com.Anchored.mylife.data.backup.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 加密备份的纯 JVM 测试：不碰 Android 框架，跑起来很快。
 *
 * 三件事必须成立：正确口令能还原、口令错了必须失败（而不是还原出垃圾）、
 * 文件头能一眼认出是不是加密的。
 */
class BackupCryptoTest {

    private val payload = "人生账本 backup payload".toByteArray(Charsets.UTF_8)

    @Test
    fun roundTripWithCorrectPassphrase() {
        val encrypted = ByteArrayOutputStream().also { out ->
            BackupCrypto.encryptedOutput(out, "correct horse".toCharArray()).use { crypto ->
                crypto.write(payload)
            }
        }.toByteArray()

        // 密文里不应该出现明文
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("人生账本"))
        assertTrue(BackupCrypto.isEncrypted(encrypted.copyOfRange(0, 8)))

        val restored = ByteArrayOutputStream().also { out ->
            BackupCrypto.decrypt(ByteArrayInputStream(encrypted), out, "correct horse".toCharArray())
        }.toByteArray()

        assertArrayEquals(payload, restored)
    }

    @Test(expected = Exception::class)
    fun wrongPassphraseFails() {
        val encrypted = ByteArrayOutputStream().also { out ->
            BackupCrypto.encryptedOutput(out, "correct horse".toCharArray()).use { crypto ->
                crypto.write(payload)
            }
        }.toByteArray()

        BackupCrypto.decrypt(
            ByteArrayInputStream(encrypted),
            ByteArrayOutputStream(),
            "wrong horse".toCharArray()
        )
    }

    @Test
    fun plainZipIsNotDetectedAsEncrypted() {
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00)
        assertFalse(BackupCrypto.isEncrypted(zipHeader))
    }
}
