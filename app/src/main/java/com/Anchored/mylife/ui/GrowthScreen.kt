package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.DailyEvent
import com.Anchored.mylife.data.database.Goal
import com.Anchored.mylife.data.database.GoalTask
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.components.PageBackdrop
import com.Anchored.mylife.ui.growth.GrowthAttributesCard
import com.Anchored.mylife.ui.growth.GrowthCurveCard
import com.Anchored.mylife.ui.growth.GrowthGoalsSection
import com.Anchored.mylife.ui.growth.GrowthHeader
import com.Anchored.mylife.ui.growth.GrowthHeroCard
import com.Anchored.mylife.ui.growth.GrowthRecordsCard
import com.Anchored.mylife.ui.growth.GrowthSayingCard
import com.Anchored.mylife.ui.growth.GrowthStoreCard
import com.Anchored.mylife.ui.reward.rememberRewardLanguage
import com.Anchored.mylife.ui.reward.rewardTitleIn
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 底栏的「成长」。
 *
 * 这一页回答"我攒下了什么"：走到哪一阶段、账上有多少积分、哪些方向长出了属性、
 * 最近攒下的每一笔，以及接着要做什么。从上到下是
 * 阶段与积分（Hero）→ 人生曲线 → 人生属性 → 成长记录 → 长期目标 → 每日一言 → 积分商城。
 *
 * 和首页的分工：首页是今天的仪表盘（完成了多少、还剩多少），这一页是账本
 * （怎么攒起来的、攒到哪儿了、可以拿去换什么）。阶段和积分走的是全 App 同一套口径
 * （`LifeStats.kt` 的阶段算法 + 一本流水账），所以它和首页、我的、商城永远对得上。
 */
@Composable
fun GrowthRoute(
    navController: NavHostController,
    viewModel: GrowthViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val repositories = RepositoryProvider.get(LocalContext.current)
    val networkEnabled by repositories.settings.networkEnabled.collectAsStateWithLifecycle()
    // 属性的颜色沿用首页那套偏好：同一个分类在两个页面上是同一个颜色
    val categoryColors by repositories.settings.categoryColors.collectAsStateWithLifecycle()

    GrowthScreen(
        uiState = state,
        categoryColors = categoryColors,
        showSaying = networkEnabled,
        onRefreshSaying = viewModel::refreshSaying,
        onCreateGoal = viewModel::createGoal,
        onAddTask = viewModel::addTask,
        onCompleteTask = viewModel::completeTask,
        onOpenStore = { navController.navigate(ROUTE_REWARD_STORE) }
    )
}

