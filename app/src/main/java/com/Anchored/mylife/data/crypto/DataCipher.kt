package com.Anchored.mylife.data.crypto

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 本地字段加密：信封加密（envelope encryption），Keystore 只在启动时碰一次。
 *
 * 为什么不是"每个字段直接找 Keystore 要密钥"：
 * Keystore 的 load / getEntry / Cipher.init 每次都是跨进程调用，1~2ms 起步。
 * 一屏几十条成就、每条两三个字段，就是上百次 IPC，列表滑动自然会卡。
 *
 * 信封加密把这件事拆成两层：
 * - Keystore 里只放一把「包装密钥」（wrapping key），它唯一的用途是加解密数据密钥；
 * - 真正加密内容的是随机生成的 32 字节数据密钥（data key），
 *   启动时由包装密钥解开一次、缓存到内存，之后所有加解密都在本进程内完成，
 *   热路径上不再有任何 Keystore 调用。
 *
 * 落盘格式（前缀带版本号，以后换密钥或换算法就加 enc3:，老数据仍按老规则读）：
 * - `enc2:` + base64(iv ‖ AES-256-GCM 密文)  当前格式，用数据密钥加密
 * - `enc1:` + base64(iv ‖ AES-256-GCM 密文)  旧格式，直接用 Keystore 密钥加密，只读
 *
 * `enc1:` 是这次改造之前那版半成品写下的数据。留一条只读的解密路径，
 * 是为了老数据不会因为换了加密方式而打不开；等它被迁移成 enc2: 之后，
 * 这条路径就不会再被走到。
 *
 * 两条容错是刻意保留的，都是为了避免"加密反而弄丢数据"：
 * - 读到没有前缀的值，就当成明文原样返回（开关关着时写下的行就是这样）
 * - 加解密任何一步失败都不抛异常，宁可保留原值也不丢内容
 *
 * 边界也要说清楚：这把钥匙不需要用户解锁，能挡住的是把库文件拷走
 * （adb backup、云同步、换机拷贝）这类离线读取；挡不住已经 root
 * 并且能在同一台设备上跑代码的人。
 */
object DataCipher {

    private const val KEYSTORE = "AndroidKeyStore"

    /** 包装密钥：只用来加解密数据密钥，自己不出 Keystore */
    private const val WRAPPING_ALIAS = "lifeledger_wrap_key"

    /** 旧格式用的别名，只读；不会再往这里写新数据 */
    private const val LEGACY_ALIAS = "lifeledger_data_key"

    /** 当前格式 */
    private const val PREFIX = "enc2:"

    /** 旧格式（半成品时期直接拿 Keystore 密钥加密） */
    private const val LEGACY_PREFIX = "enc1:"

    private const val PREFS = "lifeledger_settings"
    private const val KEY_WRAPPED_DATA_KEY = "data_key_wrapped_v1"

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_BITS = 128
    private const val DATA_KEY_BYTES = 32

    @Volatile
    private var appContext: Context? = null

    /** 解开后的数据密钥，只在内存里存在 */
    @Volatile
    private var dataKey: SecretKey? = null

    /** 旧格式的解密密钥：只有真读到 enc1: 才会去 Keystore 取一次 */
    @Volatile
    private var legacyKey: SecretKey? = null

    @Volatile
    private var wrappingKeyCache: SecretKey? = null

    private val keyLock = Any()

    /**
     * Cipher.getInstance 本身有开销，一屏上百个字段时累积起来不可忽略。
     * Cipher 不是线程安全的，所以每个线程各留一份实例，密钥和 IV 每次重新 init。
     */
    private val localCipher = object : ThreadLocal<Cipher>() {
        override fun initialValue(): Cipher = Cipher.getInstance(TRANSFORMATION)
    }

    private fun cipher(): Cipher = requireNotNull(localCipher.get()) { "无法创建 AES/GCM 实例" }

    /** 在 Application / RepositoryProvider 初始化时调用一次 */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * 预热：把数据密钥解出来放进内存。
     *
     * 失败不抛异常——真正用到时还会再试，实在拿不到密钥就退回明文，
     * 让应用还能用，而不是打不开。
     */
    fun warmUp() {
        ensureDataKey()
    }

    // ---------------- 对外只关心这三件事 ----------------

    /** 是不是密文（新旧两种格式都算） */
    fun isEncrypted(value: String): Boolean =
        value.startsWith(PREFIX) || value.startsWith(LEGACY_PREFIX)

