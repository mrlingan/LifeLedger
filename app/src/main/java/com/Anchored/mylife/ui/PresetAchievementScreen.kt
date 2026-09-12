package com.Anchored.mylife.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.ui.codex.CodexEntryCard
import com.Anchored.mylife.ui.codex.CodexProgress
import com.Anchored.mylife.ui.codex.formatRate
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppEmblem
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.CompletionConfirmDialog
import com.Anchored.mylife.ui.components.AppTopBarStyle
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.RarityBadge
import com.Anchored.mylife.ui.components.appearAnimation
import com.Anchored.mylife.ui.components.label
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 图鉴：人生经历档案馆。
 *
 * 结构：大标题 → 总进度 → 搜索与筛选 → 网格。
 * 网格里一条记录 = 徽记 + 名称 + 描述 + 细分隔线 + 稀有度与时间，
 * 已解锁的排前面、对比度更高，未解锁的保留轮廓但整体降一档。
 *
 * 达成状态的口径没变：存在 presetId 相同且已完成的成就才算解锁，
 * 所以「标记为已达成」在首页、图鉴、列表三处永远是同一个开关。
 */
@Composable
fun PresetCodexRoute(
    navController: NavHostController,
    pickMode: Boolean = false,
    viewModel: PresetAchievementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    // 图鉴文案跟着系统语言走：数据库里存的中文只当标识用
    val presetTexts = rememberPresetTexts()

    // 浏览模式（底部导航的 tab）不需要返回箭头；挑选模式是二级页面，需要
    val onBack: (() -> Unit)? = if (pickMode) {
        { navController.popBackStack() }
    } else {
        null
    }

    PresetCodexScreen(
        uiState = uiState,
        presetTexts = presetTexts,
        pickMode = pickMode,
        onBack = onBack,
        onQueryChange = viewModel::setQuery,
        onCategoryChange = viewModel::setCategory,
        onStatusFilterChange = viewModel::setStatusFilter,
        onItemClick = { preset ->
            if (pickMode) {
                // 挑选模式：带着这条成就回新建页，标题描述图标都填好，还能改
                // 回退到「把图鉴拉起来的那一页」——可能点进来的是首页，也可能是全部成就页
                val backTarget = navController.previousBackStackEntry?.destination?.route
                    ?: ROUTE_HOME
                navController.navigate("add_achievement?presetId=${preset.id}") {
                    popUpTo(backTarget) { inclusive = false }
                }
            } else {
                selectedId = preset.id
            }
        }
    )

    val selected = selectedId?.let { id -> uiState.allItems.firstOrNull { it.id == id } }
    if (selected != null && !pickMode) {
        val text = presetTexts.textOf(selected)
        PresetDetailSheet(
            preset = selected,
            text = text,
            tier = uiState.tierOf(selected),
            isCompleted = uiState.isCompleted(selected),
            isInList = uiState.achievementOf(selected) != null,
            confirmCompletion = uiState.confirmCompletion,
            onDismiss = { selectedId = null },
            onToggleCompleted = { viewModel.toggleCompleted(selected, text) },
            onAddToMine = {
                selectedId = null
                viewModel.addToMyAchievements(selected, text)
            }
        )
    }
}

