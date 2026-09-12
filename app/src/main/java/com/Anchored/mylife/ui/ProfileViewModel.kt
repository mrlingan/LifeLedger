package com.Anchored.mylife.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 昵称和签名的长度上限：首页问候语和列表里都放得下就够了 */
private const val MAX_NICKNAME = 20
private const val MAX_SIGNATURE = 40

data class ProfileUiState(
    val nickname: String = "",
    val signature: String = "",
    /** 已保存的头像路径 */
    val savedAvatarPath: String? = null,
    /** 刚选中、还没保存的头像，只用来预览；保存时才真正复制进私有目录 */
    val pickedUri: Uri? = null,
    val avatarRemoved: Boolean = false,
    val isLoaded: Boolean = false
) {
    /** 预览用的头像路径 */
    val previewAvatarPath: String? get() = if (avatarRemoved) null else savedAvatarPath

    val hasAvatar: Boolean get() = pickedUri != null || (!avatarRemoved && savedAvatarPath != null)
}

/**
 * 个人资料：昵称、签名、头像。
 *
 * 有意做成"草稿 + 保存"：头像在保存前不进私有目录，用户中途退出不会留下一堆孤儿文件，
 * 旧的头像也一直留着，直到新头像写成功才会被删掉。
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings = repositories.settings
    private val imageStore = repositories.profileImageStore

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = ProfileUiState(
            nickname = settings.nickname.value,
            signature = settings.signature.value,
            savedAvatarPath = settings.avatarPath.value,
            isLoaded = true
        )
    }

    fun setNickname(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value.take(MAX_NICKNAME))
    }

    fun setSignature(value: String) {
        _uiState.value = _uiState.value.copy(signature = value.take(MAX_SIGNATURE))
    }

    fun pickAvatar(uri: Uri) {
        _uiState.value = _uiState.value.copy(pickedUri = uri, avatarRemoved = false)
    }

    fun removeAvatar() {
        _uiState.value = _uiState.value.copy(pickedUri = null, avatarRemoved = true)
    }

    /** 保存：先把头像文件落地，再一次性写进设置，最后回到上一页 */
    fun save(onSaved: () -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            val avatarPath = withContext(Dispatchers.IO) {
                when {
                    current.pickedUri != null -> runCatching {
                        imageStore.replace(current.pickedUri, current.savedAvatarPath)
                    }.getOrNull()

                    current.avatarRemoved -> {
                        imageStore.delete(current.savedAvatarPath)
                        null
                    }

                    else -> current.savedAvatarPath
                }
            }
            settings.setProfile(
                nickname = current.nickname,
                signature = current.signature,
                avatarPath = avatarPath
            )
            onSaved()
        }
    }
}