@Composable
fun GrowthScreen(
    uiState: GrowthUiState,
    /** 分类圆环的颜色偏好（分类名 → `%08X`），和首页共用一份 */
    categoryColors: Map<String, String> = emptyMap(),
    showSaying: Boolean = true,
    onRefreshSaying: () -> Unit = {},
    onCreateGoal: (String, String, Int) -> Unit = { _, _, _ -> },
    onAddTask: (Long, String, Int) -> Unit = { _, _, _ -> },
    onCompleteTask: (Long) -> Unit = {},
    onOpenStore: () -> Unit = {}
) {
    val presetTexts = rememberPresetTexts()
    // 账本里那几笔商城兑换的标题：内置奖励也按当前语言显示
    val rewardLanguage = rememberRewardLanguage()

    var expandedAttributes by rememberSaveable { mutableStateOf(false) }
    var expandedRecords by rememberSaveable { mutableStateOf(false) }
    var creatingGoal by remember { mutableStateOf(false) }
    var addingTaskTo by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        containerColor = AppTheme.pageColor,
        // 顶部内边距交给页头自己处理（它要跟着内容一起滚），这里只留左右与底部：
        // 底部是浮在内容上的那条底栏
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // 页面上缘那层很淡的蓝光：首页同款——两页都是"看数据"的地方
            PageBackdrop()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    bottom = Spacing.xxxl + LocalBottomBarClearance.current
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item(key = "header") {
                    GrowthHeader(
                        title = stringResource(R.string.nav_growth),
                        subtitle = stringResource(R.string.growth_subtitle)
                    )
                }

                item(key = "hero") {
                    GrowthHeroCard(
                        balance = uiState.balance,
                        level = uiState.level,
                        stageProgress = uiState.stageProgress,
                        toNextLevel = uiState.toNextLevel
                    )
                }

                item(key = "curve") {
                    GrowthCurveCard(transactions = uiState.transactions)
                }

                item(key = "attributes") {
                    GrowthAttributesCard(
                        attributes = uiState.attributes,
                        labelOf = presetTexts::categoryOf,
                        colorOf = { category -> storedColorToColor(categoryColors[category]) },
                        expanded = expandedAttributes,
                        onExpandedChange = { expandedAttributes = it }
                    )
                }

                item(key = "records") {
                    GrowthRecordsCard(
                        records = uiState.transactions,
                        presetTitleOf = { record ->
                            // 记账存下的是当时的原文；能从图鉴（成就）或内置奖励目录
                            // 现取到当前语言的标题就用它，取不到就显示原文
                            record.presetId?.let { presetTexts.titleOf(it, record.title) }
                                ?: rewardTitleIn(record.title, rewardLanguage)
                        },
                        expanded = expandedRecords,
                        onExpandedChange = { expandedRecords = it }
                    )
                }

                item(key = "goals") {
                    GrowthGoalsSection(
                        goals = uiState.goals,
                        tasks = uiState.tasks,
                        onCreateGoal = { creatingGoal = true },
                        onAddTask = { goalId -> addingTaskTo = goalId },
                        onCompleteTask = onCompleteTask
                    )
                }

                if (showSaying) {
                    item(key = "saying") {
                        GrowthSayingCard(event = uiState.event, onRefresh = onRefreshSaying)
                    }
                }

                item(key = "store") {
                    GrowthStoreCard(onClick = onOpenStore)
                }
            }
        }
    }

    if (creatingGoal) {
        GoalDialog(
            onDismiss = { creatingGoal = false },
            onSave = { title, description, days ->
                onCreateGoal(title, description, days)
                creatingGoal = false
            }
        )
    }

    addingTaskTo?.let { goalId ->
        TaskDialog(
            onDismiss = { addingTaskTo = null },
            onSave = { title, reward ->
                onAddTask(goalId, title, reward)
                addingTaskTo = null
            }
        )
    }
}

/** 新建长期目标：名称 / 说明 / 总天数 */
@Composable
private fun GoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var days by remember { mutableStateOf(DEFAULT_GOAL_DAYS.toString()) }

    AppDialog(
        title = stringResource(R.string.growth_goal_dialog_title),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (title.isNotBlank()) {
                onSave(title, description, days.toIntOrNull() ?: DEFAULT_GOAL_DAYS)
            }
        },
        confirmText = stringResource(R.string.growth_dialog_create),
        dismissText = stringResource(R.string.common_cancel)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.growth_goal_field_title)
            )
            AppTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.growth_goal_field_desc)
            )
            AppTextField(
                value = days,
                onValueChange = { days = it },
                label = stringResource(R.string.growth_goal_field_days)
            )
        }
    }
}

/** 往一个目标下面加每日任务：任务名 / 做完给多少积分 */
@Composable
private fun TaskDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf(DEFAULT_TASK_REWARD.toString()) }

    AppDialog(
        title = stringResource(R.string.growth_task_dialog_title),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (title.isNotBlank()) {
                onSave(title, reward.toIntOrNull() ?: DEFAULT_TASK_REWARD)
            }
        },
        confirmText = stringResource(R.string.growth_dialog_add),
        dismissText = stringResource(R.string.common_cancel)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AppTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.growth_task_field_title)
            )
            AppTextField(
                value = reward,
                onValueChange = { reward = it },
                label = stringResource(R.string.growth_task_field_reward)
            )
        }
    }
}

