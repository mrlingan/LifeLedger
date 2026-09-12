package com.Anchored.mylife.ui

import android.text.format.DateFormat
import com.Anchored.mylife.R

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.home.CategoryProgressSection
import com.Anchored.mylife.ui.home.HomeFooter
import com.Anchored.mylife.ui.home.HomeHeader
import com.Anchored.mylife.ui.home.LifeProgressSection
import com.Anchored.mylife.ui.home.OverviewMetrics
import com.Anchored.mylife.ui.home.RecentAchievementsSection
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页：人生仪表盘。
 *
 * 这一层只做组合，每段实现在 `ui/home/` 下：
 *   问候 → 人生进度 → 核心数据 → 分类进度 → 最近解锁 → 收尾
 *
 * 首页回答三个问题：我走到哪了、我完成了什么、我用它记录了多久。
 * 完整列表不在这里（见 AllAchievementsScreen），所以首页长度不随记录数增长。
 */
@Composable
fun HomeRoute(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presetTexts = rememberPresetTexts()
    var showAddOptions by remember { mutableStateOf(false) }

    HomeScreen(
        uiState = uiState,
        labelOfCategory = presetTexts::categoryOf,
        onAchievementClick = { id -> navController.navigate("achievement_detail/$id") },
        onAddClick = { showAddOptions = true },
        onOpenProfile = { navController.navigate("profile") },
        // 图鉴和成就列表都是底部导航的 tab，用 tab 切换保证选中态与返回栈一致
        onOpenCodex = { navController.navigateToTab(ROUTE_CODEX_BROWSE) },
        onOpenAllAchievements = { navController.navigateToTab(ROUTE_ACHIEVEMENTS) }
    )

    if (showAddOptions) {
        AddOptionsSheet(
            onDismiss = { showAddOptions = false },
            onPickFromCodex = {
                showAddOptions = false
                navController.navigate("preset_achievements?pick=true")
            },
            onWriteCustom = {
                showAddOptions = false
                navController.navigate("add_achievement?presetId=-1")
            }
        )
    }
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    labelOfCategory: (String) -> String,
    onAchievementClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenAllAchievements: () -> Unit
) {
    val colors = AppTheme.colors
    val profileLabel = stringResource(R.string.profile_title)

    Scaffold(containerColor = colors.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Spacing.xxxl)
        ) {
            item(key = "header") {
                HomeHeader(
                    // 设了昵称就用「你好，XX」，没设就用按时间段变化的问候语
                    title = if (uiState.nickname.isNotBlank()) {
                        stringResource(R.string.home_greeting_named, uiState.nickname)
                    } else {
                        rememberGreeting()
                    },
                    // 签名同理：用户写了就用他自己的，没写就退回内置那句
                    subtitle = uiState.signature.ifBlank { stringResource(R.string.home_motto) },
                    // 「记录成就」已经移到底部导航正中间，顶栏不再放按钮
                    eyebrow = rememberTodayDate(),
                    // 配过昵称或头像才显示：新用户第一眼不会看到一个空的人形占位
                    avatar = if (
                        uiState.nickname.isNotBlank() || uiState.avatarPath != null
                    ) {
                        {
                            AppAvatar(
                                name = uiState.nickname,
                                path = uiState.avatarPath,
                                size = Sizes.avatarMd,
                                modifier = Modifier
                                    .clickable(onClick = onOpenProfile)
                                    // 头像是个可点入口，给读屏一个名字
                                    .semantics {
                                        contentDescription = profileLabel
                                    }
                            )
                        }
                    } else {
                        null
                    }
                )
            }

            if (uiState.totalCount == 0 && uiState.isLoaded) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        description = stringResource(R.string.home_empty_desc),
                        action = {
                            AppButton(
                                text = stringResource(R.string.home_empty_action),
                                onClick = onAddClick
                            )
                        }
                    )
                }
            } else {
                item(key = "life_progress") {
                    LifeProgressSection(uiState = uiState)
                }

                item(key = "overview") {
                    OverviewMetrics(
                        uiState = uiState,
                        modifier = Modifier.padding(top = Spacing.xxl)
                    )
                }

                item(key = "categories") {
                    CategoryProgressSection(
                        items = uiState.categories,
                        unlockedCount = uiState.codexUnlocked,
                        totalCount = uiState.codexTotal,
                        labelOf = labelOfCategory,
                        onViewAll = onOpenCodex,
                        modifier = Modifier.padding(top = Spacing.xxl)
                    )
                }

                item(key = "recent") {
                    RecentAchievementsSection(
                        items = uiState.recent,
                        onClick = onAchievementClick,
                        onViewAll = onOpenAllAchievements,
                        modifier = Modifier.padding(top = Spacing.xxl)
                    )
                }

                item(key = "footer") {
                    HomeFooter(
                        recordLine = rememberRecordLine(
                            firstRecordDate = uiState.firstRecordDate,
                            recordedDays = uiState.recordedDays
                        ),
                        modifier = Modifier.padding(top = Spacing.xxxl)
                    )
                }
            }
        }
    }
}

