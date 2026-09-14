package com.Anchored.mylife.ui

import java.text.NumberFormat
import java.util.Locale

/**
 * 积分数字的统一写法。
 *
 * 成长页、商城、兑换记录三处都在写积分，格式必须一模一样——所以只留这一份：
 * 千分位按系统语言走（中文里 1,200 和英文里的 1,200 是同一个写法，
 * 但阿拉伯语等语言下分隔符会不同，交给 [NumberFormat] 才不会写死）。
 */
internal fun xpText(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toLong())

/**
 * 一笔变动的写法：正数带 `+`，负数自带 `-`。
 *
 * 流水账里的正负是"攒了 / 花了"的唯一记号，所以符号一定要显式写出来，
 * 不能靠颜色或上下文让人去猜。
 */
internal fun xpDelta(value: Int): String = if (value >= 0) "+${xpText(value)}" else xpText(value)
