package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.PresetAchievement
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.ui.components.AchievementCard
import com.Anchored.mylife.ui.components.AchievementStatus
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import kotlinx.coroutines.launch

/** 可选图标。图标体系（iconKey）落地后，这里会换成矢量图标网格。 */
internal val EmojiChoices = listOf(
    "🏆", "🏅", "🎓", "📚", "💪", "🏃", "🚀", "🎯",
    "🎨", "🎵", "🎮", "🎬", "📷", "✈️", "🌏", "🏠",
    "💰", "❤️", "🌱", "🍳", "🧠", "🤝", "⛰️", "🔥"
)

class AddAchievementViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val achievementRepository = repositories.achievementRepository
    private val presetRepository = repositories.presetAchievementRepository
    private val settings = repositories.settings

    /** 新建成就时的默认图标：在「设置 → 成就设置」里挑过就用挑过的 */
    val defaultIcon: String get() = settings.defaultIcon.value

    /** 从图鉴带过来的预设成就；presetId 有效时才有值 */
    suspend fun loadPreset(presetId: Long): PresetAchievement? =
        presetRepository.getById(presetId)

    fun save(
        title: String,
        description: String,
        iconEmoji: String,
        presetId: Long?,
        onSaved: () -> Unit
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            achievementRepository.createAchievement(
                title = title,
                description = description,
                iconEmoji = iconEmoji.ifBlank { AchievementRepository.DEFAULT_ICON },
                presetId = presetId
            )
            onSaved()
        }
    }
}

@Composable
fun AddAchievementRoute(
    navController: NavHostController,
    presetId: Long = -1L,
    viewModel: AddAchievementViewModel = viewModel()
) {
    val context = LocalContext.current
    val presetTexts = rememberPresetTexts()
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var iconEmoji by rememberSaveable { mutableStateOf(viewModel.defaultIcon) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var sourceLabel by remember { mutableStateOf<String?>(null) }

    // 从图鉴挑进来的：把标题、描述、图标预填好，用户还能继续改
    LaunchedEffect(presetId) {
        if (presetId <= 0L) return@LaunchedEffect
        val preset = viewModel.loadPreset(presetId) ?: return@LaunchedEffect
        if (title.isBlank() && description.isBlank()) {
            val text = presetTexts.textOf(preset)
            title = text.title
            description = text.description
            if (preset.iconEmoji.isNotBlank()) {
                iconEmoji = preset.iconEmoji
            }
            sourceLabel = context.getString(
                R.string.add_source,
                presetTexts.categoryOf(preset.category),
                "${preset.rate}%"
            )
        }
    }

    AddAchievementScreen(
        title = title,
        description = description,
        iconEmoji = iconEmoji,
        sourceLabel = sourceLabel,
        titleError = titleError,
        onTitleChange = {
            title = it
            if (it.isNotBlank()) titleError = false
        },
        onDescriptionChange = { description = it },
        onEmojiChange = { iconEmoji = it },
        onBack = { navController.popBackStack() },
        onSave = {
            if (title.isBlank()) {
                titleError = true
            } else {
                viewModel.save(
                    title = title,
                    description = description,
                    iconEmoji = iconEmoji,
                    presetId = presetId.takeIf { it > 0L },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    )
}

@Composable
fun AddAchievementScreen(
    title: String,
    description: String,
    iconEmoji: String,
    sourceLabel: String?,
    titleError: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AppTheme.colors

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(title = stringResource(R.string.add_title), onBack = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(Sizes.gutter)) {

                Text(
                    text = stringResource(R.string.add_preview),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                AchievementCard(
                    title = title.ifBlank { stringResource(R.string.add_untitled) },
                    description = description.takeIf { it.isNotBlank() },
                    icon = {
                        Text(
                            text = iconEmoji,
                            style = AppTheme.type.numberMedium
                        )
                    },
                    status = AchievementStatus.InProgress,
                    onClick = {}
                )

                if (sourceLabel != null) {
                    Spacer(modifier = Modifier.height(Spacing.xl))

                    AppCard(tone = AppCardTone.Accent) {
                        Text(
                            text = stringResource(R.string.add_from_codex),
                            style = AppTheme.type.caption,
                            color = colors.accentStrong
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = sourceLabel,
                            style = AppTheme.type.bodySmall,
                            color = colors.accentStrong
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xxl))

                Text(
                    text = stringResource(R.string.add_icon),
                    style = AppTheme.type.caption,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                EmojiPickerRow(
                    selected = iconEmoji,
                    onSelect = onEmojiChange
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                AppTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = stringResource(R.string.add_field_title),
                    placeholder = stringResource(R.string.add_field_title_hint),
                    isError = titleError,
                    supportingText = if (titleError) stringResource(R.string.add_title_required) else null
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AppTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.add_field_desc),
                    placeholder = stringResource(R.string.add_field_desc_hint),
                    singleLine = false,
                    minLines = 4
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                AppButton(
                    text = stringResource(R.string.add_save),
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.md))

                Text(
                    text = stringResource(R.string.add_save_hint),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }
    }
}

/** 图标选择器。选中项用强调色浅底 + 细描边，未选中只有底色。 */
@Composable
internal fun EmojiPickerRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.md)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(items = EmojiChoices, key = { it }) { emoji ->
            val isSelected = emoji == selected

            Box(
                modifier = Modifier
                    .size(Sizes.avatarMd)
                    .clip(shape)
                    .background(if (isSelected) colors.accentSoft else colors.surfaceSunken)
                    .then(
                        if (isSelected) {
                            Modifier.border(Sizes.hairline, colors.accent, shape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = AppTheme.type.h2
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "新建成就")
@Composable
private fun AddAchievementPreview() {
    LifeLedgerTheme {
        AddAchievementScreen(
            title = "完成人生第一场马拉松",
            description = "42.195 公里，净成绩 4 小时 12 分",
            iconEmoji = "🏅",
            sourceLabel = "兴趣 · 8% 的人达成",
            titleError = false,
            onTitleChange = {},
            onDescriptionChange = {},
            onEmojiChange = {},
            onBack = {},
            onSave = {}
        )
    }
}
