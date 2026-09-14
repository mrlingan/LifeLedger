package com.Anchored.mylife.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.data.database.Media
import com.Anchored.mylife.data.database.Note
import com.Anchored.mylife.data.media.MediaFileStore
import com.Anchored.mylife.data.media.MotionPhotoExtractor
import com.Anchored.mylife.data.repository.AchievementRepository
import com.Anchored.mylife.data.repository.MediaRepository
import com.Anchored.mylife.data.repository.NoteRepository
import com.Anchored.mylife.data.repository.PresetAchievementRepository
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

/**
 * 成就详情状态。
 *
 * 派生数据（笔记数、媒体数、完成用时）都在这里算好，
 * 页面只负责显示。
 */
data class AchievementDetailUiState(
    val achievement: Achievement? = null,
    val notes: List<Note> = emptyList(),
    val mediaByNote: Map<Long, List<Media>> = emptyMap(),
    val isLoaded: Boolean = false
) {
    val noteCount: Int get() = notes.size

    val mediaCount: Int get() = mediaByNote.values.sumOf { it.size }

    /** 从记录到完成用了多少天；未完成返回 null */
    val daysToComplete: Long?
        get() {
            val item = achievement ?: return null
            val end = item.completedDate ?: return null
            return ((end - item.createdDate) / DAY_MILLIS).coerceAtLeast(0L)
        }
}

class AchievementDetailViewModel(
    private val appContext: Context,
    private val achievementRepository: AchievementRepository,
    private val noteRepository: NoteRepository,
    private val mediaRepository: MediaRepository,
    private val settings: AppSettings,
    private val presetRepository: PresetAchievementRepository,
    private val achievementId: Long
) : ViewModel() {

    private val mediaFileStore = MediaFileStore(appContext)

    val uiState: StateFlow<AchievementDetailUiState> = combine(
        achievementRepository.observeAchievementById(achievementId),
        noteRepository.observeNotesByAchievementId(achievementId),
        mediaRepository.observeMediaByAchievementId(achievementId)
    ) { achievement, notes, media ->
        AchievementDetailUiState(
            achievement = achievement,
            notes = notes,
            mediaByNote = media.groupBy { it.noteId },
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AchievementDetailUiState()
    )

    /** 标记完成，日期可以自定义（默认现在） */
    fun markCompleted(completedDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            achievementRepository.markCompleted(achievementId, completedDate)
        }
    }

    fun markUncompleted() {
        viewModelScope.launch {
            achievementRepository.markUncompleted(achievementId)
        }
    }

    /** 可选的分类：图鉴内置 + 用户新建 + 自己成就上用过的 */
    val categories: StateFlow<List<String>> = categoryDirectoryFlow(
        settings = settings,
        presetRepository = presetRepository,
        achievementRepository = achievementRepository
    )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCategory(name: String) = settings.addCustomCategory(name)

    fun updateInfo(
        title: String,
        description: String,
        iconEmoji: String,
        category: String
    ) {
        viewModelScope.launch {
            achievementRepository.updateAchievementInfo(
                achievementId = achievementId,
                title = title,
                description = description,
                iconEmoji = iconEmoji,
                category = category
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            val achievement = uiState.value.achievement
            if (achievement != null) {
                achievementRepository.deleteAchievement(achievement)
            }
            onDeleted()
        }
    }

    fun addNote(content: String, attachments: List<Uri>) {
        if (content.isBlank() && attachments.isEmpty()) return
        viewModelScope.launch {
            val noteId = noteRepository.createNote(
                achievementId = achievementId,
                content = content.trim()
            )
            importMedia(noteId = noteId, uris = attachments)
        }
    }

    fun attachMedia(noteId: Long, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch { importMedia(noteId = noteId, uris = uris) }
    }

    fun updateNote(noteId: Long, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            noteRepository.updateNoteContent(noteId = noteId, content = content.trim())
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            noteRepository.deleteNoteById(noteId)
        }
    }

    fun deleteMedia(media: Media) {
        viewModelScope.launch {
            mediaRepository.deleteMediaWithFiles(media)
        }
    }

    /**
     * 把选中的内容复制进私有目录再入库。
     * 是 JPEG 的话顺手检测一次实况照片，能抽出视频就记成 LIVE_PHOTO。
     */
    private suspend fun importMedia(noteId: Long, uris: List<Uri>) {
        uris.forEach { uri ->
            runCatching {
                val path = withContext(Dispatchers.IO) {
                    mediaFileStore.copyToPrivateStorage(uri, fallbackExtension = ".jpg")
                }

                val mimeType = runCatching { appContext.contentResolver.getType(uri) }.getOrNull()
                val detectedType = mediaRepository.resolveFileType(mimeType, path)

                val motionVideoPath = if (detectedType == MediaRepository.FileType.IMAGE) {
                    withContext(Dispatchers.IO) {
                        MotionPhotoExtractor.extractVideo(File(path))?.absolutePath
                    }
                } else {
                    null
                }

                mediaRepository.addMedia(
                    noteId = noteId,
                    filePath = path,
                    fileType = if (motionVideoPath != null) {
                        MediaRepository.FileType.LIVE_PHOTO
                    } else {
                        detectedType
                    },
                    motionVideoPath = motionVideoPath
                )
            }
        }
    }

    companion object {
        fun factory(context: Context, achievementId: Long): ViewModelProvider.Factory {
            val repositories = RepositoryProvider.get(context.applicationContext)
            return viewModelFactory {
                initializer {
                    AchievementDetailViewModel(
                        appContext = context.applicationContext,
                        achievementRepository = repositories.achievementRepository,
                        noteRepository = repositories.noteRepository,
                        mediaRepository = repositories.mediaRepository,
                        settings = repositories.settings,
                        presetRepository = repositories.presetAchievementRepository,
                        achievementId = achievementId
                    )
                }
            }
        }
    }
}
