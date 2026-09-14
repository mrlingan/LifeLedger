package com.Anchored.mylife.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 分类圆环颜色的存取。
 *
 * 设置层不认识 Compose 的 [Color]（数据层不依赖界面），落盘统一是 `%08X` 的 ARGB 字符串，
 * 界面这一层负责来回翻译。翻译不出来的值一律当成"没设过"，
 * 这样手改坏的偏好文件不会让首页直接崩。
 */
internal fun Color.toStoredColor(): String = "%08X".format(toArgb())

internal fun storedColorToColor(value: String?): Color? {
    if (value.isNullOrBlank()) return null
    val argb = value.toLongOrNull(radix = 16)?.toInt() ?: return null
    return Color(argb)
}
