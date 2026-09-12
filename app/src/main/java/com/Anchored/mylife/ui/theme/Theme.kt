package com.Anchored.mylife.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.Anchored.mylife.data.settings.ListDensity
import com.Anchored.mylife.data.settings.MotionChoice

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
private fun AppColors.toColorScheme(): ColorScheme =
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

        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceElevated,
        onSurfaceVariant = textSecondary,
        surfaceTint = accent,

        surfaceContainerLowest = background,
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
 */
@Composable
fun LifeLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    display: AppDisplay = AppDisplay(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val systemDensity = LocalDensity.current

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
        LocalAppColors provides colors,
        LocalAppTypography provides AppType,
        LocalAppDisplay provides display,
        LocalDensity provides density
    ) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = LegacyMaterialTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
