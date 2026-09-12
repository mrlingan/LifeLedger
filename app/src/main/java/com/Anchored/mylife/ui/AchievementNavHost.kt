package com.Anchored.mylife.ui

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.data.settings.StartChoice
import com.Anchored.mylife.data.settings.MotionChoice
import com.Anchored.mylife.ui.components.AppBottomBar
import com.Anchored.mylife.ui.components.AppBottomBarAction
import com.Anchored.mylife.ui.components.AppBottomBarItem
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.AppDisplay
import com.Anchored.mylife.ui.theme.LifeLedgerTheme

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
    val startChoice by appSettings.startChoice.collectAsStateWithLifecycle()
    val listDensity by appSettings.listDensity.collectAsStateWithLifecycle()
    val fontScale by appSettings.fontScale.collectAsStateWithLifecycle()
    val motion by appSettings.motion.collectAsStateWithLifecycle()

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
        MotionChoice.FULL -> 1f
        MotionChoice.REDUCED -> 0.6f
        MotionChoice.OFF -> 0f
    }
    fun motionMs(base: Int): Int = (base * motionScale).toInt()

    LifeLedgerTheme(darkTheme = darkTheme, display = display) {
        if (startChoice == StartChoice.UNSET) {
            // 第一次打开：先问一句从哪里开始；选完这个值就变了，界面自然切到主页
            OnboardingRoute()
            return@LifeLedgerTheme
        }

        AppLockGate(enabled = appLockEnabled) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppTheme.colors.background
            ) {
                var showAddOptions by remember { mutableStateOf(false) }
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route
                val pickMode = backStackEntry?.arguments?.getBoolean("pick") == true
                val selectedTab = BottomTab.entries.indexOfFirst { it.route == currentRoute }

                Scaffold(
                    containerColor = Color.Transparent,
                    // 顶部内边距由各个页面自己处理（大标题顶栏 / 紧凑顶栏）；
                    // 这一层只负责给底部导航留出位置，不重复消耗系统栏
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (selectedTab >= 0 && !pickMode) {
                            AppBottomBar(
                                items = BottomTab.entries.map { tab ->
                                    AppBottomBarItem(
                                        icon = tab.icon,
                                        label = stringResource(tab.labelRes)
                                    )
                                },
                                selectedIndex = selectedTab,
                                onSelect = { index ->
                                    navController.navigateToTab(
                                        BottomTab.entries[index].navigateRoute
                                    )
                                },
                                // 「记录成就」放在四个 tab 正中间：它是动作，不是页面
                                centerAction = AppBottomBarAction(
                                    icon = Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.add_title),
                                    onClick = { showAddOptions = true }
                                )
                            )
                        }
                    }
                ) { innerPadding ->
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
                                        easing = AppMotion.Decelerate
                                    ),
                                    initialOffsetX = { it / 8 }
                                )
                        },
                        exitTransition = {
                            fadeOut(tween(motionMs(AppMotion.Base))) +
                                slideOutHorizontally(
                                    animationSpec = tween(motionMs(AppMotion.Base)),
                                    targetOffsetX = { -it / 12 }
                                )
                        },
                        popEnterTransition = {
                            fadeIn(tween(motionMs(AppMotion.Medium))) +
                                slideInHorizontally(
                                    animationSpec = tween(
                                        motionMs(AppMotion.Medium),
                                        easing = AppMotion.Decelerate
                                    ),
                                    initialOffsetX = { -it / 8 }
                                )
                        },
                        popExitTransition = {
                            fadeOut(tween(motionMs(AppMotion.Base))) +
                                slideOutHorizontally(
                                    animationSpec = tween(motionMs(AppMotion.Base)),
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
