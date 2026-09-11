package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.ui.components.AchievementCard
import com.Anchored.mylife.ui.components.AchievementStatus
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.RarityBadge
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.label
import com.Anchored.mylife.ui.components.appearAnimation
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.RarityTier
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 成就图鉴：收藏与探索。
 *
 * 和首页的区别在于——首页是「我做过什么」，图鉴是「世界上还有什么可以做」。
 * 所以这里按稀有度分区、未解锁保持轮廓、已达成有视觉重量。
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

    PresetCodexScreen(
        uiState = uiState,
        presetTexts = presetTexts,
        pickMode = pickMode,
        onBack = { navController.popBackStack() },
        onQueryChange = viewModel::setQuery,
        onCategoryChange = viewModel::setCategory,
        onItemClick = { preset ->
            if (pickMode) {
                // 挑选模式：带着这条成就回新建页，标题描述图标都填好，还能改
                navController.navigate("add_achievement?presetId=${preset.id}") {
                    popUpTo("achievement_list") { inclusive = false }
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
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onItemClick: (PresetAchievement) -> Unit
) {
    val colors = AppTheme.colors
    val textOf: (PresetAchievement) -> PresetText = { presetTexts.textOf(it) }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.codex_title),
                subtitle = if (pickMode) stringResource(R.string.codex_subtitle_pick) else stringResource(R.string.codex_subtitle),
                onBack = onBack,
                actions = {
                    Text(
                        text = "${uiState.unlockedCount} / ${uiState.totalCount}",
                        style = AppTheme.type.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(end = Spacing.lg)
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = Spacing.xxxl)
        ) {
            item(key = "progress") {
                CodexProgress(uiState = uiState)
            }

            item(key = "search") {
                AppTextField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.codex_search_hint),
                    leadingIcon = Icons.Outlined.Search,
                    modifier = Modifier.padding(horizontal = Sizes.gutter)
                )
            }

            item(key = "categories") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Sizes.gutter),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.md)
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

            if (uiState.visibleItems(textOf).isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.codex_not_found),
                        description = stringResource(R.string.codex_not_found_desc)
                    )
                }
            } else {
                uiState.sections(textOf).forEach { (tier, sectionItems) ->
                    item(key = "section_${tier.name}") {
                        SectionHeader(
                            title = "${tier.label()} · ${sectionItems.size}",
                            modifier = Modifier.padding(
                                start = Sizes.gutter,
                                end = Sizes.gutter,
                                top = Spacing.xl,
                                bottom = Spacing.xs
                            )
                        )
                    }

                    itemsIndexed(
                        items = sectionItems,
                        key = { _, item -> "preset_${item.id}" }
                    ) { index, preset ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = Sizes.gutter)
                                .appearAnimation(index)
                        ) {
                            PresetRow(
                                preset = preset,
                                text = presetTexts.textOf(preset),
                                tier = uiState.tierOf(preset),
                                isCompleted = uiState.isCompleted(preset),
                                isInList = uiState.achievementOf(preset) != null,
                                onClick = { onItemClick(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodexProgress(uiState: PresetCodexUiState) {
    Column(
        modifier = Modifier.padding(
            start = Sizes.gutter,
            end = Sizes.gutter,
            top = Spacing.lg,
            bottom = Spacing.lg
        )
    ) {
        AppProgressBar(progress = uiState.progress)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(
                R.string.codex_progress_summary,
                uiState.unlockedCount,
                uiState.totalCount
            ),
            style = AppTheme.type.caption,
            color = AppTheme.colors.textTertiary
        )
    }
}

@Composable
private fun PresetRow(
    preset: PresetAchievement,
    text: PresetText,
    tier: RarityTier,
    isCompleted: Boolean,
    isInList: Boolean,
    onClick: () -> Unit
) {
    AchievementCard(
        title = text.title,
        description = text.description,
        icon = {
            Text(
                // 图标体系落地前，先用标题首字占位
                text = preset.iconEmoji.ifBlank { text.title.take(1) },
                style = AppTheme.type.numberMedium
            )
        },
        status = when {
            isCompleted -> AchievementStatus.Completed
            isInList -> AchievementStatus.InProgress
            else -> AchievementStatus.Locked
        },
        rarity = tier,
        meta = stringResource(R.string.codex_rate, formatRate(preset.rate)),
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDetailSheet(
    preset: PresetAchievement,
    text: PresetText,
    tier: RarityTier,
    isCompleted: Boolean,
    isInList: Boolean,
    onDismiss: () -> Unit,
    onToggleCompleted: () -> Unit,
    onAddToMine: () -> Unit
) {
    val colors = AppTheme.colors
    val presetTexts = rememberPresetTexts()
    val iconShape = RoundedCornerShape(Radius.lg)

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
                Box(
                    modifier = Modifier
                        .size(Sizes.avatarLg)
                        .clip(iconShape)
                        .background(colors.surfaceSunken),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.iconEmoji.ifBlank { text.title.take(1) },
                        style = AppTheme.type.h2
                    )
                }

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
                text = if (isCompleted) stringResource(R.string.codex_unlocked_undo) else stringResource(R.string.codex_mark_unlocked),
                onClick = onToggleCompleted,
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
}

private fun formatRate(rate: Double): String =
    if (rate % 1.0 == 0.0) "${rate.toInt()}%" else "$rate%"

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewPresets = listOf(
    PresetAchievement(
        id = 1,
        title = "全款置业",
        description = "无贷款拥有一个属于自己的家",
        story = "你一次性付清了房款，销售看着你的转账记录愣了三秒。",
        category = "生活",
        rarity = "legendary",
        rate = 4.0,
        iconEmoji = "🏠",
        isUnlocked = true
    ),
    PresetAchievement(
        id = 2,
        title = "财富自由",
        description = "被动收入覆盖生活支出",
        story = "你终于拥有了地球上最昂贵的东西——自由。",
        category = "职业",
        rarity = "epic",
        rate = 5.2
    ),
    PresetAchievement(
        id = 13,
        title = "看一次极光",
        description = "在北极圈附近目睹大自然的灯光秀",
        story = "绿色的光带在头顶铺开，你站在原地半天说不出话。",
        category = "旅行",
        rarity = "rare",
        rate = 12.0
    ),
    PresetAchievement(
        id = 109,
        title = "幸存者",
        description = "活到今天",
        story = "你成功活到了现在，这本身就是一件了不起的事。",
        category = "成长",
        rarity = "common",
        rate = 100.0
    )
)

@Preview(showBackground = true, heightDp = 1100, name = "图鉴 · 列表")
@Composable
private fun PresetCodexPreview() {
    LifeLedgerTheme {
        PresetCodexScreen(
            uiState = PresetCodexUiState(
                allItems = previewPresets,
                tierByPresetId = previewPresets.associate { it.id to RarityTier.fromRate(it.rate) },
                categories = listOf("生活", "职业", "旅行", "成长"),
                isLoaded = true
            ),
            onBack = {},
            onQueryChange = {},
            onCategoryChange = {},
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1100, name = "图鉴 · 深色")
@Composable
private fun PresetCodexDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        PresetCodexScreen(
            uiState = PresetCodexUiState(
                allItems = previewPresets,
                tierByPresetId = previewPresets.associate { it.id to RarityTier.fromRate(it.rate) },
                categories = listOf("生活", "职业", "旅行", "成长"),
                isLoaded = true
            ),
            onBack = {},
            onQueryChange = {},
            onCategoryChange = {},
            onItemClick = {}
        )
    }
}
