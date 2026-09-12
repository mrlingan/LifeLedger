package com.Anchored.mylife.ui

import android.text.format.DateFormat
import com.Anchored.mylife.R

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.ui.components.AnimatedMetricNumber
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppTopBarStyle
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.StatTile
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 首页：人生 Dashboard。
 *
 * 只有「总览」，从上到下：
 *   问候 → 核心数字与完成度 → 最近解锁 → 人生数据 → 全部成就入口
 *
 * 刻意不放成就列表：首页要回答的是「我已经完成了多少」，
 * 列表（含筛选和逐条勾选）在独立的「全部成就」页。
 * 也不放 FAB，主操作放在顶栏。
 */
@Composable
fun HomeRoute(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddOptions by remember { mutableStateOf(false) }

    HomeScreen(
        uiState = uiState,
        onAchievementClick = { id -> navController.navigate("achievement_detail/$id") },
        onAddClick = { showAddOptions = true },
        onOpenCodex = { navController.navigate("preset_achievements?pick=false") },
        onOpenSettings = { navController.navigate("settings") },
        onOpenAllAchievements = { navController.navigate("all_achievements") }
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
    onAchievementClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllAchievements: () -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(containerColor = colors.background) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Spacing.xxxl)
        ) {
            item(key = "topbar") {
                AppTopBar(
                    title = stringResource(R.string.home_title),
                    style = AppTopBarStyle.Large,
                    subtitle = rememberTodayText(),
                    // 状态栏内边距由外层 Scaffold 处理，这里不要再叠一次
                    withStatusBarPadding = false,
                    actions = {
                        AppIconButton(
                            icon = Icons.Outlined.Star,
                            contentDescription = stringResource(R.string.codex_title),
                            onClick = onOpenCodex
                        )
                        AppIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            onClick = onOpenSettings
                        )
                        AppIconButton(
                            icon = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.add_title),
                            onClick = onAddClick
                        )
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
                item(key = "hero") {
                    HeroSection(uiState = uiState)
                }

                if (uiState.recentlyUnlocked.isNotEmpty()) {
                    item(key = "recent_header") {
                        SectionHeader(
                            title = stringResource(R.string.home_recent),
                            modifier = Modifier.padding(
                                start = Sizes.gutter,
                                end = Sizes.gutter,
                                top = Spacing.xl,
                                bottom = Spacing.md
                            )
                        )
                    }
                    item(key = "recent_row") {
                        RecentUnlockedRow(
                            items = uiState.recentlyUnlocked,
                            onClick = onAchievementClick
                        )
                    }
                }

                item(key = "life_stats") {
                    LifeStatsSection(uiState = uiState)
                }

                item(key = "all_achievements_entry") {
                    AllAchievementsEntry(
                        uiState = uiState,
                        onClick = onOpenAllAchievements
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSection(uiState: HomeUiState) {
    val colors = AppTheme.colors
    val percent = (uiState.completionRate * 100).roundToInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = Sizes.gutter,
                end = Sizes.gutter,
                top = Spacing.sm,
                bottom = Spacing.lg
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            AnimatedMetricNumber(
                value = uiState.completedCount,
                style = AppTheme.type.display,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.home_hero_label),
                style = AppTheme.type.body,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = if (uiState.inProgressCount > 0) {
                    stringResource(
                        R.string.home_hero_summary_progress,
                        uiState.totalCount,
                        uiState.inProgressCount
                    )
                } else {
                    stringResource(R.string.home_hero_summary, uiState.totalCount)
                },
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }

        AppProgressRing(
            progress = uiState.completionRate,
            diameter = Sizes.progressRingSize
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
                    style = AppTheme.type.numberMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.home_completion),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    }
}

@Composable
private fun RecentUnlockedRow(
    items: List<Achievement>,
    onClick: (Long) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Sizes.gutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        items(items = items, key = { it.id }) { achievement ->
            RecentUnlockedTile(
                achievement = achievement,
                onClick = { onClick(achievement.id) }
            )
        }
    }
}

@Composable
private fun RecentUnlockedTile(
    achievement: Achievement,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.lg)

    Column(
        modifier = Modifier
            .width(Sizes.recentTile)
            .clip(shape)
            .clickable(onClick = onClick),
        // 图标、标题、日期统一居中
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.avatarLg)
                .clip(shape)
                .background(colors.surfaceElevated)
                .border(Sizes.hairline, colors.border, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.iconEmoji,
                style = AppTheme.type.h2
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = achievement.title,
            style = AppTheme.type.bodySmall,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xxs))

        Text(
            text = achievement.completedDate?.let { formatDay(it) }.orEmpty(),
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LifeStatsSection(uiState: HomeUiState) {
    AppCard(
        modifier = Modifier.padding(
            start = Sizes.gutter,
            end = Sizes.gutter,
            top = Spacing.xxl
        )
    ) {
        SectionHeader(title = stringResource(R.string.home_stats))

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatTile(
                value = uiState.totalCount.toString(),
                label = stringResource(R.string.home_stat_total),
                modifier = Modifier.weight(1f)
            )

            AppVerticalDivider(height = Spacing.xxl)

            StatTile(
                value = uiState.streakDays.toString(),
                label = stringResource(R.string.home_stat_streak),
                supporting = if (uiState.streakDays > 0) stringResource(R.string.home_stat_days) else null,
                modifier = Modifier.weight(1f)
            )

            AppVerticalDivider(height = Spacing.xxl)

            StatTile(
                value = uiState.unlockedThisWeek.toString(),
                label = stringResource(R.string.home_stat_week),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 「全部成就」入口。
 *
 * 只放一个入口，不把列表铺在首页——列表有自己的一页，
 * 首页的篇幅因此不再随成就数量增长。
 */
@Composable
private fun AllAchievementsEntry(
    uiState: HomeUiState,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.padding(
            start = Sizes.gutter,
            end = Sizes.gutter,
            top = Spacing.xxl
        ),
        // 内部交给 AppSettingRow 排版，卡片本身不再叠一层内边距
        contentPadding = PaddingValues(0.dp),
        onClick = onClick
    ) {
        AppSettingRow(
            title = stringResource(R.string.home_all),
            subtitle = stringResource(
                R.string.home_all_desc,
                uiState.totalCount,
                uiState.inProgressCount
            ),
            showChevron = true
        )
    }
}

/** 日期按系统语言格式化，问候语取自资源，随语言切换 */
@Composable
private fun rememberTodayText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..5 -> stringResource(R.string.home_greeting_dawn)
        in 6..11 -> stringResource(R.string.home_greeting_morning)
        in 12..17 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
    val date = remember {
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMdEEEE")
        SimpleDateFormat(pattern, locale).format(Date())
    }
    return "$date · $greeting"
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewHomeAchievements = listOf(
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
        iconEmoji = "🏊"
    ),
    Achievement(
        id = 3,
        title = "出版自己的第一本书",
        description = "写了两年的稿子终于下印",
        createdDate = 1_735_689_600_000,
        completedDate = null,
        isCompleted = false,
        iconEmoji = "📚"
    )
)

private val previewHomeState = HomeUiState(
    isLoaded = true,
    totalCount = 12,
    completedCount = 5,
    inProgressCount = 7,
    completionRate = 5f / 12f,
    streakDays = 9,
    unlockedThisWeek = 3,
    recentlyUnlocked = previewHomeAchievements.filter { it.isCompleted }
)

@Preview(showBackground = true, heightDp = 900, name = "首页 · 有数据")
@Composable
private fun HomeScreenPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = previewHomeState,
            onAchievementClick = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {},
            onOpenAllAchievements = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "首页 · 空状态")
@Composable
private fun HomeScreenEmptyPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = HomeUiState(isLoaded = true),
            onAchievementClick = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {},
            onOpenAllAchievements = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900, name = "首页 · 深色")
@Composable
private fun HomeScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        HomeScreen(
            uiState = previewHomeState,
            onAchievementClick = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {},
            onOpenAllAchievements = {}
        )
    }
}
