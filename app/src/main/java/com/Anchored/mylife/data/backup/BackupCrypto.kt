package com.Anchored.mylife.data.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份文件加密。
 *
 * 只加密"会离开手机"的东西——导出的备份 zip。库里的数据不动：
 * 它本来就躺在 Android 的文件级加密 + 应用沙箱里，而备份经常被放进网盘，
 * 那才是真正需要口令的地方。
 *
 * 文件格式（自描述，导入时读头 8 字节就知道是不是加密的）：
 *   [8B magic "LIFELED1"][16B salt][12B iv][AES-256-GCM 密文 + 16B tag]
 *
 * 口令经 PBKDF2-HMAC-SHA256 迭代 120000 次派生 256 位密钥；GCM 自带完整性校验，
 * 口令错了或者文件被动过，解密都会失败而不是吐出一堆垃圾。
 */
object BackupCrypto {

    private const val MAGIC_TEXT = "LIFELED1"
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    val MAGIC: ByteArray = MAGIC_TEXT.toByteArray(Charsets.US_ASCII)
    val HEADER_LENGTH = MAGIC.size + SALT_LENGTH + IV_LENGTH

    /** 在裸输出流之上套一层加密，并把头部写出去 */
    fun encryptedOutput(output: OutputStream, passphrase: CharArray): OutputStream {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        output.write(MAGIC)
        output.write(salt)
        output.write(iv)
        return CipherOutputStream(output, cipher(Cipher.ENCRYPT_MODE, passphrase, salt, iv))
    }

    /** 读取并解密：先吃掉头部，再按同一套参数解流 */
    fun decrypt(input: InputStream, output: OutputStream, passphrase: CharArray) {
        val header = ByteArray(HEADER_LENGTH)
        var read = 0
        while (read < header.size) {
            val count = input.read(header, read, header.size - read)
            if (count < 0) error("加密备份不完整")
            read += count
        }
        require(header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) { "不是加密备份" }

        val salt = header.copyOfRange(MAGIC.size, MAGIC.size + SALT_LENGTH)
        val iv = header.copyOfRange(MAGIC.size + SALT_LENGTH, header.size)
        CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, passphrase, salt, iv)).use { plain ->
            plain.copyTo(output)
        }
    }

    /** 只凭开头几个字节判断是不是加密备份 */
    fun isEncrypted(header: ByteArray): Boolean =
        header.size >= MAGIC.size && header.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    private fun cipher(
        mode: Int,
        passphrase: CharArray,
        salt: ByteArray,
        iv: ByteArray
    ): Cipher {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        val keyBytes = try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(TAG_BITS, iv))
        }
    }
}
