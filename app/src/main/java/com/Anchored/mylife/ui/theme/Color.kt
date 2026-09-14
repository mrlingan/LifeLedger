package com.Anchored.mylife.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 颜色系统。
 *
 * 规则：**只有这个文件里允许出现 `Color(0x…)`**。
 * 页面和组件一律通过 `AppTheme.colors.xxx` 取语义色。
 *
 * 体系：iOS 分组背景 + 系统蓝强调色 + 标准语义状态色。
 * 页面只使用语义角色，不依赖某个系统主题下的具体色值。
 */

// ---------------- 中性色阶 ----------------
// 对齐 iOS 的 grouped background 和 system gray 层级
internal val Ink950 = Color(0xFF000000)
internal val Ink900 = Color(0xFF000000)
internal val Ink800 = Color(0xFF1C1C1E)
internal val Ink700 = Color(0xFF2C2C2E)
private val Gray600 = Color(0xFF636366)
private val Gray500 = Color(0xFF6C6C70)
private val Gray400 = Color(0xFF8E8E93)
private val Gray300 = Color(0xFFC7C7CC)
private val Gray200 = Color(0xFFD1D1D6)
private val Gray150 = Color(0xFFE5E5EA)
private val Gray100 = Color(0xFFE5E5EA)
private val Gray50 = Color(0xFFF2F2F7)
private val PureWhite = Color(0xFFFFFFFF)

// ---------------- 强调：iOS System Blue ----------------
private val Teal900 = Color(0xFF003A75)
private val Teal700 = Color(0xFF0066CC)
private val Teal600 = Color(0xFF007AFF)
private val Teal300 = Color(0xFF64D2FF)
private val Teal50 = Color(0xFFEAF3FF)

// ---------------- 状态 ----------------
private val Green600 = Color(0xFF34C759)
private val Green400 = Color(0xFF30D158)
private val Amber600 = Color(0xFFFF9500)
private val Amber400 = Color(0xFFFF9F0A)
private val Red600 = Color(0xFFFF3B30)
private val Red400 = Color(0xFFFF453A)
private val Slate600 = Color(0xFF007AFF)
private val Slate400 = Color(0xFF0A84FF)

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
private val DarkTextPrimary = Color(0xFFF2F2F7)
private val DarkTextSecondary = Color(0xFFAEAEB2)
private val DarkTextTertiary = Color(0xFF8E8E93)
private val DarkDivider = Color(0xFF38383A)
private val DarkBorder = Color(0xFF48484A)

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
 * 液态玻璃专用色。
 *
 * 玻璃不是一块有固定颜色的表面：它的观感来自「底色多奶、上缘多亮、边缘多硬」，
 * 所以这里按角色拆开，浅色与深色各给一套，页面和组件不要再自己调白色百分比。
 */
@Immutable
data class GlassColors(
    /** 玻璃底色。alpha 就是「奶度」：越大越糊，越小越透 */
    val tint: Color,
    /** 上缘高光渐变：从这条亮边往下淡出 */
    val sheen: Color,
    /** 描边：上缘提亮、下缘压暗，玻璃才有厚度转折 */
    val rimTop: Color,
    val rimBottom: Color,
    /** 选中态玻璃滴的底色，比整条栏更亮一点才浮得起来 */
    val droplet: Color,
    val dropletSheen: Color,
    val dropletRimTop: Color,
    val dropletRimBottom: Color
)

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
    val legendary: Color,

    // 液态玻璃
    val glass: GlassColors
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
    legendary = Legendary600,
    // 浅色玻璃：偏白的奶玻璃，上缘亮、下缘收一条极淡的暗边
    glass = GlassColors(
        tint = Color(0x7AFFFFFF),
        sheen = Color(0x59FFFFFF),
        rimTop = Color(0xB3FFFFFF),
        rimBottom = Color(0x14000000),
        droplet = Color(0x6BFFFFFF),
        dropletSheen = Color(0x73FFFFFF),
        dropletRimTop = Color(0xCCFFFFFF),
        dropletRimBottom = Color(0x1F000000)
    )
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
    accentStrong = Teal300,
    onAccent = Ink900,
    success = Green400,
    warning = Amber400,
    error = Red400,
    info = Slate400,
    bronze = Bronze400,
    silver = Silver400,
    gold = Gold400,
    platinum = Platinum400,
    legendary = Legendary400,
    // 深色玻璃：不是纯黑，带一点灰才有玻璃的实体感
    glass = GlassColors(
        tint = Color(0x8C2C2C2E),
        sheen = Color(0x1FFFFFFF),
        rimTop = Color(0x3DFFFFFF),
        rimBottom = Color(0x0AFFFFFF),
        droplet = Color(0x3DFFFFFF),
        dropletSheen = Color(0x33FFFFFF),
        dropletRimTop = Color(0x66FFFFFF),
        dropletRimBottom = Color(0x14FFFFFF)
    )
)

/**
 * 首页「分类进度」里圆环可以挑的颜色。
 *
 * 固定色值，不跟明暗主题走：用户挑的是"这个分类是蓝的"，不是"它在浅色下是蓝的"。
 * 这几个色在两种主题下都还看得清——深色底上不发闷，浅色底上不发飘。
 */
internal val CategoryColorChoices: List<Color> = listOf(
    Color(0xFF007AFF), // 蓝
    Color(0xFF32ADE6), // 青
    Color(0xFF34C759), // 绿
    Color(0xFFFFCC00), // 黄
    Color(0xFFFF9500), // 橙
    Color(0xFFFF3B30), // 红
    Color(0xFFFF2D55), // 粉
    Color(0xFFAF52DE), // 紫
    Color(0xFFA2845E), // 棕
    Color(0xFF8E8E93)  // 灰
)
