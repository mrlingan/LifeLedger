package com.Anchored.mylife.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.Anchored.mylife.R
import com.Anchored.mylife.data.crypto.AppPin
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.data.settings.StartChoice
import com.Anchored.mylife.data.settings.MotionChoice
import com.Anchored.mylife.ui.components.AppBottomBar
import com.Anchored.mylife.ui.components.AppBottomBarAction
import com.Anchored.mylife.ui.components.AppBottomBarItem
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.components.liquidglass.liquidGlassBackdrop
import com.Anchored.mylife.ui.components.liquidglass.rememberLiquidGlassBackdrop
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.AppDisplay
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

// ---------------------------------------------------------------------------
// 路由表
// ---------------------------------------------------------------------------

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ACHIEVEMENTS = "all_achievements"

/** 图鉴：pick=true 时是从新建流程里挑一条，这时它是二级页面 */
internal const val ROUTE_CODEX = "preset_achievements?pick={pick}"
internal const val ROUTE_CODEX_BROWSE = "preset_achievements?pick=false"
internal const val ROUTE_CODEX_PICK = "preset_achievements?pick=true"
internal const val ROUTE_SETTINGS = "settings"

/**
 * 底部导航的四个入口。
 *
 * [route] 用来和当前目的地比对（导航库返回的是路由模板，带参数占位符），
 * [navigateRoute] 才是真正要跳的路由。
 */
private enum class BottomTab(
    val route: String,
    val navigateRoute: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Home(ROUTE_HOME, ROUTE_HOME, R.string.nav_home, Icons.Outlined.Home),
    Achievements(
        ROUTE_ACHIEVEMENTS,
        ROUTE_ACHIEVEMENTS,
        R.string.nav_achievements,
        Icons.AutoMirrored.Outlined.List
    ),
    Codex(ROUTE_CODEX, ROUTE_CODEX_BROWSE, R.string.nav_codex, Icons.Outlined.Star),
    Settings(ROUTE_SETTINGS, ROUTE_SETTINGS, R.string.nav_settings, Icons.Outlined.Settings)
}

/**
 * 切换底部 tab。
 *
 * popUpTo 起始页 + saveState / restoreState：切走再切回来时，
 * 各个 tab 自己的滚动位置和筛选状态还在，也不会在返回栈里堆一摞。
 */
internal fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * 导航图。
 *
 * 除了路由，这里还负责三件全局的事：
 * - 主题：根据用户的深色模式偏好决定 light / dark
 * - 应用锁：把锁屏盖在所有页面之上
 * - 底部导航：只在四个顶级页面上出现，二级页面（详情 / 新建 / 备份 / 挑图鉴）自动收起
 */
