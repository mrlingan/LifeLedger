package com.Anchored.mylife.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 尺寸系统。
 *
 * 间距只允许用 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 这套刻度。
 * 页面里不要再出现 13dp、17dp、18dp、20dp、22dp 这类随手写的值——
 * 需要新尺寸时先在这里加 token，再去页面用。
 */

@Immutable
data class AppSpacing(
    /** 仅用于极细的分隔与内联间距 */
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    /** 基础间距：卡片内边距、列表项间距、屏幕左右留白 */
    val lg: Dp = 16.dp,
    /** 区块之间的间距 */
    val xl: Dp = 24.dp,
    /** 大区块 / 分区之间的间距 */
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** 页面级的叙事性留白 */
    val huge: Dp = 64.dp
)

@Immutable
data class AppRadius(
    /** 小控件：徽章、标签、输入框 */
    val sm: Dp = 8.dp,
    /** 普通容器：次级面板、内嵌块 */
    val md: Dp = 12.dp,
    /** 普通卡片 */
    val lg: Dp = 16.dp,
    /** Hero 卡片、大图标容器 */
    val hero: Dp = 20.dp,
    /** 全圆角：胶囊按钮、头像 */
    val pill: Dp = 999.dp
)

@Immutable
data class AppSizes(
    val hairline: Dp = 1.dp,
    val iconSm: Dp = 16.dp,
    val iconMd: Dp = 20.dp,
    val iconLg: Dp = 24.dp,
    /** 最小可点击区域，不要小于这个值 */
    val touchTarget: Dp = 48.dp,
    val avatarSm: Dp = 36.dp,
    val avatarMd: Dp = 48.dp,
    val avatarLg: Dp = 72.dp,
    /** 成就详情页的主角图标尺寸 */
    val heroIcon: Dp = 96.dp,
    val progressBar: Dp = 6.dp,
    val progressRing: Dp = 4.dp,
    /** 环形进度的默认直径 */
    val progressRingSize: Dp = 96.dp,
    /** 分类进度圆环直径 */
    val categoryRing: Dp = 56.dp,
    /** 分类进度项宽度：圆环 + 标签横向排布时的统一列宽 */
    val categoryTile: Dp = 76.dp,
    /** 最近解锁的卡片宽度（横向排布，一屏看到两张多一点） */
    val recentCard: Dp = 168.dp,
    /**
     * 图鉴单元的最小宽度。
     *
     * 图鉴条目是「徽记在左、标题与介绍在右」的横排，太窄会把标题挤断，
     * 所以这个值定得比较宽：手机上一列（其实就是一份档案索引），
     * 平板或横屏自动变两列。
     */
    val codexCell: Dp = 320.dp,
    /** 最近解锁的横滑卡片宽度 */
    val recentTile: Dp = 96.dp,
    /** 紧凑顶栏高度 */
    val topBar: Dp = 56.dp,
    /** 底部导航内容高度（不含系统导航栏内边距） */
    val bottomBar: Dp = 56.dp,
    /** 底部导航中间那个主操作按钮的边长 */
    val bottomBarAction: Dp = 44.dp,
    /** 主题设置里背景图预览条的高度 */
    val wallpaperPreview: Dp = 88.dp,
    /** 开关：轨道宽 / 轨道高 / 滑块直径 */
    val switchWidth: Dp = 44.dp,
    val switchHeight: Dp = 26.dp,
    val switchKnob: Dp = 20.dp,
    /** 应用内密码键盘：一个键的高度、密码圆点的直径、键盘最大宽度 */
    val pinKey: Dp = 52.dp,
    val pinDot: Dp = 14.dp,
    val pinPad: Dp = 300.dp,
    /** 编辑态控件：⊖ / ⊕ 的圆圈直径、里面的横杠长度、横杠粗细 */
    val editControl: Dp = 22.dp,
    val editGlyph: Dp = 10.dp,
    val editStroke: Dp = 2.dp,
    /** 分段控件的高度：滑块要按固定高度算位置，不能再靠内容撑 */
    val segmented: Dp = 40.dp,
    /** 分类圆环的颜色样块（挑色弹窗里也是它） */
    val categorySwatch: Dp = 28.dp,
    /**
     * 首页那张自定义图片的高度。
     *
     * 刻意压到 72dp：人生进度那张卡在标准字号下大约 160dp 高，
     * 这条横幅不到它的一半——用户要的是"一条图"，不是又一块内容。
     */
    val homeBanner: Dp = 72.dp,
    /** 屏幕左右统一留白 */
    val gutter: Dp = 16.dp
)

val Spacing = AppSpacing()
val Radius = AppRadius()
val Sizes = AppSizes()
