package com.Anchored.mylife.ui

import android.text.format.DateFormat
import com.Anchored.mylife.R

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.home.CategoryProgressSection
import com.Anchored.mylife.ui.home.HomeFooter
import com.Anchored.mylife.ui.home.HomeHeader
import com.Anchored.mylife.ui.home.HomeImageSection
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
 * 问候语固定在最上方，下面几段（人生进度 / 核心数据 / 分类进度 / 最近解锁 / 收尾）
 * 由用户在「设置 → 首页板块」里决定显示哪些、按什么顺序，这里照单渲染。
 * 每段实现在 `ui/home/` 下。
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
    val sections by viewModel.homeSections.collectAsStateWithLifecycle()
    val categoryColors by viewModel.categoryColors.collectAsStateWithLifecycle()
    val homeImagePath by viewModel.homeImagePath.collectAsStateWithLifecycle()
    val presetTexts = rememberPresetTexts()
    var showAddOptions by remember { mutableStateOf(false) }
    var showLevelProgress by remember { mutableStateOf(false) }

    HomeScreen(
        uiState = uiState,
        sections = sections,
        labelOfCategory = presetTexts::categoryOf,
        colorOfCategory = { category -> storedColorToColor(categoryColors[category]) },
        homeImagePath = homeImagePath,
        onAchievementClick = { id -> navController.navigate("achievement_detail/$id") },
        onAddClick = { showAddOptions = true },
        onLevelClick = { showLevelProgress = true },
        // 图鉴和成就列表都是底部导航的 tab，用 tab 切换保证选中态与返回栈一致
        onOpenCodex = { navController.navigateToTab(ROUTE_CODEX_BROWSE) },
        onOpenAllAchievements = { navController.navigateToTab(ROUTE_ACHIEVEMENTS) },
        // 板块全被隐藏时，首页给一个直达入口，不用自己绕到设置里找
        onOpenHomeLayout = { navController.navigate(ROUTE_HOME_LAYOUT) }
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
    if (showLevelProgress) {
        AppDialog(
            title = stringResource(R.string.home_level_dialog_title),
            onDismissRequest = { showLevelProgress = false },
            onConfirm = { showLevelProgress = false },
            confirmText = stringResource(R.string.common_got_it),
            dismissText = null
        ) {
            // 等级体系还没做，先给一句实话；正文包在 Column 里，
            // 因为对话框的正文槽是 Box，直接放多个子项会叠在一起
            Column {
                AppDialogText(stringResource(R.string.common_coming_soon))
            }
        }
    }
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    sections: List<HomeSection>,
    labelOfCategory: (String) -> String,
    /** 分类圆环的颜色；没挑过的分类返回 null，用主题强调色 */
    colorOfCategory: (String) -> Color? = { null },
    /** 自定义图片那一张卡片的图；null = 没上传过，那一段不显示 */
    homeImagePath: String? = null,
    onAchievementClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onLevelClick: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenAllAchievements: () -> Unit,
    onOpenHomeLayout: () -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(containerColor = AppTheme.pageColor) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // 底栏是浮在内容上的：末尾再多留出它压住的高度
            contentPadding = PaddingValues(
                bottom = Spacing.xxxl + LocalBottomBarClearance.current
            )
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
                    // 配过昵称或头像才显示：新用户第一眼不会看到一个空的人形占位。
                    // 头像只作展示，点了不跳转——个人资料的入口在设置页顶部那张资料卡上
                    // （首页这一块是"仪表盘"，不该藏一个会把人带走的按钮）
                    avatar = if (
                        uiState.nickname.isNotBlank() ||
                        uiState.avatarPath != null ||
                        uiState.avatarPreset != null
                    ) {
                        {
                            AppAvatar(
                                name = uiState.nickname,
                                path = uiState.avatarPath,
                                preset = uiState.avatarPreset,
                                size = Sizes.avatarMd
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
                // 板块由用户排序：按配置逐个铺开，key 用板块名，
                // 改顺序时列表项不会因为复用而串内容
                if (sections.isEmpty()) {
                    item(key = "sections_hidden") {
                        EmptyState(
                            title = stringResource(R.string.home_sections_hidden_title),
                            description = stringResource(R.string.home_sections_hidden_desc),
                            action = {
                                AppButton(
                                    text = stringResource(R.string.home_sections_hidden_action),
                                    onClick = onOpenHomeLayout
                                )
                            }
                        )
                    }
                }

                sections.forEachIndexed { index, section ->
                    item(key = section.name) {
                        HomeSectionBlock(
                            section = section,
                            uiState = uiState,
                            labelOfCategory = labelOfCategory,
                            colorOfCategory = colorOfCategory,
                            homeImagePath = homeImagePath,
                            onAchievementClick = onAchievementClick,
                            onLevelClick = onLevelClick,
                            onOpenCodex = onOpenCodex,
                            onOpenAllAchievements = onOpenAllAchievements,
                            // 第一段紧接问候语（留白由问候语自己带），
                            // 之后每段按原来的节奏分隔
                            modifier = Modifier.padding(
                                top = when {
                                    index == 0 -> 0.dp
                                    section == HomeSection.FOOTER -> Spacing.xxxl
                                    else -> Spacing.xxl
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 首页中的一段。
 *
 * 每段的留白、数据都在各自实现里，这里只做「板块 → 内容」的映射，
 * 所以加新板块时：先在 [HomeSection] 里加一项，再来这里补一个分支。
 */
@Composable
private fun HomeSectionBlock(
    section: HomeSection,
    uiState: HomeUiState,
    labelOfCategory: (String) -> String,
    colorOfCategory: (String) -> Color?,
    homeImagePath: String?,
    onAchievementClick: (Long) -> Unit,
    onLevelClick: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenAllAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (section) {
        HomeSection.LIFE_PROGRESS -> LifeProgressSection(
            uiState = uiState,
            onClick = onLevelClick,
            modifier = modifier
        )

        HomeSection.OVERVIEW -> OverviewMetrics(
            uiState = uiState,
            modifier = modifier
        )

        HomeSection.CATEGORIES -> CategoryProgressSection(
            items = uiState.categories,
            unlockedCount = uiState.codexUnlocked,
            totalCount = uiState.codexTotal,
            labelOf = labelOfCategory,
            colorOf = colorOfCategory,
            onViewAll = onOpenCodex,
            modifier = modifier
        )

        HomeSection.CUSTOM_IMAGE -> HomeImageSection(
            path = homeImagePath,
            modifier = modifier
        )

        HomeSection.RECENT -> RecentAchievementsSection(
            items = uiState.recent,
            onClick = onAchievementClick,
            onViewAll = onOpenAllAchievements,
            modifier = modifier
        )

        HomeSection.FOOTER -> HomeFooter(
            recordLine = rememberRecordLine(
                firstRecordDate = uiState.firstRecordDate,
                recordedDays = uiState.recordedDays
            ),
            modifier = modifier
        )
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
            sections = HomeSection.DEFAULT_ORDER,
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onLevelClick = {},
            onOpenCodex = {},
            onOpenAllAchievements = {},
            onOpenHomeLayout = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "首页 · 空状态")
@Composable
private fun HomeScreenEmptyPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = HomeUiState(isLoaded = true),
            sections = HomeSection.DEFAULT_ORDER,
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onLevelClick = {},
            onOpenCodex = {},
            onOpenAllAchievements = {},
            onOpenHomeLayout = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "首页 · 自定义板块（已隐藏两项）")
@Composable
private fun HomeScreenCustomSectionsPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = previewHomeState,
            // 用户把「人生进度」和收尾一行关掉了，其余顺序也换过
            sections = listOf(
                HomeSection.OVERVIEW,
                HomeSection.RECENT,
                HomeSection.CATEGORIES
            ),
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onLevelClick = {},
            onOpenCodex = {},
            onOpenAllAchievements = {},
            onOpenHomeLayout = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "首页 · 深色")
@Composable
private fun HomeScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        HomeScreen(
            uiState = previewHomeState,
            sections = HomeSection.DEFAULT_ORDER,
            labelOfCategory = { it },
            onAchievementClick = {},
            onAddClick = {},
            onLevelClick = {},
            onOpenCodex = {},
            onOpenAllAchievements = {},
            onOpenHomeLayout = {}
        )
    }
}
