package com.Anchored.mylife.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.ThemeMode
import com.Anchored.mylife.ui.theme.AppMotion
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme

/**
 * 导航图。
 *
 * 除了路由，这里还负责两件全局的事：
 * - 主题：根据用户的深色模式偏好决定 light / dark
 * - 应用锁：把锁屏盖在所有页面之上
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

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LifeLedgerTheme(darkTheme = darkTheme) {
        AppLockGate(enabled = appLockEnabled) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = AppTheme.colors.background
            ) {
                NavHost(
                    navController = navController,
                    // 首页只负责总览；成就列表独立在 "all_achievements"
                    startDestination = "home",
                    // 克制转场：小幅横移 + 淡入淡出，不用整屏滑动
                    enterTransition = {
                        fadeIn(tween(AppMotion.Medium)) +
                            slideInHorizontally(
                                animationSpec = tween(
                                    AppMotion.Medium,
                                    easing = AppMotion.Decelerate
                                ),
                                initialOffsetX = { it / 8 }
                            )
                    },
                    exitTransition = {
                        fadeOut(tween(AppMotion.Base)) +
                            slideOutHorizontally(
                                animationSpec = tween(AppMotion.Base),
                                targetOffsetX = { -it / 12 }
                            )
                    },
                    popEnterTransition = {
                        fadeIn(tween(AppMotion.Medium)) +
                            slideInHorizontally(
                                animationSpec = tween(
                                    AppMotion.Medium,
                                    easing = AppMotion.Decelerate
                                ),
                                initialOffsetX = { -it / 8 }
                            )
                    },
                    popExitTransition = {
                        fadeOut(tween(AppMotion.Base)) +
                            slideOutHorizontally(
                                animationSpec = tween(AppMotion.Base),
                                targetOffsetX = { it / 12 }
                            )
                    }
                ) {
                    composable("home") {
                        HomeRoute(navController = navController)
                    }

                    composable("all_achievements") {
                        AllAchievementsRoute(navController = navController)
                    }

                    composable(
                        route = "achievement_detail/{achievementId}",
                        arguments = listOf(
                            navArgument("achievementId") { type = NavType.LongType }
                        )
                    ) { backStackEntry ->
                        val achievementId = backStackEntry.arguments?.getLong("achievementId")
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
                    ) { backStackEntry ->
                        AddAchievementRoute(
                            navController = navController,
                            presetId = backStackEntry.arguments?.getLong("presetId") ?: -1L
                        )
                    }

                    composable(
                        route = "preset_achievements?pick={pick}",
                        arguments = listOf(
                            navArgument("pick") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        PresetCodexRoute(
                            navController = navController,
                            pickMode = backStackEntry.arguments?.getBoolean("pick") ?: false
                        )
                    }

                    composable("backup") {
                        BackupRoute(navController = navController)
                    }

                    composable("settings") {
                        SettingsRoute(navController = navController)
                    }
                }
            }
        }
    }
}
