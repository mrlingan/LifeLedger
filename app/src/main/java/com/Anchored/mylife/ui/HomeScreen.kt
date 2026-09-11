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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.ui.components.AchievementCard
import com.Anchored.mylife.ui.components.AchievementStatus
import com.Anchored.mylife.ui.components.AnimatedMetricNumber
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppTopBarStyle
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.StatTile
import com.Anchored.mylife.ui.components.appearAnimation
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
 * 结构是「总览 → 重点 → 最近 → 数据 → 细节」：
 *   问候 → 核心数字与完成度 → 最近解锁 → 人生数据 → 继续完成 → 全部成就
 *
 * 刻意没有 FAB，主操作放在顶栏；页面不再用大色块和渐变。
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
        onToggleCompleted = viewModel::toggleCompleted,
        onFilterChange = viewModel::setFilter,
        onAddClick = { showAddOptions = true },
        onOpenCodex = { navController.navigate("preset_achievements?pick=false") },
        onOpenSettings = { navController.navigate("settings") }
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
    onToggleCompleted: (Long) -> Unit,
    onFilterChange: (AchievementFilter) -> Unit,
    onAddClick: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenSettings: () -> Unit
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
                            AppButton(text = stringResource(R.string.home_empty_action), onClick = onAddClick)
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

                if (uiState.inProgressPreview.isNotEmpty()) {
                    item(key = "in_progress_header") {
                        SectionHeader(
                            title = stringResource(R.string.home_continue),
                            subtitle = stringResource(R.string.home_continue_sub),
                            modifier = Modifier.padding(
                                start = Sizes.gutter,
                                end = Sizes.gutter,
                                top = Spacing.xxl,
                                bottom = Spacing.xs
                            )
                        )
                    }
                    itemsIndexed(
                        items = uiState.inProgressPreview,
                        key = { _, item -> "progress_${item.id}" }
                    ) { index, achievement ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = Sizes.gutter)
                                .appearAnimation(index)
                        ) {
                            AchievementRow(
                                achievement = achievement,
                                onClick = { onAchievementClick(achievement.id) },
                                onToggleCompleted = { onToggleCompleted(achievement.id) }
                            )
                        }
                    }
                }

                item(key = "all_header") {
                    SectionHeader(
                        title = stringResource(R.string.home_all),
                        subtitle = stringResource(R.string.home_count_items, uiState.totalCount),
                        modifier = Modifier.padding(
                            start = Sizes.gutter,
                            end = Sizes.gutter,
                            top = Spacing.xxl,
                            bottom = Spacing.md
                        )
                    )
                }

                item(key = "filter") {
                    AppSegmentedControl(
                    options = AchievementFilter.entries.map {
                        "${stringResource(it.labelRes)} ${uiState.countOf(it)}"
                    },
                        selectedIndex = AchievementFilter.entries.indexOf(uiState.filter),
                        onSelect = { index ->
                            onFilterChange(AchievementFilter.entries[index])
                        },
                        modifier = Modifier.padding(horizontal = Sizes.gutter)
                    )
                }

                if (uiState.visibleAchievements.isEmpty()) {
                    item(key = "filter_empty") {
                        EmptyState(title = stringResource(R.string.home_filter_empty))
                    }
                } else {
                    itemsIndexed(
                        items = uiState.visibleAchievements,
                        key = { _, item -> "all_${item.id}" }
                    ) { index, achievement ->
                        Column(
                            modifier = Modifier
                                .padding(horizontal = Sizes.gutter)
                                .appearAnimation(index)
                        ) {
                            AchievementRow(
                                achievement = achievement,
                                onClick = { onAchievementClick(achievement.id) },
                                onToggleCompleted = { onToggleCompleted(achievement.id) }
                            )
                            if (index < uiState.visibleAchievements.lastIndex) {
                                AppDivider()
                            }
                        }
                    }
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

@Composable
private fun AchievementRow(
    achievement: Achievement,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val colors = AppTheme.colors

    AchievementCard(
        title = achievement.title,
        description = achievement.description.takeIf { it.isNotBlank() },
        icon = {
            Text(
                text = achievement.iconEmoji,
                style = AppTheme.type.numberMedium
            )
        },
        status = if (achievement.isCompleted) {
            AchievementStatus.Completed
        } else {
            AchievementStatus.InProgress
        },
        meta = achievement.metaText(),
        onClick = onClick,
        trailing = {
            AppIconButton(
                icon = Icons.Outlined.CheckCircle,
                contentDescription = if (achievement.isCompleted) stringResource(R.string.detail_undo_complete) else stringResource(R.string.detail_timeline_done),
                onClick = onToggleCompleted,
                tint = if (achievement.isCompleted) colors.accent else colors.textTertiary
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOptionsSheet(
    onDismiss: () -> Unit,
    onPickFromCodex: () -> Unit,
    onWriteCustom: () -> Unit
) {
    val colors = AppTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = Radius.hero, topEnd = Radius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    bottom = Spacing.xxl
                )
        ) {
            Text(
                text = stringResource(R.string.home_sheet_title),
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.home_sheet_desc),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            AppCard(onClick = onPickFromCodex, tone = AppCardTone.Soft) {
                OptionRow(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.home_sheet_codex),
                    subtitle = stringResource(R.string.home_sheet_codex_desc)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            AppCard(onClick = onWriteCustom, tone = AppCardTone.Soft) {
                OptionRow(
                    icon = Icons.Outlined.Add,
                    title = stringResource(R.string.home_sheet_custom),
                    subtitle = stringResource(R.string.home_sheet_custom_desc)
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = AppTheme.colors

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(Sizes.iconLg)
        )

        Spacer(modifier = Modifier.width(Spacing.md))

        Column {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = subtitle,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }
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

private fun formatDay(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(millis))

@Composable
private fun Achievement.metaText(): String = if (isCompleted && completedDate != null) {
    stringResource(R.string.home_meta_completed, formatDay(completedDate))
} else {
    stringResource(R.string.home_meta_created, formatDay(createdDate))
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

@Preview(showBackground = true, heightDp = 1100, name = "首页 · 有数据")
@Composable
private fun HomeScreenPreview() {
    LifeLedgerTheme {
        HomeScreen(
            uiState = HomeUiState(
                all = previewHomeAchievements,
                isLoaded = true,
                totalCount = 12,
                completedCount = 5,
                inProgressCount = 7,
                completionRate = 5f / 12f,
                streakDays = 9,
                unlockedThisWeek = 3,
                recentlyUnlocked = previewHomeAchievements.filter { it.isCompleted },
                inProgress = previewHomeAchievements.filterNot { it.isCompleted }
            ),
            onAchievementClick = {},
            onToggleCompleted = {},
            onFilterChange = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {}
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
            onToggleCompleted = {},
            onFilterChange = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "首页 · 深色")
@Composable
private fun HomeScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        HomeScreen(
            uiState = HomeUiState(
                all = previewHomeAchievements,
                isLoaded = true,
                totalCount = 12,
                completedCount = 5,
                inProgressCount = 7,
                completionRate = 5f / 12f,
                streakDays = 9,
                unlockedThisWeek = 3,
                recentlyUnlocked = previewHomeAchievements.filter { it.isCompleted },
                inProgress = previewHomeAchievements.filterNot { it.isCompleted }
            ),
            onAchievementClick = {},
            onToggleCompleted = {},
            onFilterChange = {},
            onAddClick = {},
            onOpenCodex = {},
            onOpenSettings = {}
        )
    }
}
