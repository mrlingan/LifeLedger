package com.Anchored.mylife.ui.reward

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.database.RewardRedemption
import com.Anchored.mylife.data.reward.RewardCatalog
import com.Anchored.mylife.ui.LEVEL_STEP
import com.Anchored.mylife.ui.xpText
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppSnackbarHost
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.LevelPill
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 积分商城。
 *
 * 它是二级页面（从「成长」或「我的」进来，带返回箭头），所以没有底栏：
 * 商城是"想好了来换一件"的地方，不是每天都要路过的第五个 tab。
 *
 * 页面从上到下：钱包（积分 + 阶段）→ 分类 → 奖励两列 → 自定义奖励。
 * 兑换不弹二次确认：换错了在这一页的「我的奖励」里能撤销，积分原样退回——
 * 与其拦一道"确定吗"，不如让决定是可逆的。
 */
@Composable
fun RewardStoreRoute(
    navController: NavHostController,
    viewModel: RewardStoreViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // 内置奖励的文案是落库的，语言变了得对一遍——进这一页、以及在这一页切语言，
    // 都会走到这里（key 是当前语言，变了就重跑）
    val language = rememberRewardLanguage()
    LaunchedEffect(language) { viewModel.syncCatalogLanguage(language) }

    LaunchedEffect(message) {
        val current = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(current)
        viewModel.consumeMessage()
    }

    RewardStoreScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = { navController.popBackStack() },
        onRedeem = viewModel::redeem,
        onUndo = viewModel::undoRedemption,
        onSaveCustom = viewModel::saveCustomReward,
        onDeleteReward = viewModel::deleteReward
    )
}