@Composable
fun PresetCodexScreen(
    uiState: PresetCodexUiState,
    presetTexts: PresetTextResolver = rememberPresetTexts(),
    pickMode: Boolean = false,
    onBack: (() -> Unit)?,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onStatusFilterChange: (CodexStatusFilter) -> Unit,
    onItemClick: (PresetAchievement) -> Unit
) {
    val colors = AppTheme.colors
    val textOf: (PresetAchievement) -> PresetText = { presetTexts.textOf(it) }
    val entries = uiState.gridItems(textOf)

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Sizes.codexCell),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = Sizes.gutter,
                end = Sizes.gutter,
                bottom = Spacing.xxxl
            ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                AppTopBar(
                    title = stringResource(R.string.codex_title),
                    subtitle = if (pickMode) {
                        stringResource(R.string.codex_subtitle_pick)
                    } else {
                        stringResource(R.string.codex_subtitle)
                    },
                    style = AppTopBarStyle.Large,
                    onBack = onBack
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "progress") {
                CodexProgress(
                    unlockedCount = uiState.unlockedCount,
                    totalCount = uiState.totalCount,
                    progress = uiState.progress
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }, key = "controls") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    AppTextField(
                        value = uiState.query,
                        onValueChange = onQueryChange,
                        placeholder = stringResource(R.string.codex_search_hint),
                        leadingIcon = Icons.Outlined.Search
                    )

                    AppSegmentedControl(
                        options = CodexStatusFilter.entries.map { stringResource(it.labelRes) },
                        selectedIndex = CodexStatusFilter.entries.indexOf(uiState.statusFilter),
                        onSelect = { index ->
                            onStatusFilterChange(CodexStatusFilter.entries[index])
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        contentPadding = PaddingValues(end = Sizes.gutter)
                    ) {
                        item(key = "category_all") {
                            AppChip(
                                label = stringResource(R.string.codex_count_all, uiState.totalCount),
                                selected = uiState.selectedCategory == null,
                                onClick = { onCategoryChange(null) }
                            )
                        }
                        items(items = uiState.categories, key = { it }) { category ->
                            AppChip(
                                label = "${presetTexts.categoryOf(category)} ${uiState.countOf(category)}",
                                selected = uiState.selectedCategory == category,
                                onClick = { onCategoryChange(category) }
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.codex_not_found),
                        description = stringResource(R.string.codex_not_found_desc)
                    )
                }
            } else {
                gridItems(
                    items = entries,
                    key = { preset -> "preset_${preset.id}" }
                ) { preset ->
                    val text = presetTexts.textOf(preset)
                    CodexEntryCard(
                        title = text.title,
                        description = text.description,
                        tier = uiState.tierOf(preset),
                        rate = preset.rate,
                        unlocked = uiState.isCompleted(preset),
                        onClick = { onItemClick(preset) },
                        modifier = Modifier.appearAnimation()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDetailSheet(
    preset: PresetAchievement,
    text: PresetText,
    tier: RarityTier,
    isCompleted: Boolean,
    isInList: Boolean,
    confirmCompletion: Boolean,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
    onAddToMine: () -> Unit
) {
    val colors = AppTheme.colors
    val presetTexts = rememberPresetTexts()
    val completionFeedback = rememberCompletionFeedback()
    var confirmPending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = Radius.hero, topEnd = Radius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = Sizes.gutter, end = Sizes.gutter, bottom = Spacing.xxl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppEmblem(
                    text = text.title,
                    tier = tier,
                    unlocked = isCompleted
                )

                Spacer(modifier = Modifier.size(Spacing.lg))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text.title,
                        style = AppTheme.type.h2,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RarityBadge(tier = tier)
                        Spacer(modifier = Modifier.size(Spacing.sm))
                        Text(
                            text = stringResource(
                                R.string.codex_rate_with_category,
                                presetTexts.categoryOf(preset.category),
                                formatRate(preset.rate)
                            ),
                            style = AppTheme.type.caption,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = text.description,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = text.story,
                style = AppTheme.type.body,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            AppButton(
                text = if (isCompleted) {
                    stringResource(R.string.codex_unlocked_undo)
                } else {
                    stringResource(R.string.codex_mark_unlocked)
                },
                onClick = {
                    if (!isCompleted && confirmCompletion) {
                        confirmPending = true
                    } else {
                        if (!isCompleted) completionFeedback()
                        onToggleCompleted()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (!isInList) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                AppButton(
                    text = stringResource(R.string.codex_add_first),
                    onClick = onAddToMine,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmPending) {
        CompletionConfirmDialog(
            onConfirm = {
                confirmPending = false
                completionFeedback()
                onToggleCompleted()
            },
            onDismiss = { confirmPending = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewPresets = listOf(
    PresetAchievement(
        id = 1,
        title = "跑完一场马拉松",
        description = "42.195 公里，用双脚丈量",
        story = "第 35 公里几乎放弃，最后拐过街角看到了终点。",
        category = "兴趣",
        rarity = "epic",
        rate = 8.0,
        iconEmoji = ""
    ),
    PresetAchievement(
        id = 2,
        title = "学会游泳",
        description = "从不敢下水到能连续游 1000 米",
        story = "第一次把头埋进水里的时候，你以为自己做不到。",
        category = "技能",
        rarity = "common",
        rate = 80.0,
        iconEmoji = ""
    ),
    PresetAchievement(
        id = 3,
        title = "海外旅行",
        description = "去一个语言不通的地方生活几天",
        story = "语言不通反而让观察变得仔细。",
        category = "旅行",
        rarity = "rare",
        rate = 11.0,
        iconEmoji = ""
    ),
    PresetAchievement(
        id = 4,
        title = "全款置业",
        description = "不靠贷款买下自己的房子",
        story = "你把很多个想买的东西换成了这一个。",
        category = "生活",
        rarity = "legendary",
        rate = 4.0,
        iconEmoji = ""
    )
)

private val previewCodexState = PresetCodexUiState(
    allItems = previewPresets,
    tierByPresetId = previewPresets.associate { it.id to RarityTier.fromRate(it.rate) },
    categories = listOf("兴趣", "技能", "旅行", "生活"),
    isLoaded = true
)

@Preview(showBackground = true, heightDp = 1500, name = "图鉴")
@Composable
private fun PresetCodexPreview() {
    LifeLedgerTheme {
        PresetCodexScreen(
            uiState = previewCodexState,
            pickMode = false,
            onBack = null,
            onQueryChange = {},
            onCategoryChange = {},
            onStatusFilterChange = {},
            onItemClick = {}
        )
    }
}
