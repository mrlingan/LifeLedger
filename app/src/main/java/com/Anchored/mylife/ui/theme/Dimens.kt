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
    /**
     * 成组的容器：设置页那种"一张卡里装一整组行"的大卡片。
     *
     * 比 Hero 再大一圈（24dp）。这种卡里最上面一行离圆角很近，
     * 圆角小了会显得尖，和里面 42dp 的图标方块也不像一个尺度。
     */
    val xl: Dp = 24.dp,
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
    /** 「我的」页图鉴收藏的徽章圆环直径：一行五个，比分类圆环小一档才排得下 */
    val tierBadge: Dp = 48.dp,
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
     * 设置项行首那枚图标方块：一个软色底的小方块，里面放着图标字形。
     *
     * 42dp 比常规图标容器（[homeQuickIcon] 40dp）大一档：设置行里它要和两行文字
     * 齐高，小了会显得"飘"在两行字中间。
     */
    val settingMark: Dp = 42.dp,
    /** 上面那个方块的圆角。比卡片小一档，看起来才是"卡里的一枚图标" */
    val settingMarkRadius: Dp = 14.dp,
    /** 取色器：色带的高度与游标直径（游标比色带粗一圈，压在上面才抓得住） */
    val colorStrip: Dp = 14.dp,
    val colorMarker: Dp = 26.dp,
    /** 取色器每行左边的标签宽度：三条色带要左右对齐才看得出是同一条轴上选的 */
    val pickerLabel: Dp = 44.dp,
    /** 图鉴页头的搜索药丸高度：和输入框一样高，展开前后不会跳一下 */
    val searchPill: Dp = 40.dp,
    /** 状态标签下面那条指示器的高度（压在分割线上） */
    val tabIndicator: Dp = 3.dp,
    /** 图鉴总进度圆环：比分类圆环大一圈，里面的百分比才放得下 */
    val codexRing: Dp = 76.dp,
    val codexRingStroke: Dp = 6.dp,
    /** 图鉴「最新解锁」卡片的封面高度 */
    val codexRecentCover: Dp = 88.dp,
    /**
     * 积分商城奖励卡片顶部的图标区高度。
     *
     * 两列并排时卡片宽度只有 160dp 上下，图标区再高就挤掉正文；
     * 和「最新解锁」的封面取同一个值，两处并排出现时高度是对齐的。
     */
    val rewardCover: Dp = 88.dp,
    /** 图鉴分类格子的高度：两行分类名 + 一行计数 */
    val codexCategoryTile: Dp = 72.dp,
    /** 图鉴稀有度圆环直径：一行五个 */
    val codexTierRing: Dp = 56.dp,
    /**
     * 首页自定义图片**还没有图**时，设置页里那个占位框的高度。
     *
     * 有图之后尺寸由图片自己决定（宽度铺满卡片、高度按原图比例），
     * 这个值只剩"没图时占个位"的用途，所以压到 72dp 就够了。
     */
    val homeBanner: Dp = 72.dp,
    /** 首页快捷入口方块里的图标容器：一个圆角方块，和卡片一样靠表面色分层 */
    val homeQuickIcon: Dp = 40.dp,
    /** 上面那个方块的圆角。比卡片小一档，看起来才是"卡里的一枚图标" */
    val homeQuickRadius: Dp = 14.dp,
    /** 首页「最近解锁」每一行左边的缩略图：宽比高多一点，和记录卡片的横构图一致 */
    val homeThumbWidth: Dp = 88.dp,
    val homeThumbHeight: Dp = 68.dp,
    /** 成长页「人生属性」格子里那枚分类图标：一个圆，和标签同宽才排得下三列 */
    val growthAttributeIcon: Dp = 40.dp,
    /** 成长记录每一行左边的图标 */
    val growthRecordIcon: Dp = 44.dp,
    /** 成长曲线的画布高度 */
    val growthChart: Dp = 132.dp,
    /** 成长曲线的线宽与末端那个点：细线 + 小点，画的是余额，不是一根装饰 */
    val growthChartStroke: Dp = 2.dp,
    val growthChartDot: Dp = 3.dp,
    /**
     * 「人生属性」格子里那条细进度条。
     *
     * 比常规进度条（6dp）细一档：一行三个格子并排时，6dp 会显得比它装的内容还重。
     */
    val growthMicroBar: Dp = 4.dp,
    /** 屏幕左右统一留白 */
    val gutter: Dp = 16.dp
)

val Spacing = AppSpacing()
val Radius = AppRadius()
val Sizes = AppSizes()
