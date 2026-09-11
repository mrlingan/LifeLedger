package com.Anchored.mylife.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 颜色系统。
 *
 * 规则：**只有这个文件里允许出现 `Color(0x…)`**。
 * 页面和组件一律通过 `AppTheme.colors.xxx` 取语义色。
 *
 * 体系：中性灰阶 + 深墨色 + 单一强调青绿 + 状态色 + 稀有度金属色。
 * 强调色克制使用，稀有度色只出现在成就相关元素上。
 */

// ---------------- 中性色阶 ----------------
// 冷灰，带一点点青，避免纯灰发脏
internal val Ink950 = Color(0xFF0B1011)
internal val Ink900 = Color(0xFF0F1416)
internal val Ink800 = Color(0xFF161D1F)
internal val Ink700 = Color(0xFF212A2B)
private val Gray600 = Color(0xFF4A5556)
private val Gray500 = Color(0xFF6C7777)
private val Gray400 = Color(0xFF8E9897)
private val Gray300 = Color(0xFFB7BFBF)
private val Gray200 = Color(0xFFD9DEDD)
private val Gray150 = Color(0xFFE6EAE9)
private val Gray100 = Color(0xFFEFF2F1)
private val Gray50 = Color(0xFFF5F7F6)
private val PureWhite = Color(0xFFFFFFFF)

// ---------------- 强调：低饱和青绿 ----------------
private val Teal900 = Color(0xFF16302B)
private val Teal700 = Color(0xFF2A5A50)
private val Teal600 = Color(0xFF35695E)
private val Teal300 = Color(0xFF9CC4BB)
private val Teal50 = Color(0xFFEEF4F2)

// ---------------- 状态 ----------------
private val Green600 = Color(0xFF3F7A5E)
private val Green400 = Color(0xFF7FBEA0)
private val Amber600 = Color(0xFFA9762E)
private val Amber400 = Color(0xFFD8B070)
private val Red600 = Color(0xFFA8443C)
private val Red400 = Color(0xFFE29A92)
private val Slate600 = Color(0xFF4A6C8C)
private val Slate400 = Color(0xFF93B4CE)

// ---------------- 稀有度：金属色 ----------------
// 只在成就图标、稀有度徽章、进度这类元素上使用，不做大面积铺色
private val Bronze600 = Color(0xFFA9714B)
private val Bronze400 = Color(0xFFC68F66)
private val Silver600 = Color(0xFF8892A0)
private val Silver400 = Color(0xFFA7B0BB)
private val Gold600 = Color(0xFFB8912F)
private val Gold400 = Color(0xFFD4B45A)
private val Platinum600 = Color(0xFF5F8FA6)
private val Platinum400 = Color(0xFF8FB6C8)
private val Legendary600 = Color(0xFF9B3D5E)
private val Legendary400 = Color(0xFFC4718D)

// ---------------- 深色模式专用中性值 ----------------
private val DarkTextPrimary = Color(0xFFE9EDEC)
private val DarkTextSecondary = Color(0xFF9AA5A4)
private val DarkTextTertiary = Color(0xFF6E7978)
private val DarkDivider = Color(0xFF252E2F)
private val DarkBorder = Color(0xFF2E3839)
private val DarkAccentStrong = Color(0xFF7FBFAF)

/** 稀有度档位。数据库里的 rarity 字符串到档位的映射放到图鉴重构阶段处理。 */
enum class RarityTier {
    Bronze,
    Silver,
    Gold,
    Platinum,
    Legendary;

    companion object {
        /**
         * 按达成率推导档位：越难达成越稀有。
         *
         * 这样做的原因是数据库里的 rarity 只有四档（common/rare/epic/legendary），
         * 而设计上需要五档。达成率本来就是这套数据里最客观的难度指标，
         * 直接按它换算，既不用改表，也不会出现"标注和数值矛盾"的情况。
         */
        fun fromRate(rate: Double): RarityTier = when {
            rate > 50.0 -> Bronze
            rate > 20.0 -> Silver
            rate > 10.0 -> Gold
            rate > 5.0 -> Platinum
            else -> Legendary
        }
    }
}

/**
 * 语义色。页面只认这些名字，不认具体色值。
 */
@Immutable
data class AppColors(
    val isDark: Boolean,

    // 基础层
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,

    // 文字三档
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // 线
    val divider: Color,
    val border: Color,

    // 强调
    val accent: Color,
    val accentSoft: Color,
    val accentStrong: Color,
    val onAccent: Color,

    // 状态
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,

    // 稀有度
    val bronze: Color,
    val silver: Color,
    val gold: Color,
    val platinum: Color,
    val legendary: Color
) {
    fun rarityColor(tier: RarityTier): Color = when (tier) {
        RarityTier.Bronze -> bronze
        RarityTier.Silver -> silver
        RarityTier.Gold -> gold
        RarityTier.Platinum -> platinum
        RarityTier.Legendary -> legendary
    }
}

internal val LightAppColors = AppColors(
    isDark = false,
    background = Gray50,
    surface = PureWhite,
    surfaceElevated = PureWhite,
    surfaceSunken = Gray100,
    textPrimary = Ink900,
    textSecondary = Gray500,
    textTertiary = Gray400,
    divider = Gray150,
    border = Gray200,
    accent = Teal600,
    accentSoft = Teal50,
    accentStrong = Teal700,
    onAccent = PureWhite,
    success = Green600,
    warning = Amber600,
    error = Red600,
    info = Slate600,
    bronze = Bronze600,
    silver = Silver600,
    gold = Gold600,
    platinum = Platinum600,
    legendary = Legendary600
)

internal val DarkAppColors = AppColors(
    isDark = true,
    background = Ink900,
    surface = Ink800,
    surfaceElevated = Ink700,
    surfaceSunken = Ink950,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    divider = DarkDivider,
    border = DarkBorder,
    accent = Teal300,
    accentSoft = Teal900,
    accentStrong = DarkAccentStrong,
    onAccent = Ink900,
    success = Green400,
    warning = Amber400,
    error = Red400,
    info = Slate400,
    bronze = Bronze400,
    silver = Silver400,
    gold = Gold400,
    platinum = Platinum400,
    legendary = Legendary400
)