@Composable
fun RewardStoreScreen(
    uiState: RewardStoreUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit = {},
    onRedeem: (RewardItem) -> Unit = {},
    onUndo: (Long) -> Unit = {},
    onSaveCustom: (Long?, String, String, String, String, Int) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteReward: (Long) -> Unit = {}
) {
    // null = 「全部」；分类被删空时下面会把它收回「全部」，不会卡在一个空标签页上
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showMyRewards by remember { mutableStateOf(false) }
    var creatingReward by remember { mutableStateOf(false) }
    var editingReward by remember { mutableStateOf<RewardItem?>(null) }
    var deletingReward by remember { mutableStateOf<RewardItem?>(null) }
    var undoingRecord by remember { mutableStateOf<RewardRedemption?>(null) }

    val visible = remember(uiState.items, selectedCategory) {
        val category = selectedCategory
        if (category == null) uiState.items else uiState.items.filter { it.category == category }
    }

    LaunchedEffect(uiState.categories) {
        if (selectedCategory != null && selectedCategory !in uiState.categories) {
            selectedCategory = null
        }
    }

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.store_title),
                subtitle = stringResource(R.string.store_subtitle),
                onBack = onBack,
                actions = {
                    AppTextLink(
                        text = stringResource(R.string.store_my_rewards),
                        onClick = { showMyRewards = true }
                    )
                }
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Sizes.gutter,
                end = Sizes.gutter,
                top = Spacing.lg,
                bottom = Spacing.xxxl + LocalBottomBarClearance.current
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item(key = "balance") {
                StoreBalanceCard(uiState)
            }

            if (uiState.categories.size > 1) {
                item(key = "categories") {
                    StoreCategoryTabs(
                        categories = uiState.categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                }
            }

            if (visible.isEmpty()) {
                // 还没读出来之前什么都不说：内置奖励是首次进商城时写进去的，
                // 这几帧里弹一句「还没有奖励」会让人以为目录空了
                if (uiState.isLoaded) {
                    item(key = "empty") {
                        EmptyState(
                            title = if (uiState.items.isEmpty()) {
                                stringResource(R.string.store_empty_title)
                            } else {
                                stringResource(R.string.store_filter_empty)
                            },
                            description = stringResource(R.string.store_empty_desc)
                        )
                    }
                }
            } else {
                items(
                    count = (visible.size + 1) / 2,
                    key = { row -> "row-${visible[row * 2].id}" }
                ) { row ->
                    val left = visible[row * 2]
                    val right = visible.getOrNull(row * 2 + 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        RewardCard(
                            item = left,
                            onRedeem = { onRedeem(left) },
                            onEdit = { editingReward = left },
                            modifier = Modifier.weight(1f)
                        )
                        if (right != null) {
                            RewardCard(
                                item = right,
                                onRedeem = { onRedeem(right) },
                                onEdit = { editingReward = right },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "custom") {
                CustomRewardTile(onClick = { creatingReward = true })
            }
        }
    }

    if (showMyRewards) {
        MyRewardsSheet(
            redemptions = uiState.activeRedemptions,
            spentTotal = uiState.spentTotal,
            onDismiss = { showMyRewards = false },
            onUndo = { record -> undoingRecord = record }
        )
    }

    if (creatingReward || editingReward != null) {
        val editing = editingReward
        RewardEditorDialog(
            initial = editing,
            balance = uiState.balance,
            categories = uiState.categories.ifEmpty { RewardCatalog.CATEGORY_ORDER },
            onDismiss = {
                creatingReward = false
                editingReward = null
            },
            onSave = { title, description, icon, category, price ->
                onSaveCustom(editing?.id, title, description, icon, category, price)
                creatingReward = false
                editingReward = null
            },
            onDelete = editing?.let { item ->
                {
                    // 先收起编辑弹窗，再用一个确认弹窗问一次：两个弹窗不叠在一起
                    editingReward = null
                    deletingReward = item
                }
            }
        )
    }

    deletingReward?.let { item ->
        AppDialog(
            title = stringResource(R.string.store_delete_confirm_title),
            onDismissRequest = { deletingReward = null },
            onConfirm = {
                onDeleteReward(item.id)
                deletingReward = null
            },
            confirmText = stringResource(R.string.common_delete),
            dismissText = stringResource(R.string.common_cancel),
            destructive = true,
            content = {
                AppDialogText(stringResource(R.string.store_delete_confirm_body, item.title))
            }
        )
    }

    undoingRecord?.let { record ->
        AppDialog(
            title = stringResource(R.string.store_undo_confirm_title),
            onDismissRequest = { undoingRecord = null },
            onConfirm = {
                onUndo(record.id)
                undoingRecord = null
            },
            confirmText = stringResource(R.string.store_undo),
            dismissText = stringResource(R.string.common_cancel),
            content = {
                AppDialogText(
                    stringResource(R.string.store_undo_confirm_body, xpText(record.price))
                )
            }
        )
    }
}

// ---------------------------------------------------------------------------
// 钱包：积分 + 阶段
// ---------------------------------------------------------------------------

/**
 * 钱包卡片。
 *
 * 上面一行是"我有多少"（积分余额）和"我走到哪了"（阶段）。
 * 阶段用的是全 App 同一套算法（每 5 条已完成成就一阶段），
 * 所以这张卡上的 Lv. 和首页、我的页永远是同一个数。
 */
@Composable
private fun StoreBalanceCard(uiState: RewardStoreUiState) {
    val colors = AppTheme.colors
    // 阶段进度 = 当前阶段已经走完的比例（和首页那条进度条同一算法）
    val levelProgress = (LEVEL_STEP - uiState.toNextLevel).toFloat() / LEVEL_STEP

    AppCard(tone = AppCardTone.Glass) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.store_balance_label),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = xpText(uiState.balance),
                        style = AppTheme.type.numberLarge,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.store_xp_unit),
                        style = AppTheme.type.bodyLarge,
                        color = colors.accent
                    )
                }
            }

            LevelPill(level = uiState.level)
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = stringResource(
                R.string.store_spent_summary,
                uiState.redeemedCount,
                xpText(uiState.spentTotal)
            ),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppProgressBar(progress = levelProgress)

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.home_level_next, uiState.toNextLevel),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )
    }
}

// ---------------------------------------------------------------------------
// 分类
// ---------------------------------------------------------------------------

