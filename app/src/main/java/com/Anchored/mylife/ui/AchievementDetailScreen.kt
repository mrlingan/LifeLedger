package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.database.Note
import com.Anchored.mylife.ui.components.AchievementStatus
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDialogText
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppVerticalDivider
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.components.StatTile
import com.Anchored.mylife.ui.components.StatusBadge
import com.Anchored.mylife.ui.components.TimelineItem
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
import java.util.TimeZone

/**
 * 成就详情：整个 App 里最该有分量的一页。
 *
 * 目标是「人生纪念碑」而不是「列表项的展开」：
 *   大图标 → 名称 → 状态与日期 → 描述 → 解锁记录 → 相关数据 → 记事本
 *
 * 全页只有两张卡片（数据卡、笔记卡），其余靠留白和分隔线组织。
 */
@Composable
fun AchievementDetailRoute(
    achievementId: Long,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: AchievementDetailViewModel = viewModel(
        key = "achievement_detail_$achievementId",
        factory = AchievementDetailViewModel.factory(context, achievementId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pickMode by remember { mutableStateOf<MediaPickMode?>(null) }
    var composerMedia by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var viewingMedia by remember { mutableStateOf<Media?>(null) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        when (val mode = pickMode) {
            MediaPickMode.Composer -> composerMedia = composerMedia + uris
            is MediaPickMode.Attach -> viewModel.attachMedia(mode.noteId, uris)
            null -> Unit
        }
        pickMode = null
    }

    val launchMediaPicker: (MediaPickMode) -> Unit = { mode ->
        pickMode = mode
        mediaPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    AchievementDetailScreen(
        uiState = uiState,
        composerMedia = composerMedia,
        onBack = { navController.popBackStack() },
        onMarkCompleted = { date -> viewModel.markCompleted(date) },
        onMarkUncompleted = viewModel::markUncompleted,
        onUpdateInfo = { title, description, emoji ->
            viewModel.updateInfo(title, description, emoji)
        },
        onDelete = { viewModel.delete { navController.popBackStack() } },
        onSaveNote = { noteId, content, attachments ->
            if (noteId == null) {
                viewModel.addNote(content = content, attachments = attachments)
            } else {
                viewModel.updateNote(noteId, content)
            }
        },
        onDeleteNote = viewModel::deleteNote,
        onViewMedia = { media -> viewingMedia = media },
        onAddMedia = { noteId -> launchMediaPicker(MediaPickMode.Attach(noteId)) },
        onPickComposerMedia = { launchMediaPicker(MediaPickMode.Composer) },
        onRemoveComposerMedia = { uri -> composerMedia = composerMedia.filterNot { it == uri } },
        onClearComposerMedia = { composerMedia = emptyList() }
    )

    viewingMedia?.let { media ->
        MediaViewerDialog(
            media = media,
            onDismiss = { viewingMedia = null },
            onDelete = {
                viewingMedia = null
                viewModel.deleteMedia(media)
            }
        )
    }
}

private sealed interface MediaPickMode {
    data object Composer : MediaPickMode
    data class Attach(val noteId: Long) : MediaPickMode
}

private sealed interface NoteEditorTarget {
    data object New : NoteEditorTarget
    data class Existing(val note: Note) : NoteEditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailScreen(
    uiState: AchievementDetailUiState,
    composerMedia: List<Uri>,
    onBack: () -> Unit,
    onMarkCompleted: (Long) -> Unit,
    onMarkUncompleted: () -> Unit,
    onUpdateInfo: (String, String, String) -> Unit,
    onDelete: () -> Unit,
    onSaveNote: (Long?, String, List<Uri>) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onViewMedia: (Media) -> Unit,
    onAddMedia: (Long) -> Unit,
    onPickComposerMedia: () -> Unit,
    onRemoveComposerMedia: (Uri) -> Unit,
    onClearComposerMedia: () -> Unit
) {
    val colors = AppTheme.colors
    val achievement = uiState.achievement

    var menuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteEditor by remember { mutableStateOf<NoteEditorTarget?>(null) }

    Scaffold(
        containerColor = colors.background,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.detail_title),
                onBack = onBack,
                actions = {
                    if (achievement != null) {
                        Box {
                            AppIconButton(
                                icon = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.common_more),
                                onClick = { menuExpanded = true }
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_menu_edit), style = AppTheme.type.body) },
                                    onClick = {
                                        menuExpanded = false
                                        showEditDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.detail_menu_delete), style = AppTheme.type.body) },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (achievement == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.isLoaded) stringResource(R.string.detail_gone) else stringResource(R.string.common_loading),
                    style = AppTheme.type.body,
                    color = colors.textSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = Spacing.xxxl)
            ) {
                item(key = "hero") {
                    DetailHero(
                        achievement = achievement,
                        onToggleComplete = {
                            if (achievement.isCompleted) {
                                onMarkUncompleted()
                            } else {
                                onMarkCompleted(System.currentTimeMillis())
                            }
                        },
                        onPickDate = { showDatePicker = true }
                    )
                }

                item(key = "timeline") {
                    Column(
                        modifier = Modifier.padding(
                            start = Sizes.gutter,
                            end = Sizes.gutter,
                            top = Spacing.xxl
                        )
                    ) {
                        SectionHeader(title = stringResource(R.string.detail_timeline))
                        Spacer(modifier = Modifier.height(Spacing.lg))

                        TimelineItem(
                            title = stringResource(R.string.detail_timeline_create),
                            time = achievement.createdDate.toDayText(),
                            emphasized = true,
                            isLast = achievement.completedDate == null
                        )

                        achievement.completedDate?.let { completedAt ->
                            TimelineItem(
                                title = stringResource(R.string.detail_timeline_done),
                                time = completedAt.toDayText(),
                                emphasized = true,
                                isLast = true
                            )
                        }
                    }
                }

                item(key = "stats") {
                    StatSection(
                        uiState = uiState,
                        modifier = Modifier.padding(
                            start = Sizes.gutter,
                            end = Sizes.gutter,
                            top = Spacing.xxl
                        )
                    )
                }

                item(key = "notes_header") {
                    SectionHeader(
                        title = stringResource(R.string.detail_notes),
                        subtitle = if (uiState.noteCount > 0) {
                            stringResource(R.string.detail_notes_count, uiState.noteCount)
                        } else {
                            null
                        },
                        action = {
                            AppButton(
                                text = stringResource(R.string.common_add),
                                onClick = { noteEditor = NoteEditorTarget.New },
                                variant = AppButtonVariant.Text,
                                leadingIcon = Icons.Outlined.Add
                            )
                        },
                        modifier = Modifier.padding(
                            start = Sizes.gutter,
                            end = Sizes.gutter,
                            top = Spacing.xxl,
                            bottom = Spacing.sm
                        )
                    )
                }

                if (uiState.notes.isEmpty()) {
                    item(key = "notes_empty") {
                        EmptyState(
                            title = stringResource(R.string.detail_note_empty),
                            description = stringResource(R.string.detail_note_empty_desc)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = uiState.notes,
                        key = { _, item -> item.id }
                    ) { index, note ->
                        NoteCardItem(
                            note = note,
                            mediaList = uiState.mediaByNote[note.id].orEmpty(),
                            onEdit = { noteEditor = NoteEditorTarget.Existing(note) },
                            onDelete = { onDeleteNote(note.id) },
                            onAddMedia = { onAddMedia(note.id) },
                            onMediaClick = onViewMedia,
                            modifier = Modifier
                                .padding(
                                    start = Sizes.gutter,
                                    end = Sizes.gutter,
                                    top = Spacing.sm
                                )
                                .appearAnimation(index)
                        )
                    }
                }
            }
        }
    }

    if (achievement != null && showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (achievement.completedDate ?: System.currentTimeMillis())
                .toUtcPickerMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.common_ok),
                    onClick = {
                        val picked = pickerState.selectedDateMillis
                        showDatePicker = false
                        if (picked != null) {
                            onMarkCompleted(picked.toLocalDayStart())
                        }
                    }
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = { showDatePicker = false },
                    variant = AppButtonVariant.Text
                )
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (achievement != null && showEditDialog) {
        EditAchievementDialog(
            achievement = achievement,
            onDismiss = { showEditDialog = false },
            onConfirm = { title, description, emoji ->
                showEditDialog = false
                onUpdateInfo(title, description, emoji)
            }
        )
    }

    if (achievement != null && showDeleteDialog) {
        AppDialog(
            title = stringResource(R.string.detail_menu_delete),
            onDismissRequest = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            confirmText = stringResource(R.string.common_delete),
            destructive = true,
            content = {
                AppDialogText(
                    stringResource(R.string.detail_delete_message, achievement.title)
                )
            }
        )
    }

    noteEditor?.let { target ->
        val isNewNote = target is NoteEditorTarget.New
        NoteEditorDialog(
            initialContent = when (target) {
                NoteEditorTarget.New -> ""
                is NoteEditorTarget.Existing -> target.note.content
            },
            allowMedia = isNewNote,
            pendingMedia = if (isNewNote) composerMedia else emptyList(),
            onPickMedia = onPickComposerMedia,
            onRemoveMedia = onRemoveComposerMedia,
            onDismiss = {
                noteEditor = null
                onClearComposerMedia()
            },
            onConfirm = { content ->
                when (target) {
                    NoteEditorTarget.New -> onSaveNote(null, content, composerMedia)
                    is NoteEditorTarget.Existing -> onSaveNote(target.note.id, content, emptyList())
                }
                noteEditor = null
                onClearComposerMedia()
            }
        )
    }
}

