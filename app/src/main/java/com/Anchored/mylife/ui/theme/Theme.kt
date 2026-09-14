package com.Anchored.mylife.ui.theme

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.Anchored.mylife.data.settings.AppSettings
import com.Anchored.mylife.data.settings.ListDensity
import com.Anchored.mylife.data.settings.MotionChoice
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.LocalLiquidGlassEnabled
import com.Anchored.mylife.ui.components.liquidglass.liquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.rememberLiquidGlassBackdrop

/**
 * 设计系统的统一入口。
 *
 * 页面只通过这里取样式：
 * ```
 * Text(
 *     text = "127",
 *     style = AppTheme.type.numberLarge,
 *     color = AppTheme.colors.textPrimary
 * )
 * ```
 *
 * - 颜色：[AppColors]（见 Color.kt），禁止页面里直接写 Color(0x…)
 * - 字体：[AppTypography]（见 Type.kt）
 * - 间距 / 圆角 / 尺寸：`Spacing` / `Radius` / `Sizes`（见 Dimens.kt）
 * - 动效：`AppMotion`（见 Motion.kt）
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    /**
     * 页面根容器的填充色（Scaffold / 整屏 Box 用这个，不要用 [AppColors.background]）。
     *
     * 没设置背景图时它就是页面底色；设置之后它是透明的 —— 图片和遮罩由
     * [LifeLedgerTheme] 统一铺在内容下面，页面再填一次底色会把图片压暗两遍。
     */
    val pageColor: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalAppPageColor.current

    val type: AppTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAppTypography.current

    /** 显示偏好：列表密度、字号缩放、动效强度 */
    val display: AppDisplay
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDisplay.current
}

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }
private val LocalAppPageColor = staticCompositionLocalOf { LightAppColors.background }
private val LocalAppTypography = staticCompositionLocalOf { AppType }
private val LocalAppDisplay = staticCompositionLocalOf { AppDisplay() }

/**
 * 显示与动效偏好。
 *
 * 由设置页写入、主题层提供，组件按需读取：列表密度决定列表项之间的留白，
 * 字号缩放只作用于 sp（dp 布局不动），动效强度决定动画时长与入场效果。
 */
@Immutable
data class AppDisplay(
    val density: ListDensity = ListDensity.STANDARD,
    val fontScale: Float = 1f,
    val motion: MotionChoice = MotionChoice.FULL
)

/** 列表项的竖直留白，跟随显示密度 */
val listRowPadding: androidx.compose.ui.unit.Dp
    @Composable
    @ReadOnlyComposable
    get() = when (AppTheme.display.density) {
        ListDensity.COMPACT -> Spacing.md
        ListDensity.STANDARD -> Spacing.lg
        ListDensity.COMFY -> Spacing.xl
    }

/**
 * 过渡常量。
 *
 * 首页 / 图鉴 / 新建页的头部目前还用这两个值画背景渐变，
 * Phase 3 起会由 AppTopBar 取代，届时删除这两个常量。
 *
 * 现在它们指向深墨色 —— 先彻底去掉原来的紫蓝渐变。
 */
val BrandViolet = Ink900
val BrandVioletBright = Ink700

/**
 * 把语义色映射到 Material 的 colorScheme。
 *
 * 还没重构的页面用的是 `MaterialTheme.colorScheme.xxx`，
 * 经过这层映射，它们也会立刻切换成新配色，不需要改页面代码。
 */
private fun AppColors.toColorScheme(pageColor: Color = background): ColorScheme =
    (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = accentStrong,
        inversePrimary = accent,

        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = accentSoft,
        onSecondaryContainer = accentStrong,

        // tertiary 在当前页面里承担「已完成」的语义，用成就金色
        tertiary = gold,
        onTertiary = onAccent,
        tertiaryContainer = gold.copy(alpha = 0.16f),
        onTertiaryContainer = textPrimary,

        background = pageColor,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = textSecondary,
        surfaceTint = accent,

        surfaceContainerLowest = pageColor,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceElevated,
        surfaceContainerHigh = surfaceElevated,
        surfaceContainerHighest = surfaceElevated,

        outline = border,
        outlineVariant = divider,

        error = error,
        onError = onAccent,
        errorContainer = error.copy(alpha = 0.16f),
        onErrorContainer = textPrimary,

        inverseSurface = textPrimary,
        inverseOnSurface = background
    )