/** 分类标签：横向一条，滚得动。选中的那个是任务色玻璃，和别处的筛选器一致 */
@Composable
private fun StoreCategoryTabs(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AppChip(
            label = stringResource(R.string.codex_filter_all),
            selected = selected == null,
            onClick = { onSelect(null) }
        )
        categories.forEach { category ->
            AppChip(
                label = rewardCategoryLabel(category),
                selected = category == selected,
                onClick = { onSelect(category) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 奖励卡片
// ---------------------------------------------------------------------------

/**
 * 一件奖励。
 *
 * 标题与描述都固定占两行（`minLines = 2`）：两列并排时卡片高度必须一致，
 * 否则一行卡片高一截、一行矮一截。用行数而不是固定 dp 高度，是因为
 * 字号缩放（设置里可以调大）会让一行变高，固定 dp 会把第二行裁掉。
 *
 * 自定义奖励整张卡可点：点进去改名字、改价格、或者删掉它。
 */
@Composable
private fun RewardCard(
    item: RewardItem,
    onRedeem: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier,
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(Spacing.md),
        onClick = if (item.isCustom) onEdit else null
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(Sizes.rewardCover)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Radius.lg))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, style = AppTheme.type.display)
            }

            CategoryTag(
                category = item.category,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = item.title,
            style = AppTheme.type.body.copy(fontWeight = FontWeight.SemiBold),
            color = colors.textPrimary,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = item.description,
            style = AppTheme.type.caption,
            color = colors.textSecondary,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = xpText(item.price),
                style = AppTheme.type.numberMedium,
                color = colors.accent
            )
            Spacer(modifier = Modifier.width(Spacing.xxs))
            Text(
                text = stringResource(R.string.store_xp_unit),
                style = AppTheme.type.caption,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        AppButton(
            text = stringResource(R.string.store_redeem),
            onClick = onRedeem,
            variant = AppButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** 压在图标区右上角的分类标签 */
@Composable
private fun CategoryTag(category: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(colors.accentSoft)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
    ) {
        Text(
            text = rewardCategoryLabel(category),
            style = AppTheme.type.caption,
            color = colors.accentStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 自定义奖励入口。
 *
 * 内置那六条是"照这个样子来"，真正贴自己生活的奖励得自己写：这张卡就是那个口子。
 * 点一下开编辑弹窗（名称 / 图标 / 分类 / 描述 / 价格）。
 */
@Composable
private fun CustomRewardTile(onClick: () -> Unit) {
    val colors = AppTheme.colors

    AppCard(tone = AppCardTone.Glass, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = RewardCatalog.FALLBACK_ICON, style = AppTheme.type.h1)

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.store_custom_title),
                    style = AppTheme.type.bodyLarge,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = stringResource(R.string.store_custom_desc),
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(Sizes.iconMd)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewState = RewardStoreUiState(
    isLoaded = true,
    balance = 2680,
    items = RewardCatalog.defaultItems(RewardCatalog.LANGUAGE_ZH),
    redemptions = listOf(
        RewardRedemption(
            id = 1,
            rewardId = 1,
            title = "买一杯喜欢的咖啡",
            icon = "☕",
            category = RewardCatalog.CATEGORY_LIFE,
            price = 100
        )
    ),
    level = 18,
    toNextLevel = 3,
    categories = listOf(
        RewardCatalog.CATEGORY_LIFE,
        RewardCatalog.CATEGORY_FUN,
        RewardCatalog.CATEGORY_TRAVEL,
        RewardCatalog.CATEGORY_STUDY,
        RewardCatalog.CATEGORY_DIGITAL
    )
)

@Preview(showBackground = true, heightDp = 1400, name = "积分商城")
@Composable
private fun RewardStorePreview() {
    LifeLedgerTheme {
        RewardStoreScreen(uiState = previewState)
    }
}

@Preview(showBackground = true, heightDp = 1400, name = "积分商城 · 深色")
@Composable
private fun RewardStoreDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        RewardStoreScreen(uiState = previewState)
    }
}