@Composable
private fun DetailHero(
    achievement: Achievement,
    onToggleComplete: () -> Unit,
    onPickDate: () -> Unit
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(Radius.hero)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Sizes.gutter, end = Sizes.gutter, top = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.heroIcon)
                .clip(shape)
                .background(colors.surfaceElevated)
                .border(Sizes.hairline, colors.border, shape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = achievement.iconEmoji,
                style = AppTheme.type.display
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = achievement.title,
            style = AppTheme.type.h1,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        StatusBadge(
            status = if (achievement.isCompleted) {
                AchievementStatus.Completed
            } else {
                AchievementStatus.InProgress
            },
            text = achievement.statusText()
        )

        if (achievement.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            Text(
                text = achievement.description,
                style = AppTheme.type.bodyLarge,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            AppButton(
                text = if (achievement.isCompleted) stringResource(R.string.detail_undo_complete) else stringResource(R.string.detail_timeline_done),
                onClick = onToggleComplete,
                variant = if (achievement.isCompleted) {
                    AppButtonVariant.Secondary
                } else {
                    AppButtonVariant.Primary
                },
                leadingIcon = if (achievement.isCompleted) null else Icons.Outlined.Check
            )
            AppButton(
                text = stringResource(R.string.detail_pick_date),
                onClick = onPickDate,
                variant = AppButtonVariant.Text
            )
        }
    }
}

