package com.Anchored.mylife.data.crypto

import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.Note

/**
 * 哪些字段算"用户自己的内容"：成就的标题与描述、笔记正文。
 *
 * 图鉴的 109 条来自开源清单（公共内容），媒体路径是文件系统信息，
 * 两者都不加密——加密它们只会增加开销，不增加隐私。
 *
 * 读和写是两条不同的规则，这也是这次改造的核心：
 * - 写（[encrypted]）：是否加密由设置里的开关决定，关着就存明文；
 * - 读（[decrypted]）：不看开关，只要落盘的是密文就解密。
 *   否则用户一关开关，之前加密的数据就打不开了。
 */

fun Achievement.encrypted(enabled: Boolean): Achievement =
    if (!enabled) {
        this
    } else {
        copy(
            title = DataCipher.encrypt(title),
            description = DataCipher.encrypt(description)
        )
    }

fun Achievement.decrypted(): Achievement = copy(
    title = DataCipher.decrypt(title),
    description = DataCipher.decrypt(description)
)

fun Note.encrypted(enabled: Boolean): Note =
    if (!enabled) this else copy(content = DataCipher.encrypt(content))

fun Note.decrypted(): Note = copy(content = DataCipher.decrypt(content))

/** 迁移判断：这一行里还有没有明文（或旧格式）需要改写 */
fun Achievement.needsReencoding(): Boolean =
    DataCipher.needsReencoding(title) || DataCipher.needsReencoding(description)

fun Note.needsReencoding(): Boolean = DataCipher.needsReencoding(content)

/** 迁移写入：把落盘值统一成当前格式 */
fun Achievement.reencoded(): Achievement = copy(
    title = DataCipher.reencode(title),
    description = DataCipher.reencode(description)
)

fun Note.reencoded(): Note = copy(content = DataCipher.reencode(content))

/** 这一行有没有用户内容；空行没有加密的必要，也不该算进统计 */
fun Achievement.hasContent(): Boolean = title.isNotEmpty() || description.isNotEmpty()

fun Note.hasContent(): Boolean = content.isNotEmpty()
