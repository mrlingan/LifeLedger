package com.Anchored.mylife.ui

import com.Anchored.mylife.R

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppSegmentedControl
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.appearAnimation
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 「全部成就」页：成就的唯一完整入口。
 *
 * 为什么要从首页搬出来：
 * - 首页的职责是「一眼看到你已经完成了多少」，列表越长越像一份待办清单；
 * - 列表放在这里之后，筛选、状态切换、搜索式的浏览都有了稳定的归属，
 *   首页再多成就也不会被撑长。
 */
@Composable
fun AllAchievementsRoute(
    navController: NavHostController,
    viewModel: AchievementListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddOptions by remember { mutableStateOf(false) }

    AllAchievementsScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onAchievementClick = { id -> navController.navigate("achievement_detail/$id") },
        onToggleCompleted = viewModel::toggleCompleted,
        onFilterChange = viewModel::setFilter,
        onAddClick = { showAddOptions = true }
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
fun AllAchievementsScreen(
    uiState: AchievementListUiState,
    onBack: () -> Unit,
    onAchievementClick: (Long) -> Unit,
    onToggleCompleted: (Long) -> Unit,
    onFilterChange: (AchievementFilter) -> Unit,
    onAddClick: () -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_all),
                onBack = onBack,
                actions = {
                    AppIconButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.add_title),
                        onClick = onAddClick
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 筛选固定在顶部：列表很长时也能随手切换，不用滚回最上面
            AppSegmentedControl(
                options = AchievementFilter.entries.map {
                    "${stringResource(it.labelRes)} ${uiState.countOf(it)}"
                },
                selectedIndex = AchievementFilter.entries.indexOf(uiState.filter),
                onSelect = { index -> onFilterChange(AchievementFilter.entries[index]) },
                modifier = Modifier.padding(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    top = Spacing.md,
                    bottom = Spacing.sm
                )
            )

            when {
                // 首帧还没读到数据库：什么都不画，避免空状态闪一下
                !uiState.isLoaded -> Box(modifier = Modifier.fillMaxSize())

                uiState.totalCount == 0 -> EmptyState(
                    title = stringResource(R.string.home_empty_title),
                    description = stringResource(R.string.home_empty_desc),
                    action = {
                        AppButton(
                            text = stringResource(R.string.home_empty_action),
                            onClick = onAddClick
                        )
                    }
                )

                uiState.visibleAchievements.isEmpty() -> EmptyState(
                    title = stringResource(R.string.home_filter_empty)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xxxl)
                ) {
                    itemsIndexed(
                        items = uiState.visibleAchievements,
                        key = { _, item -> item.id }
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