/** 问候语随时间变化，文案两边都有资源 */
@Composable
private fun rememberGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return stringResource(
        when (hour) {
            in 0..5 -> R.string.home_greeting_dawn
            in 6..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    )
}

/** 日期按系统语言格式化 */
@Composable
private fun rememberTodayDate(): String {
    val locale = Locale.getDefault()
    return remember(locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMdEEEE")
        SimpleDateFormat(pattern, locale).format(Date())
    }
}

/** 「从 2024年3月 开始记录 · 已记录 890 天」 */
@Composable
private fun rememberRecordLine(firstRecordDate: Long?, recordedDays: Int): String? {
    if (firstRecordDate == null) return null

    val locale = Locale.getDefault()
    val startDate = remember(firstRecordDate, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "yMMM")
        SimpleDateFormat(pattern, locale).format(Date(firstRecordDate))
    }
    return stringResource(R.string.home_record_line, startDate, recordedDays)
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewAchievements = listOf(
    Achievement(
        id = 1,
        title = "完成人生第一场马拉松",
        description = "42.195 公里，净成绩 4 小时 12 分",
        createdDate = 1_735_689_600_000,
        completedDate = 1_747_008_000_000,
        isCompleted = true,
        iconEmoji = "🏅"
    ),
    Achievement(
        id = 2,
        title = "学会游泳",
        description = "从不敢下水到能连续游 1000 米",
        createdDate = 1_735_689_600_000,
        completedDate = 1_746_000_000_000,
        isCompleted = true,
        iconEmoji = "🏊",
        presetId = 86L
    ),
    Achievement(
        id = 3,
        title = "出版自己的第一本书",
        description = "写了两年的稿子终于下印",
        createdDate = 1_700_000_000_000,
        completedDate = 1_744_000_000_000,
        isCompleted = true,
        iconEmoji = "📚",
        presetId = 6L
    )
)

private val previewHomeState = HomeUiState(
    isLoaded = true,
    totalCount = 12,
    completedCount = 8,
    inProgressCount = 4,
    completionRate = 8f / 12f,
    level = 2,
    toNextLevel = 2,
    streakDays = 4,
    recordedDays = 6,
    codexUnlocked = 5,
    codexTotal = 109,
    categories = listOf(
        CategoryProgress("技能", 3, 7),
        CategoryProgress("兴趣", 1, 8),
        CategoryProgress("旅行", 1, 10),
        CategoryProgress("成长", 0, 19),
        CategoryProgress("生活", 0, 13)
    ),
    recent = listOf(
        RecentAchievement(previewAchievements[0], RarityTier.Platinum),
        RecentAchievement(previewAchievements[1], RarityTier.Bronze),
        RecentAchievement(previewAchievements[2], null)
    ),
    firstRecordDate = 1_700_000_000_000
)

@Preview(showBackground = true, heightDp = 1200, name = "首页 · 有数据")
@Composable
private fun HomeScreenPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = previewHomeState,
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onOpenProfile = {},
            onOpenCodex = {},
            onOpenAllAchievements = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "首页 · 空状态")
@Composable
private fun HomeScreenEmptyPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = HomeUiState(isLoaded = true),
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onOpenProfile = {},
            onOpenCodex = {},
            onOpenAllAchievements = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "首页 · 深色")
@Composable
private fun HomeScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        HomeScreen(
            uiState = previewHomeState,
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onOpenProfile = {},
            onOpenCodex = {},
            onOpenAllAchievements = {}
        )
    }
}
