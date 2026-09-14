package com.Anchored.mylife.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppIcons
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppProgressRing
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.components.LevelPill
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.label
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * 底栏的「我的」。
 *
 * 这一页只回答一个问题：我攒下了什么。从上到下依次是
 * 我是谁（页头）→ 四个总览数字 → 阶段进度 → 图鉴收集 → 数据量 → 去哪看更多 → 设置入口。
 *
 * 和首页的分工：首页是今天的人生仪表盘，摆的是进度和最近解锁；
 * 这一页是我的档案，摆的是画像、收藏和数据量。两页共用的统计口径在 LifeStats.kt。
 */
@Composable
fun MyRoute(
    navController: NavHostController,
    viewModel: MyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MyScreen(
        uiState = uiState,
        onOpenProfile = { navController.navigate(ROUTE_PROFILE) },
        onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
        onOpenReminder = { navController.navigate("reminder") },
        onOpenBackup = { navController.navigate("backup") },
        // 成就列表、图鉴、成长都是底栏的 tab：用 tab 切换，选中态和返回栈才对得上
        onOpenAllAchievements = { navController.navigateToTab(ROUTE_ACHIEVEMENTS) },
        onOpenCodex = { navController.navigateToTab(ROUTE_CODEX_BROWSE) },
        onOpenGrowth = { navController.navigateToTab(ROUTE_GROWTH) },
        // 商城是二级页面：从这一页进去，返回就回到「我的」
        onOpenStore = { navController.navigate(ROUTE_REWARD_STORE) }
    )
}