@Composable
private fun StatSection(
    uiState: AchievementDetailUiState,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.detail_stats))

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatTile(
                value = uiState.noteCount.toString(),
                label = stringResource(R.string.backup_stat_notes),
                modifier = Modifier.weight(1f)
            )

            AppVerticalDivider(height = Spacing.xxl)

            StatTile(
                value = uiState.mediaCount.toString(),
                label = stringResource(R.string.backup_stat_media),
                modifier = Modifier.weight(1f)
            )

            AppVerticalDivider(height = Spacing.xxl)

            StatTile(
                value = uiState.daysToComplete?.toString() ?: "—",
                label = stringResource(R.string.detail_stat_duration),
                supporting = if (uiState.daysToComplete != null) stringResource(R.string.home_stat_days) else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NoteCardItem(
    note: Note,
    mediaList: List<Media>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddMedia: () -> Unit,
    onMediaClick: (Media) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(modifier = modifier) {
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                style = AppTheme.type.body,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        MediaStrip(
            mediaList = mediaList,
            onMediaClick = onMediaClick,
            onAddClick = onAddMedia
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.detail_note_updated,
                    note.updatedDate.toDateTimeText()
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f)
            )
            AppIconButton(
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.detail_note_edit),
                onClick = onEdit,
                tint = colors.textSecondary
            )
            AppIconButton(
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.detail_cd_delete_note),
                onClick = onDelete,
                tint = colors.error
            )
        }
    }
}

@Composable
private fun EditAchievementDialog(
    achievement: Achievement,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(achievement.title) }
    var description by remember { mutableStateOf(achievement.description) }
    var iconEmoji by remember { mutableStateOf(achievement.iconEmoji) }

    AppDialog(
        title = stringResource(R.string.detail_menu_edit),
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(title, description, iconEmoji) },
        confirmText = stringResource(R.string.common_save),
        content = {
            Column {
                EmojiPickerRow(
                    selected = iconEmoji,
                    onSelect = { iconEmoji = it }
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.add_field_title),
                    placeholder = stringResource(R.string.detail_field_title_hint)
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.detail_field_desc),
                    placeholder = stringResource(R.string.detail_field_desc_hint),
                    singleLine = false,
                    minLines = 3
                )
            }
        }
    )
}