@Composable
fun AchievementNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val appSettings = remember(context) {
        RepositoryProvider.get(context.applicationContext).settings
    }

    val themeMode by appSettings.themeMode.collectAsStateWithLifecycle()
    val appLockEnabled by appSettings.appLockEnabled.collectAsStateWithLifecycle()
    val appPinHash by appSettings.appPinHash.collectAsStateWithLifecycle()
    val startChoice by appSettings.startChoice.collectAsStateWithLifecycle()
    val listDensity by appSettings.listDensity.collectAsStateWithLifecycle()
    val fontScale by appSettings.fontScale.collectAsStateWithLifecycle()
    val motion by appSettings.motion.collectAsStateWithLifecycle()
    val backgroundImageUri by appSettings.backgroundImageUri.collectAsStateWithLifecycle()
    val backgroundImageOpacity by appSettings.backgroundImageOpacity.collectAsStateWithLifecycle()

    // 背景图在这里解码一次，交给主题层统一铺底
    val backgroundImage = backgroundImageUri?.let { uri ->
        rememberUriThumbnail(Uri.parse(uri), sizePx = 1440)
    }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val display = remember(listDensity, fontScale, motion) {
        AppDisplay(
            density = listDensity,
            fontScale = fontScale.scale,
            motion = motion
        )
    }

    // 页面转场时长跟着动效设置走：关闭时直接 0，等于没有转场
    val motionScale = when (motion) {
        MotionChoice.ELEGANT -> 1.45f
        MotionChoice.FULL -> 1f
        MotionChoice.REDUCED -> 0.6f
        MotionChoice.OFF -> 0f
    }
    fun motionMs(base: Int): Int = (base * motionScale).toInt()
    val enterEasing = AppMotion.enterEasing()
    val exitEasing = AppMotion.exitEasing()

    // 玻璃底栏的采样源：整屏「背景 + 页面」录进一层 GPU 图层，底栏再折射它。
    // 底栏自己必须留在这一层之外（见 LifeLedgerTheme 的 overlay），否则会采样到自己。
    val glassBackdrop = rememberLiquidGlassBackdrop()
    var appLocked by remember { mutableStateOf(false) }
    var showAddOptions by remember { mutableStateOf(false) }

    val themeBackStackEntry by navController.currentBackStackEntryAsState()
    val themeRoute = themeBackStackEntry?.destination?.route
    val themePickMode = themeBackStackEntry?.arguments?.getBoolean("pick") == true
    val themeSelectedTab = BottomTab.entries.indexOfFirst { it.route == themeRoute }
    // 第一次打开（还没选从哪里开始）不出底栏；二级页面、挑图鉴、锁屏同理
    val barVisible = startChoice != StartChoice.UNSET &&
        themeSelectedTab >= 0 &&
        !themePickMode &&
        !appLocked

    LifeLedgerTheme(
        darkTheme = darkTheme,
        display = display,
        backgroundImage = backgroundImage,
        backgroundImageSet = backgroundImageUri != null,
        backgroundImageOpacity = backgroundImageOpacity,
        contentModifier = Modifier.liquidGlassBackdrop(glassBackdrop, enabled = barVisible),
        overlay = {
            if (barVisible) {
                AppBottomBar(
                    items = BottomTab.entries.map { tab ->
                        AppBottomBarItem(
                            icon = tab.icon,
                            label = stringResource(tab.labelRes)
                        )
                    },
                    selectedIndex = themeSelectedTab,
                    onSelect = { index ->
                        navController.navigateToTab(BottomTab.entries[index].navigateRoute)
                    },
                    backdrop = glassBackdrop,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    // 「记录成就」放在四个 tab 正中间：它是动作，不是页面
                    centerAction = AppBottomBarAction(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_title),
                        onClick = { showAddOptions = true }
                    )
                )
            }
        }
    ) {
        if (startChoice == StartChoice.UNSET) {
            // 第一次打开：先问一句从哪里开始；选完这个值就变了，界面自然切到主页
            OnboardingRoute()
            return@LifeLedgerTheme
        }

        // 底栏浮在内容上，页面要留出它压住的高度，滚到末尾才不会把最后一条压在玻璃下面
        val bottomBarClearance = if (barVisible) Sizes.bottomBar + Spacing.xl else 0.dp

        AppLockGate(
            enabled = appLockEnabled,
            pinSet = appPinHash != null,
            verifyPin = { pin -> AppPin.verify(pin, appPinHash) },
            onLockedChange = { appLocked = it }
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = AppTheme.pageColor) {
                Scaffold(
                    containerColor = Color.Transparent,
                    // 顶部内边距由各个页面自己处理（大标题顶栏 / 紧凑顶栏）；
                    // 这一层不再给底栏留位置：底栏是浮在上面的，内容要从玻璃底下穿过去
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    CompositionLocalProvider(
                        LocalBottomBarClearance provides bottomBarClearance
                    ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_HOME,
                        modifier = Modifier.padding(
                            bottom = innerPadding.calculateBottomPadding()
                        ),
                        // 克制转场：小幅横移 + 淡入淡出，不用整屏滑动
                        enterTransition = {
                            fadeIn(tween(motionMs(AppMotion.Medium))) +
                                slideInHorizontally(
                                    animationSpec = tween(
                                        motionMs(AppMotion.Medium),
                                        easing = enterEasing
                                    ),
                                    initialOffsetX = { it / 8 }
                                )
                        },
                        exitTransition = {
                            fadeOut(tween(motionMs(AppMotion.Base))) +
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        motionMs(AppMotion.Base),
                                        easing = exitEasing
                                    ),
                                    targetOffsetX = { -it / 12 }
                                )
                        },
                        popEnterTransition = {
                            fadeIn(tween(motionMs(AppMotion.Medium))) +
                                slideInHorizontally(
                                    animationSpec = tween(
                                        motionMs(AppMotion.Medium),
                                        easing = enterEasing
                                    ),
                                    initialOffsetX = { -it / 8 }
                                )
                        },
                        popExitTransition = {
                            fadeOut(tween(motionMs(AppMotion.Base))) +
                                slideOutHorizontally(
                                    animationSpec = tween(
                                        motionMs(AppMotion.Base),
                                        easing = exitEasing
                                    ),
                                    targetOffsetX = { it / 12 }
                                )
                        }
                    ) {
                        composable(ROUTE_HOME) {
                            HomeRoute(navController = navController)
                        }

                        composable(ROUTE_ACHIEVEMENTS) {
                            AllAchievementsRoute(navController = navController)
                        }

                        composable(
                            route = "achievement_detail/{achievementId}",
                            arguments = listOf(
                                navArgument("achievementId") { type = NavType.LongType }
                            )
                        ) { entry ->
                            val achievementId = entry.arguments?.getLong("achievementId")
                                ?: return@composable
                            AchievementDetailRoute(
                                achievementId = achievementId,
                                navController = navController
                            )
                        }

                        composable(
                            route = "add_achievement?presetId={presetId}",
                            arguments = listOf(
                                navArgument("presetId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { entry ->
                            AddAchievementRoute(
                                navController = navController,
                                presetId = entry.arguments?.getLong("presetId") ?: -1L
                            )
                        }

                        composable(
                            route = ROUTE_CODEX,
                            arguments = listOf(
                                navArgument("pick") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) { entry ->
                            PresetCodexRoute(
                                navController = navController,
                                pickMode = entry.arguments?.getBoolean("pick") ?: false
                            )
                        }

                        composable("backup") {
                            BackupRoute(navController = navController)
                        }

                        composable("profile") {
                            ProfileRoute(navController = navController)
                        }

                        composable("achievement_settings") {
                            AchievementSettingsRoute(navController = navController)
                        }

                        composable("reminder") {
                            ReminderRoute(navController = navController)
                        }

                        composable("data_security") {
                            DataSecurityRoute(navController = navController)
                        }

                        composable(ROUTE_SETTINGS) {
                            SettingsRoute(navController = navController)
                        }
                    }
                    }
                }

                if (showAddOptions) {
                    AddOptionsSheet(
                        onDismiss = { showAddOptions = false },
                        onPickFromCodex = {
                            showAddOptions = false
                            navController.navigate(ROUTE_CODEX_PICK)
                        },
                        onWriteCustom = {
                            showAddOptions = false
                            navController.navigate("add_achievement?presetId=-1")
                        }
                    )
                }
            }
        }
    }
}