@Composable
fun MyScreen(
    uiState: MyUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAllAchievements: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenStore: () -> Unit
) {
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
            item(key = "profile") {
                MyProfileHeader(
                    uiState = uiState,
                    onOpenProfile = onOpenProfile,
                    onOpenSettings = onOpenSettings
                )
            }

            item(key = "stats") {
                MyStatsCard(uiState = uiState, modifier = Modifier.padding(top = Spacing.xl))
            }

            item(key = "level") {
                MyLevelCard(uiState = uiState, modifier = Modifier.padding(top = Spacing.lg))
            }

            item(key = "collection") {
                MyCollectionCard(
                    uiState = uiState,
                    onViewAll = onOpenCodex,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }

            item(key = "life") {
                MyLifeCard(uiState = uiState, modifier = Modifier.padding(top = Spacing.lg))
            }

            item(key = "quick") {
                MyQuickActions(
                    onOpenAllAchievements = onOpenAllAchievements,
                    onOpenCodex = onOpenCodex,
                    onOpenGrowth = onOpenGrowth,
                    onOpenStore = onOpenStore,
                    modifier = Modifier.padding(top = Spacing.xl)
                )
            }

            item(key = "settings") {
                MySettingsCard(
                    onOpenProfile = onOpenProfile,
                    onOpenReminder = onOpenReminder,
                    onOpenBackup = onOpenBackup,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.padding(top = Spacing.lg)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 页头：头像 + 昵称 + 签名 + 阶段
// ---------------------------------------------------------------------------

/**
 * 页头。
 *
 * 整块可以点，进个人资料；右上角那颗齿轮是设置。两块各有各的点击区域，
 * 齿轮在内层，所以点齿轮不会顺带把人带进资料页。
 */
@Composable
private fun MyProfileHeader(
    uiState: MyUiState,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Sizes.gutter, end = Spacing.xs, top = Spacing.xl)
            .clickable(onClick = onOpenProfile),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppAvatar(
            name = uiState.nickname,
            path = uiState.avatarPath,
            preset = uiState.avatarPreset,
            size = Sizes.avatarLg
        )

        Spacer(modifier = Modifier.width(Spacing.lg))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = uiState.nickname.ifBlank { stringResource(R.string.my_default_name) },
                    style = AppTheme.type.h2,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(Sizes.iconMd)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxs))

            Text(
                text = uiState.signature.ifBlank { stringResource(R.string.my_default_bio) },
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            LevelPill(level = uiState.level)
        }

        AppIconButton(
            icon = Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.nav_settings),
            onClick = onOpenSettings,
            tint = colors.textSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// 四个总览数字
// ---------------------------------------------------------------------------

/**
 * 一排四个数字，中间用细线分开。
 *
 * 放在一张卡里而不是四张卡片：这里是"一眼扫过"的总览，
 * 四张独立的卡片会把注意力拆散（首页的核心数据是四张卡，因为那里是仪表盘）。
 */
@Composable
private fun MyStatsCard(
    uiState: MyUiState,
    modifier: Modifier = Modifier
) {
    val stats = listOf(
        uiState.totalCount.toString() to stringResource(R.string.my_stat_total),
        uiState.completedCount.toString() to stringResource(R.string.my_stat_completed),
        uiState.codexUnlocked.toString() to stringResource(R.string.my_stat_codex),
        uiState.streakDays.toString() to stringResource(R.string.my_stat_streak)
    )

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(vertical = Spacing.lg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stats.forEachIndexed { index, (value, label) ->
                if (index > 0) {
                    AppVerticalDivider(height = Spacing.xxl)
                }
                StatCell(
                    value = value,
                    label = label,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 一个数字 + 下面一行小字 */
@Composable
private fun StatCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = AppTheme.type.numberMedium,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = AppTheme.type.caption,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// 阶段与进度
// ---------------------------------------------------------------------------

/**
 * 人生进度：阶段 + 完成比例 + 距离下一阶段还差几条。
 *
 * 用的是和首页同一套数字（完成率、阶段、还差几条），不是另一套算法——
 * 同一件事在两个页面上给出两个数，是这个页面最不该犯的错。
 */
@Composable
private fun MyLevelCard(
    uiState: MyUiState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val percent = (uiState.completionRate * 100).roundToInt()

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_life_progress),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = stringResource(R.string.home_level, uiState.level),
                    style = AppTheme.type.display,
                    color = colors.textPrimary
                )
            }

            Text(
                text = "$percent%",
                style = AppTheme.type.numberLarge,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppProgressBar(progress = uiState.completionRate, color = colors.accent)

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.home_level_next, uiState.toNextLevel),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
    }
}

// ---------------------------------------------------------------------------
// 图鉴收藏：按稀有度分开看
// ---------------------------------------------------------------------------

/**
 * 图鉴收藏：五个稀有度档位各一个圆环。
 *
 * 这是首页「分类进度」之外的另一种切法——那一行看的是"我在哪些方向上有积累"，
 * 这一行看的是"我攒下了多少稀有的东西"。两者都不进详表，点「查看全部」去图鉴。
 */
@Composable
private fun MyCollectionCard(
    uiState: MyUiState,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.tiers.isEmpty()) return

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.my_collection_title),
            subtitle = stringResource(
                R.string.home_category_subtitle,
                uiState.codexUnlocked,
                uiState.codexTotal
            ),
            action = {
                AppTextLink(
                    text = stringResource(R.string.home_view_all),
                    onClick = onViewAll
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(modifier = Modifier.fillMaxWidth()) {
            uiState.tiers.forEach { item ->
                TierBadge(
                    item = item,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** 一个稀有度档位：圆环里是已解锁数量，下面写明档位和这个档位总共有多少 */
@Composable
private fun TierBadge(
    item: TierProgress,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppProgressRing(
            progress = item.progress,
            diameter = Sizes.tierBadge,
            strokeWidth = Sizes.progressRing,
            color = colors.rarityColor(item.tier)
        ) {
            Text(
                text = item.unlockedCount.toString(),
                style = AppTheme.type.caption,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = item.tier.label(),
            style = AppTheme.type.caption,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.my_tier_total, item.totalCount),
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// 我的人生：攒下的东西
// ---------------------------------------------------------------------------

/**
 * 我的人生：记录天数、笔记、图片视频。
 *
 * 这几个数字在首页看不到——首页讲"完成了多少"，这里讲"留下了多少"。
 * 副标题沿用首页收尾那句「从 X 开始记录 · 已记录 N 天」，两页说的是同一件事。
 */
@Composable
private fun MyLifeCard(
    uiState: MyUiState,
    modifier: Modifier = Modifier
) {
    val cells = listOf(
        uiState.recordedDays.toString() to stringResource(R.string.my_life_days),
        uiState.notesCount.toString() to stringResource(R.string.my_life_notes),
        uiState.mediaCount.toString() to stringResource(R.string.my_life_media)
    )

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass
    ) {
        SectionHeader(
            title = stringResource(R.string.my_life_title),
            subtitle = rememberRecordLine(
                firstRecordDate = uiState.firstRecordDate,
                recordedDays = uiState.recordedDays
            )
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.forEachIndexed { index, (value, label) ->
                if (index > 0) {
                    AppVerticalDivider(height = Spacing.xxl)
                }
                StatCell(
                    value = value,
                    label = label,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 快捷入口
// ---------------------------------------------------------------------------

/**
 * 四个快捷入口：成就列表 / 图鉴 / 成长 / 积分商城。
 *
 * 它们同时也是底栏的 tab，但这一页把它们摆出来是有用的：
 * 看完自己的数据之后，下一步通常是"去看看"。
 *
 * 两个一行：三个并排时每个只有 110dp 上下，加上第四个就会把标题挤成两行，
 * 四个排成 2×2 之后每个方块都够宽，说明文字也放得下。
 */
@Composable
private fun MyQuickActions(
    onOpenAllAchievements: () -> Unit,
    onOpenCodex: () -> Unit,
    onOpenGrowth: () -> Unit,
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            QuickCard(
                icon = Icons.AutoMirrored.Outlined.List,
                title = stringResource(R.string.my_quick_achievements),
                description = stringResource(R.string.my_quick_achievements_desc),
                onClick = onOpenAllAchievements,
                modifier = Modifier.weight(1f)
            )
            QuickCard(
                icon = AppIcons.Compass,
                title = stringResource(R.string.my_quick_codex),
                description = stringResource(R.string.my_quick_codex_desc),
                onClick = onOpenCodex,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            QuickCard(
                icon = Icons.Outlined.Star,
                title = stringResource(R.string.my_quick_growth),
                description = stringResource(R.string.my_quick_growth_desc),
                onClick = onOpenGrowth,
                modifier = Modifier.weight(1f)
            )
            QuickCard(
                icon = Icons.Outlined.ShoppingCart,
                title = stringResource(R.string.my_quick_store),
                description = stringResource(R.string.my_quick_store_desc),
                onClick = onOpenStore,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** 一列宽的快捷卡：小图标 + 名称（带箭头）+ 一行说明 */
@Composable
private fun QuickCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier,
        tone = AppCardTone.Glass,
        // 一行三列，每列只有 90dp 上下：内边距收一档，标题才放得下四个字
        contentPadding = PaddingValues(Spacing.md),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(Sizes.iconLg)
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = AppTheme.type.body,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(Sizes.iconSm)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = description,
            style = AppTheme.type.caption,
            color = colors.textTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------------------------------------------------------------------------
// 设置入口
// ---------------------------------------------------------------------------

/**
 * 设置入口：资料、提醒、备份、全部设置。
 *
 * 都指向已经存在的页面，这一页不新增任何设置项——
 * 「我的」只是把最常去的四个口子摆到顺手的位置。
 */
@Composable
private fun MySettingsCard(
    onOpenProfile: () -> Unit,
    onOpenReminder: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(0.dp)
    ) {
        AppSettingRow(
            title = stringResource(R.string.settings_profile),
            subtitle = stringResource(R.string.settings_profile_desc),
            leadingIcon = Icons.Outlined.Person,
            showChevron = true,
            onClick = onOpenProfile
        )
        AppDivider()
        AppSettingRow(
            title = stringResource(R.string.settings_reminder),
            subtitle = stringResource(R.string.settings_reminder_desc),
            leadingIcon = Icons.Outlined.Notifications,
            showChevron = true,
            onClick = onOpenReminder
        )
        AppDivider()
        AppSettingRow(
            title = stringResource(R.string.settings_backup),
            subtitle = stringResource(R.string.my_settings_backup_desc),
            leadingIcon = Icons.Outlined.Share,
            showChevron = true,
            onClick = onOpenBackup
        )
        AppDivider()
        AppSettingRow(
            title = stringResource(R.string.my_settings_all),
            subtitle = stringResource(R.string.my_settings_all_desc),
            leadingIcon = Icons.Outlined.Settings,
            showChevron = true,
            onClick = onOpenSettings
        )
    }
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

/** 预览用的一份数据：数字和文案都对得上，方便看排版 */
private val previewMyState = MyUiState(
    isLoaded = true,
    nickname = "林岚",
    signature = "把日子过成想要的样子",
    avatarPreset = AvatarPreset.MOUNTAIN,
    totalCount = 128,
    completedCount = 96,
    inProgressCount = 32,
    completionRate = 0.75f,
    level = 20,
    toNextLevel = 4,
    streakDays = 12,
    recordedDays = 186,
    codexUnlocked = 36,
    codexTotal = 109,
    tiers = listOf(
        TierProgress(RarityTier.Bronze, 18, 41),
        TierProgress(RarityTier.Silver, 11, 32),
        TierProgress(RarityTier.Gold, 5, 21),
        TierProgress(RarityTier.Platinum, 2, 11),
        TierProgress(RarityTier.Legendary, 0, 4)
    ),
    notesCount = 74,
    mediaCount = 426,
    firstRecordDate = 1_700_000_000_000
)

@Preview(showBackground = true, heightDp = 1400, name = "我的")
@Composable
private fun MyScreenPreview() {
    LifeLedgerTheme {
        MyScreen(
            uiState = previewMyState,
            onOpenProfile = {},
            onOpenSettings = {},
            onOpenReminder = {},
            onOpenBackup = {},
            onOpenAllAchievements = {},
            onOpenCodex = {},
            onOpenGrowth = {},
            onOpenStore = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "我的 · 新用户")
@Composable
private fun MyScreenEmptyPreview() {
    LifeLedgerTheme {
        MyScreen(
            uiState = MyUiState(isLoaded = true),
            onOpenProfile = {},
            onOpenSettings = {},
            onOpenReminder = {},
            onOpenBackup = {},
            onOpenAllAchievements = {},
            onOpenCodex = {},
            onOpenGrowth = {},
            onOpenStore = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1400, name = "我的 · 深色")
@Composable
private fun MyScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        MyScreen(
            uiState = previewMyState,
            onOpenProfile = {},
            onOpenSettings = {},
            onOpenReminder = {},
            onOpenBackup = {},
            onOpenAllAchievements = {},
            onOpenCodex = {},
            onOpenGrowth = {},
            onOpenStore = {}
        )
    }
}