@Composable
private fun NoteEditorDialog(
    initialContent: String,
    allowMedia: Boolean,
    pendingMedia: List<Uri>,
    onPickMedia: () -> Unit,
    onRemoveMedia: (Uri) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }

    AppDialog(
        title = if (initialContent.isBlank()) stringResource(R.string.detail_note_new) else stringResource(R.string.detail_note_edit),
        onDismissRequest = onDismiss,
        onConfirm = { onConfirm(content) },
        confirmText = stringResource(R.string.common_save),
        content = {
            Column {
                AppTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = stringResource(R.string.detail_note_hint),
                    singleLine = false,
                    minLines = 4
                )

                if (allowMedia) {
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    Text(
                        text = stringResource(R.string.detail_note_media),
                        style = AppTheme.type.caption,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    PendingMediaRow(
                        uris = pendingMedia,
                        onRemove = onRemoveMedia,
                        onAddClick = onPickMedia
                    )
                }
            }
        }
    )
}

// ---------------------------------------------------------------------------
// 日期工具
// ---------------------------------------------------------------------------

private val dayFormatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
private val dateTimeFormatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())

private fun Long.toDayText(): String = dayFormatter.format(Date(this))

private fun Long.toDateTimeText(): String = dateTimeFormatter.format(Date(this))

@Composable
private fun Achievement.statusText(): String = if (isCompleted && completedDate != null) {
    stringResource(R.string.detail_status_completed, completedDate.toDayText())
} else {
    stringResource(R.string.status_in_progress)
}

/** 系统日期选择器给的是 UTC 零点，转成本地时区的当天零点再入库 */
private fun Long.toLocalDayStart(): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = this@toLocalDayStart
    }
    return Calendar.getInstance().apply {
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** 本地时间戳 -> 日期选择器需要的 UTC 零点 */
private fun Long.toUtcPickerMillis(): Long {
    val local = Calendar.getInstance().apply { timeInMillis = this@toUtcPickerMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

private val previewDetailAchievement = Achievement(
    id = 1,
    title = "完成人生第一场马拉松",
    description = "42.195 公里，净成绩 4 小时 12 分。起跑下雨，35 公里撞墙，最后 7 公里靠志愿者的加油声撑下来。",
    createdDate = 1_735_689_600_000,
    completedDate = 1_747_008_000_000,
    isCompleted = true,
    iconEmoji = "🏅"
)

private val previewDetailNotes = listOf(
    Note(
        id = 1,
        achievementId = 1,
        content = "领物的时候遇到一个跑了 20 年的大叔，他说第一次跑全马都会崩，很正常。",
        createdDate = 1_746_000_000_000,
        updatedDate = 1_747_008_000_000
    )
)

@Preview(showBackground = true, heightDp = 1200, name = "详情 · 已完成")
@Composable
private fun AchievementDetailPreview() {
    LifeLedgerTheme {
        AchievementDetailScreen(
            uiState = AchievementDetailUiState(
                achievement = previewDetailAchievement,
                notes = previewDetailNotes,
                mediaByNote = emptyMap(),
                isLoaded = true
            ),
            composerMedia = emptyList(),
            onBack = {},
            onMarkCompleted = {},
            onMarkUncompleted = {},
            onUpdateInfo = { _, _, _ -> },
            onDelete = {},
            onSaveNote = { _, _, _ -> },
            onDeleteNote = {},
            onViewMedia = {},
            onAddMedia = {},
            onPickComposerMedia = {},
            onRemoveComposerMedia = {},
            onClearComposerMedia = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "详情 · 深色")
@Composable
private fun AchievementDetailDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        AchievementDetailScreen(
            uiState = AchievementDetailUiState(
                achievement = previewDetailAchievement.copy(isCompleted = false, completedDate = null),
                notes = emptyList(),
                isLoaded = true
            ),
            composerMedia = emptyList(),
            onBack = {},
            onMarkCompleted = {},
            onMarkUncompleted = {},
            onUpdateInfo = { _, _, _ -> },
            onDelete = {},
            onSaveNote = { _, _, _ -> },
            onDeleteNote = {},
            onViewMedia = {},
            onAddMedia = {},
            onPickComposerMedia = {},
            onRemoveComposerMedia = {},
            onClearComposerMedia = {}
        )
    }
}