/**
 * 全局主题。
 *
 * 刻意去掉了动态取色（Dynamic Color）：
 * 这是一个有明确视觉定位的产品，配色不接受被壁纸改写。
 *
 * 背景图片也在这里统一处理：图片铺在最底层，上面盖一层页面底色的遮罩，
 * 内容再叠在最上面。遮罩只画这一次 —— 页面自己的根容器改用
 * [AppTheme.pageColor]（有图时是透明的），否则两层遮罩叠起来图片就看不见了。
 *
 * 这一层背景还会**单独录进一份采样源**（[LocalLiquidGlassBackdrop]），
 * 让页面里的玻璃卡片能折射它。底栏那份采样源录的是「背景 + 页面」，两者必须分开：
 * 卡片自己就在页面里，录进同一层就会采样到自己。
 *
 * 用户在设置里关掉液态玻璃（[glassEnabled] = false）时这份采样源干脆不录：页面里的
 * 卡片取不到采样源会自己退回普通卡片，界面也不用为一份没人用的图层买单。
 *
 * @param backgroundImage 已经解码好的背景图；还没解码完可以传 null
 * @param backgroundImageSet 用户是否设置了背景图。解码未完成时也要靠它
 *   决定页面底色，免得图片加载出来的一瞬间整屏颜色跳一下
 * @param backgroundImageOpacity 背景图显示强度，1 表示完全显示
 * @param contentModifier 加在「背景 + 内容」这一层上的修饰符。液态玻璃要折射背后
 *   的画面，就得把这一整层录进同一个图层，所以录制的入口在这里，而不是每个页面各录一份
 * @param overlay 画在背景与内容之后、不被 [contentModifier] 覆盖的一层。
 *   玻璃底栏属于这一层：它必须能看见底下的内容，又不能把自己也录进采样图层
 * @param glassEnabled 是否启用液态玻璃。关掉之后页面里的玻璃卡片退回普通卡片表面；
 *   底栏由调用方传 null 采样源，退回纯色磨砂
 */
@Composable
fun LifeLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    display: AppDisplay = AppDisplay(),
    backgroundImage: ImageBitmap? = null,
    backgroundImageSet: Boolean = backgroundImage != null,
    backgroundImageOpacity: Float = AppSettings.BACKGROUND_OPACITY_DEFAULT,
    glassEnabled: Boolean = true,
    contentModifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit
) {
    val baseColors = if (darkTheme) DarkAppColors else LightAppColors
    val opacity = backgroundImageOpacity.coerceIn(
        AppSettings.BACKGROUND_OPACITY_MIN,
        AppSettings.BACKGROUND_OPACITY_MAX
    )
    val pageColor = if (backgroundImageSet) Color.Transparent else baseColors.background
    val systemDensity = LocalDensity.current

    // 页面内玻璃（卡片）的采样源：只录背景层。
    // 关掉开关、或者在布局预览里跑（预览不走真实的硬件加速录制管线）时给 null，
    // 卡片取不到采样源就退回普通表面。
    // 「有没有采样源」和「要不要玻璃」是两件事：前者看这个值，后者看
    // [LocalLiquidGlassEnabled]（对话框里采不到页面，但材质仍然是玻璃）。
    val glassOn = glassEnabled && !LocalInspectionMode.current
    val pageGlassBackdrop = if (glassOn) {
        rememberLiquidGlassBackdrop()
    } else {
        null
    }

    // 应用内字号缩放：在系统字号之上再乘一层，只影响 sp，不动 dp
    val density = remember(systemDensity, display.fontScale) {
        Density(
            density = systemDensity.density,
            fontScale = systemDensity.fontScale * display.fontScale
        )
    }

    // 状态栏 / 导航栏图标跟随主题：
    // 浅色背景用深色图标，深色背景用浅色图标，避免出现"白底白图标"。
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides baseColors,
        LocalAppPageColor provides pageColor,
        LocalAppTypography provides AppType,
        LocalAppDisplay provides display,
        LocalLiquidGlassBackdrop provides pageGlassBackdrop,
        LocalLiquidGlassEnabled provides glassOn,
        LocalDensity provides density
    ) {
        MaterialTheme(
            colorScheme = baseColors.toColorScheme(pageColor),
            typography = LegacyMaterialTypography,
            shapes = AppShapes,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(contentModifier)
                ) {
                    // 背景层单独录一份，供页面内的玻璃卡片采样
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .liquidGlassBackdrop(pageGlassBackdrop)
                    ) {
                        // 底色永远先铺一层：没有背景图时采样层也得是不透明的，
                        // 否则玻璃算出来的 alpha 是 0，卡片会退化成只剩描边的空框
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(baseColors.background)
                        )

                        if (backgroundImageSet) {
                            if (backgroundImage != null) {
                                Image(
                                    bitmap = backgroundImage,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // 唯一的遮罩层：强度越低调越淡，图片越清楚
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(baseColors.background.copy(alpha = 1f - opacity))
                            )
                        }
                    }
                    content()
                }

                overlay()
            }
        }
    }
}
