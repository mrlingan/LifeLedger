package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.database.DatabaseProvider
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 加密迁移在界面上呈现出来的几种状态 */
sealed interface EncryptionMigrationUi {

    /** 没在跑。开关关着，或者库里已经没有待改写的行 */
    data object Idle : EncryptionMigrationUi

    data class Running(val done: Int, val total: Int) : EncryptionMigrationUi

    /** 这一轮改写完成，[newlyEncrypted] 是真正被改写的行数 */
    data class Done(val newlyEncrypted: Int) : EncryptionMigrationUi

    /** 还有 [pending] 行没能改成密文，可以重试 */
    data class Failed(val pending: Int) : EncryptionMigrationUi
}

data class DataSecurityUiState(
    val databasePath: String = "",
    val mediaPath: String = "",
    val profilePath: String = "",
    val databaseSize: Long = 0,
    val mediaSize: Long = 0,
    val profileSize: Long = 0,
    val encryptionEnabled: Boolean = false,
    /** 库里已经有密文的行数（关闭开关时用来告诉用户"老数据还能读"） */
    val encryptedRowCount: Int = 0,
    val migration: EncryptionMigrationUi = EncryptionMigrationUi.Idle
)

/**
 * 数据安全页回答三件事：数据存在哪、有没有加密、加密到底挡得住什么。
 *
 * 「有没有加密」不再是写死的一句话，而是跟着开关变的：
 * 打开开关的那一刻才逐行迁移老数据，界面同步显示进度，失败可以重试。
 */
class DataSecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val filesDir = application.filesDir
    private val repositories = RepositoryProvider.get(application)
    private val settings = repositories.settings
    private val migration = repositories.dataEncryptionMigration

    private val _uiState = MutableStateFlow(DataSecurityUiState())
    val uiState: StateFlow<DataSecurityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val storage = withContext(Dispatchers.IO) {
                // 数据库在 filesDir 之外的 databases/ 目录，路径必须按真实的来，
                // 这页的作用就是让用户知道数据到底在哪
                val db = DatabaseProvider.databaseFile(application).parentFile
                val media = File(filesDir, "media")
                val profile = File(filesDir, "profile")
                DataSecurityUiState(
                    databasePath = db?.absolutePath.orEmpty(),
                    mediaPath = media.absolutePath,
                    profilePath = profile.absolutePath,
                    databaseSize = db?.sizeOf() ?: 0L,
                    mediaSize = media.sizeOf(),
                    profileSize = profile.sizeOf()
                )
            }
            _uiState.update { it.copy(
                databasePath = storage.databasePath,
                mediaPath = storage.mediaPath,
                profilePath = storage.profilePath,
                databaseSize = storage.databaseSize,
                mediaSize = storage.mediaSize,
                profileSize = storage.profileSize
            ) }
        }

        viewModelScope.launch {
            settings.dataEncryptionEnabled.collect { enabled ->
                _uiState.update { it.copy(encryptionEnabled = enabled) }
            }
        }

        // 打开页面先看一眼库里还有没有明文：上次迁移被打断也靠这一步发现，
        // 这时开关是开着的，直接把状态摆成"还有 N 条没加密 + 重试"。
        viewModelScope.launch {
            val scan = withContext(Dispatchers.IO) { migration.scan() }
            _uiState.update {
                it.copy(
                    encryptedRowCount = scan.encrypted,
                    migration = if (settings.dataEncryptionEnabled.value && scan.pending > 0) {
                        EncryptionMigrationUi.Failed(scan.pending)
                    } else {
                        EncryptionMigrationUi.Idle
                    }
                )
            }
        }
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDataEncryptionEnabled(enabled)
            if (enabled) {
                runMigration()
            } else {
                // 关掉只是不再加密新写入的内容。库里已有的密文照旧留在原地，
                // 读的时候仍然会解密，所以这里什么都不用搬。
                _uiState.update { it.copy(migration = EncryptionMigrationUi.Idle) }
                refreshCounts()
            }
        }
    }

    fun retryMigration() {
        viewModelScope.launch { runMigration() }
    }

    private suspend fun runMigration() {
        _uiState.update { it.copy(migration = EncryptionMigrationUi.Running(0, 0)) }

        val result = withContext(Dispatchers.IO) {
            migration.run { done, total ->
                _uiState.update { it.copy(migration = EncryptionMigrationUi.Running(done, total)) }
            }
        }

        val scan = withContext(Dispatchers.IO) { migration.scan() }
        _uiState.update {
            it.copy(
                encryptedRowCount = scan.encrypted,
                migration = if (scan.pending > 0) {
                    EncryptionMigrationUi.Failed(scan.pending)
                } else {
                    EncryptionMigrationUi.Done(result.encrypted)
                }
            )
        }
    }

    private suspend fun refreshCounts() {
        val scan = withContext(Dispatchers.IO) { migration.scan() }
        _uiState.update { it.copy(encryptedRowCount = scan.encrypted) }
    }

    private fun File.sizeOf(): Long =
        if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