    /** 是否需要改写成当前格式：空值不用，已经是 enc2: 的也不用 */
    fun needsReencoding(value: String): Boolean =
        value.isNotEmpty() && !value.startsWith(PREFIX)

    fun encrypt(plain: String): String {
        if (plain.isEmpty() || isEncrypted(plain)) return plain
        val key = ensureDataKey() ?: return plain
        return runCatching {
            val cipher = cipher()
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(iv + body, Base64.NO_WRAP)
        }.getOrDefault(plain)
    }

    fun decrypt(stored: String): String = when {
        stored.startsWith(PREFIX) -> decode(stored.removePrefix(PREFIX), ensureDataKey(), stored)
        stored.startsWith(LEGACY_PREFIX) -> decode(stored.removePrefix(LEGACY_PREFIX), legacyKey(), stored)
        else -> stored
    }

    /**
     * 迁移用：把落盘值统一成当前格式。
     *
     * 明文直接加密；enc1: 先解密再用数据密钥重新加密；
     * 已经是 enc2: 或者拿不到密钥时原样返回。
     */
    fun reencode(stored: String): String = when {
        stored.isEmpty() -> stored
        stored.startsWith(PREFIX) -> stored
        else -> encrypt(decrypt(stored))
    }

    // ---------------- 内部实现 ----------------

    private fun decode(body: String, key: SecretKey?, fallback: String): String {
        if (key == null) return fallback
        return runCatching {
            val bytes = Base64.decode(body, Base64.NO_WRAP)
            if (bytes.size <= IV_LENGTH) return@runCatching fallback
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val cipherText = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = cipher()
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrDefault(fallback)
    }

    /**
     * 数据密钥：优先从内存拿；没有就从 SharedPreferences 里取出被包装的版本，
     * 用 Keystore 的包装密钥解开。
     */
    private fun ensureDataKey(): SecretKey? {
        dataKey?.let { return it }
        synchronized(keyLock) {
            dataKey?.let { return it }
            if (appContext == null) return null
            val key = runCatching { loadOrCreateDataKey() }.getOrNull()
            dataKey = key
            return key
        }
    }

    private fun loadOrCreateDataKey(): SecretKey {
        val prefs = requireNotNull(appContext) { "DataCipher 还没有初始化" }
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_WRAPPED_DATA_KEY, null)?.let { stored ->
            runCatching { unwrapDataKey(stored) }.getOrNull()?.let { return it }
            // 包装坏了（比如系统里的包装密钥丢了）：只能重新生成一把。
            // 此时旧的密文会解不开，但读路径不抛异常，也不会丢行。
        }
        return createDataKey(prefs)
    }

    private fun createDataKey(prefs: SharedPreferences): SecretKey {
        val raw = ByteArray(DATA_KEY_BYTES).also { SecureRandom().nextBytes(it) }
        val wrapped = wrapDataKey(raw)
        // 必须落盘成功才能使用：否则密钥丢了，用它加密的数据就再也解不开
        check(prefs.edit().putString(KEY_WRAPPED_DATA_KEY, wrapped).commit()) {
            "数据密钥保存失败"
        }
        return SecretKeySpec(raw, "AES")
    }

    private fun wrapDataKey(raw: ByteArray): String {
        val cipher = cipher()
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val iv = cipher.iv
        val body = cipher.doFinal(raw)
        return Base64.encodeToString(iv + body, Base64.NO_WRAP)
    }

    private fun unwrapDataKey(stored: String): SecretKey {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_LENGTH)
        val body = bytes.copyOfRange(IV_LENGTH, bytes.size)
        val cipher = cipher()
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(TAG_BITS, iv))
        return SecretKeySpec(cipher.doFinal(body), "AES")
    }

    private fun wrappingKey(): SecretKey {
        wrappingKeyCache?.let { return it }
        synchronized(keyLock) {
            wrappingKeyCache?.let { return it }
            val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
            val existing = (keyStore.getEntry(WRAPPING_ALIAS, null) as? KeyStore.SecretKeyEntry)
                ?.secretKey
            val key = existing ?: generateWrappingKey()
            wrappingKeyCache = key
            return key
        }
    }

    private fun generateWrappingKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                WRAPPING_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    /** 旧格式的密钥。只有读到 enc1: 才会走这里 */
    private fun legacyKey(): SecretKey? {
        legacyKey?.let { return it }
        synchronized(keyLock) {
            legacyKey?.let { return it }
            val key = runCatching {
                val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
                (keyStore.getEntry(LEGACY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            }.getOrNull()
            legacyKey = key
            return key
        }
    }
}