/** 新目标默认 100 天，新任务默认 10 XP——都是"先有个数，再自己改" */
private const val DEFAULT_GOAL_DAYS = 100
private const val DEFAULT_TASK_REWARD = 10

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewTransactions = listOf(
    GrowthRecord(
        id = 1,
        type = "achievement",
        amount = 50,
        createdAt = 1_757_760_000_000,
        title = "完成人生第一场马拉松",
        presetId = 12L
    ),
    GrowthRecord(
        id = 2,
        type = "daily_task",
        amount = 30,
        createdAt = 1_757_673_600_000,
        title = "跑步 30 分钟",
        presetId = null
    ),
    GrowthRecord(
        id = 3,
        type = "reward_redeem",
        amount = -100,
        createdAt = 1_757_587_200_000,
        title = "一杯喜欢的咖啡",
        presetId = null
    ),
    GrowthRecord(
        id = 4,
        type = "random_event",
        amount = 50,
        createdAt = 1_757_500_800_000,
        title = "Task sprint",
        presetId = null
    ),
    GrowthRecord(
        id = 5,
        type = "achievement_reversal",
        amount = -50,
        createdAt = 1_757_414_400_000,
        title = "学会游泳",
        presetId = null
    )
)

private val previewGrowthState = GrowthUiState(
    balance = 860,
    transactions = previewTransactions,
    goals = listOf(
        Goal(
            id = 1,
            title = "跑完第一个半程马拉松",
            description = "每周三次，慢慢加到 21 公里",
            startDate = 1_756_000_000_000,
            dueDate = System.currentTimeMillis() + 42L * 86_400_000L
        )
    ),
    tasks = listOf(
        GoalTask(
            goalId = 1,
            title = "慢跑 30 分钟",
            scheduledDate = 1_756_000_000_000,
            reward = 10
        ),
        GoalTask(
            goalId = 1,
            title = "拉伸 10 分钟",
            scheduledDate = 1_756_000_000_000,
            reward = 5,
            isCompleted = true,
            completedAt = 1_756_100_000_000
        )
    ),
    event = DailyEvent(
        dateKey = "2026-09-14",
        title = "你走过的路，都会成为独特的勋章。",
        description = "",
        type = "special",
        rarity = "common",
        effect = "saying_remote",
        expiresAt = 1_757_760_000_000
    ),
    level = 7,
    toNextLevel = 3,
    completedCount = 32,
    attributes = listOf(
        GrowthAttribute("技能", 7, 350, 2, 0.75f, 150),
        GrowthAttribute("成长", 6, 300, 2, 0.5f, 100),
        GrowthAttribute("健康", 5, 250, 2, 0.25f, 50),
        GrowthAttribute("旅行", 4, 200, 2, 0f, 0),
        GrowthAttribute("兴趣", 3, 150, 1, 0.75f, 150),
        GrowthAttribute("学业", 2, 100, 1, 0.5f, 100),
        GrowthAttribute("社交", 1, 50, 1, 0.25f, 50)
    )
)

@Preview(showBackground = true, heightDp = 2400, name = "成长")
@Composable
private fun GrowthScreenPreview() {
    LifeLedgerTheme {
        GrowthScreen(uiState = previewGrowthState)
    }
}

@Preview(showBackground = true, heightDp = 1400, name = "成长 · 空状态")
@Composable
private fun GrowthScreenEmptyPreview() {
    LifeLedgerTheme {
        GrowthScreen(uiState = GrowthUiState(), showSaying = false)
    }
}

@Preview(showBackground = true, heightDp = 2400, name = "成长 · 深色")
@Composable
private fun GrowthScreenDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        GrowthScreen(uiState = previewGrowthState)
    }
}
