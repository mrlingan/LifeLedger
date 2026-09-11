package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.Anchored.mylife.data.backup.BackupManager
import com.Anchored.mylife.data.backup.BackupSummary
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val stats: BackupSummary = BackupSummary(),
    val isBusy: Boolean = false,
    val message: String? = null
)

class BackupViewModel(
    private val appContext: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    fun suggestedFileName(): String = backupManager.suggestedFileName()

    fun export(uri: Uri) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = runCatching { backupManager.exportTo(uri) }
            _uiState.update { state ->
                state.copy(
                    isBusy = false,
                    message = result.fold(
                        onSuccess = {
                            appContext.getString(
                                R.string.backup_export_success,
                                it.achievementCount,
                                it.noteCount,
                                it.mediaCount
                            )
                        },
                        onFailure = {
                            appContext.getString(
                                R.string.backup_export_failed,
                                it.message
                                    ?: appContext.getString(R.string.common_unknown_error)
                            )
                        }
                    )
                )
            }
        }
    }

    fun import(uri: Uri) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = runCatching { backupManager.importFrom(uri) }
            val stats = runCatching { backupManager.stats() }.getOrElse { BackupSummary() }
            _uiState.update { state ->
                state.copy(
                    stats = stats,
                    isBusy = false,
                    message = result.fold(
                        onSuccess = {
                            appContext.getString(
                                R.string.backup_import_success,
                                it.achievementCount,
                                it.noteCount,
                                it.mediaCount
                            )
                        },
                        onFailure = {
                            appContext.getString(
                                R.string.backup_import_failed,
                                it.message
                                    ?: appContext.getString(R.string.common_unknown_error)
                            )
                        }
                    )
                )
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val stats = runCatching { backupManager.stats() }.getOrElse { BackupSummary() }
            _uiState.update { it.copy(stats = stats) }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            val manager = RepositoryProvider.get(appContext).backupManager
            return viewModelFactory {
                initializer { BackupViewModel(appContext, manager) }
            }
        }
    }
}
