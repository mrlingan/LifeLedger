package com.Anchored.mylife.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AchievementIconView
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.IconUploadTile
import com.Anchored.mylife.data.achievement.AchievementIcon
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 成就设置：默认图标、完成前二次确认、关注的分类。
 *
 * 这三项都只影响"记录成就"这条主线上的手感，不动任何既有数据。
 */
@Composable
fun AchievementSettingsRoute(
    navController: NavHostController,
    viewModel: AchievementSettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AchievementSettingsScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onIconChange = viewModel::setDefaultIcon,
        onConfirmChange = viewModel::setConfirmCompletion,
        onToggleCategory = viewModel::toggleCategory
    )
}

@Composable
fun AchievementSettingsScreen(
    uiState: AchievementSettingsUiState,
    onBack: () -> Unit,
    onIconChange: (String) -> Unit,
    onConfirmChange: (Boolean) -> Unit,
    onToggleCategory: (String) -> Unit
) {
    val colors = AppTheme.colors
    val presetTexts = rememberPresetTexts()

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.achievement_settings_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Sizes.gutter)
                .padding(bottom = Spacing.xxl)
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            SettingLabel(
                title = stringResource(R.string.achievement_settings_icon),
                description = stringResource(R.string.achievement_settings_icon_desc)
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            IconPicker(
                choices = EmojiChoices,
                selected = uiState.defaultIcon,
                onSelect = onIconChange
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            AppCard(
                tone = AppCardTone.Glass,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = Spacing.lg
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.achievement_settings_confirm),
                            style = AppTheme.type.bodyLarge,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(vertical = Spacing.lg)
                        )
                        Text(
                            text = stringResource(R.string.achievement_settings_confirm_desc),
                            style = AppTheme.type.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = Spacing.lg)
                        )
                    }
                    AppSwitch(
                        checked = uiState.confirmCompletion,
                        onCheckedChange = onConfirmChange
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            SettingLabel(
                title = stringResource(R.string.achievement_settings_categories),
                description = stringResource(R.string.achievement_settings_categories_desc)
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            CategoryPicker(
                categories = uiState.allCategories,
                selected = uiState.favoriteCategories,
                labelOf = presetTexts::categoryOf,
                onToggle = onToggleCategory
            )

            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = stringResource(R.string.achievement_settings_categories_hint),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }
    }
}

@Composable
private fun SettingLabel(title: String, description: String) {
    val colors = AppTheme.colors
    Text(
        text = title,
        style = AppTheme.type.bodyLarge,
        color = colors.textPrimary
    )
    Spacer(modifier = Modifier.height(Spacing.xxs))
    Text(
        text = description,
        style = AppTheme.type.bodySmall,
        color = colors.textSecondary
    )
}

/**
 * 默认图标：一行八个，选中项用强调色描边，不做大色块。
 *
 * 最后一行是「上传」：用户可以拿自己的图当默认图标。选中的图标如果是上传的图，
 * 它会先于 emoji 清单单独摆出来——否则"当前选中的是哪个"在格子里根本看不见。
 */
@Composable
private fun IconPicker(
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)
    val tiles = if (AchievementIcon.isCustom(selected)) {
        listOf(selected) + choices
    } else {
        choices
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        tiles.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { choice ->
                    val isSelected = choice == selected
                    Box(
                        modifier = Modifier
                            .size(Sizes.avatarMd)
                            .clip(shape)
                            .background(if (isSelected) colors.accentSoft else colors.surfaceSunken)
                            .border(
                                Sizes.hairline,
                                if (isSelected) colors.accent else colors.border,
                                shape
                            )
                            .clickable { onSelect(choice) },
                        contentAlignment = Alignment.Center
                    ) {
                        AchievementIconView(
                            icon = choice,
                            size = Sizes.avatarMd,
                            textStyle = AppTheme.type.h3
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            IconUploadTile(previousIcon = selected, onPicked = onSelect)
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<String>,
    selected: Set<String>,
    labelOf: (String) -> String,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { category ->
                    AppChip(
                        label = labelOf(category),
                        selected = category in selected,
                        onClick = { onToggle(category) }
                    )
                }
            }
        }
    }
}
