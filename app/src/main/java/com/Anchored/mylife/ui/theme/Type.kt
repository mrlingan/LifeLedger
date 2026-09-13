package com.Anchored.mylife.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体系统。
 *
 * 这个 App 本质是「人生数据系统」，所以数字单独成体系（NumberLarge / NumberMedium）。
 * 所有数字样式都开 `tnum`（等宽数字），保证数值变化时不会左右跳动。
 *
 * 页面使用方式：`AppTheme.type.h1` / `AppTheme.type.numberLarge`。
 */

private const val TABULAR_NUMBERS = "tnum"

@Immutable
data class AppTypography(
    /** 页面级超大标题，一个页面最多出现一次 */
    val display: TextStyle,
    /** 区块主标题 */
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    /** 说明性文字、单位、时间 */
    val caption: TextStyle,
    /** 核心数字：完成数、百分比这类主角数字 */
    val numberLarge: TextStyle,
    /** 次级数字：统计栏、列表里的计数 */
    val numberMedium: TextStyle
)

internal val AppType = AppTypography(
    display = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.6).sp
    ),
    h2 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    h3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    caption = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp
    ),
    numberLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = TABULAR_NUMBERS
    ),
    numberMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = TABULAR_NUMBERS
    )
)

/**
 * 映射给 Material 的字体表。
 *
 * 还没重构的页面用的是 `MaterialTheme.typography.titleMedium` 这类 M3 角色，
 * 这里把它们对齐到新体系，保证「重构一半」的状态下观感也是统一的。
 * 全部页面重构完成后，这张表可以只留兜底作用。
 */
internal val LegacyMaterialTypography = Typography(
    displayLarge = AppType.display,
    displayMedium = AppType.numberLarge,
    displaySmall = AppType.h1,
    headlineLarge = AppType.h1,
    headlineMedium = AppType.h2,
    headlineSmall = AppType.h3,
    titleLarge = AppType.h3,
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = AppType.bodySmall,
    bodyLarge = AppType.bodyLarge,
    bodyMedium = AppType.body,
    bodySmall = AppType.bodySmall,
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = AppType.caption,
    labelSmall = AppType.caption
)
