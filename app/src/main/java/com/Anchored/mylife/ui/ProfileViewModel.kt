package com.Anchored.mylife.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.profile.Gender
import com.Anchored.mylife.data.profile.MbtiType
import com.Anchored.mylife.data.repository.RepositoryProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 昵称和签名的长度上限：首页问候语和列表里都放得下就够了 */
private const val MAX_NICKNAME = 20
private const val MAX_SIGNATURE = 40

data class ProfileUiState(
    val nickname: String = "",
    val signature: String = "",
    /** 已保存的头像路径（上传的那张图） */
    val savedAvatarPath: String? = null,
    /** 已保存的内置头像 */
    val savedAvatarPreset: AvatarPreset? = null,
    /** 刚选中、还没保存的头像，只用来预览；保存时才真正复制进私有目录 */
    val pickedUri: Uri? = null,
    /** 刚选中、还没保存的内置头像 */
    val pickedPreset: AvatarPreset? = null,
    /** 性别：点了就生效，没有草稿，所以这里存的就是已落盘的值 */
    val gender: Gender? = null,
    /** MBTI：同上 */
    val mbti: MbtiType? = null,
    val isLoaded: Boolean = false
) {
    /** 预览用的图片路径：草稿里已经有新头像时，旧图不再参与显示 */
    val previewAvatarPath: String?
        get() = if (pickedUri == null && pickedPreset == null) savedAvatarPath else null

    /** 预览用的内置头像，规则同上 */
    val previewAvatarPreset: AvatarPreset?
        get() = when {
            pickedPreset != null -> pickedPreset
            pickedUri != null -> null
            else -> savedAvatarPreset
        }

    /** 当前这张头像是上传的图片：决定按钮写「上传」还是「更换」 */
    val hasUploadedAvatar: Boolean
        get() = pickedUri != null || (pickedPreset == null && savedAvatarPath != null)
}

/** 保存时真正落盘的那份头像：上传的图片路径与内置头像，二选一 */
private data class SavedAvatar(val path: String?, val preset: AvatarPreset?)

/**
 * 个人资料：昵称、签名、头像，外加「基础信息」里的性别与 MBTI。
 *
 * 有意做成"草稿 + 保存"：头像在保存前不进私有目录，用户中途退出不会留下一堆孤儿文件，
 * 旧的头像文件也一直留着，直到新头像写成功才会被删掉。
 * 性别与 MBTI 不走这套：它们就是一个选项，选了当场落地，多一次确认没有意义
 * （也因此没有清空/回滚的中间态，见 [discardEdits]）。
 *
 * 头像只有"换一个"没有"拿掉"：怎么都轮不到一个空圈——没挑内置头像、也没上传图片时，
 * 界面显示昵称的第一个字（见 AppAvatar），所以这里没有清空头像的方法。
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repositories = RepositoryProvider.get(application)
    private val settings = repositories.settings
    private val imageStore = repositories.profileImageStore

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    /**
     * 阶段：资料卡上那枚 Lv.N 牌子。
     *
     * 和首页、「我的」用同一套口径（[levelOf]）现算，不存副本——完成一条成就，
     * 三处的数字必须同时变，而且不能出现"资料页说 Lv.3、首页说 Lv.4"。
     */
    val level: StateFlow<Int> = repositories.achievementRepository
        .observeAllAchievements()
        .map { achievements -> levelOf(achievements.count { it.isCompleted }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 1
        )

    init {
        _uiState.value = ProfileUiState(
            nickname = settings.nickname.value,
            signature = settings.signature.value,
            savedAvatarPath = settings.avatarPath.value,
            savedAvatarPreset = settings.avatarPreset.value,
            gender = settings.gender.value,
            mbti = settings.mbti.value,
            isLoaded = true
        )
    }

    fun setNickname(value: String) {
        _uiState.value = _uiState.value.copy(nickname = value.take(MAX_NICKNAME))
    }

    fun setSignature(value: String) {
        _uiState.value = _uiState.value.copy(signature = value.take(MAX_SIGNATURE))
    }

    /** 上传一张自己的图：图片和内置头像只能留一个，所以顺手把内置头像的草稿清掉 */
    fun pickAvatar(uri: Uri) {
        _uiState.value = _uiState.value.copy(pickedUri = uri, pickedPreset = null)
    }

    /** 挑内置头像：同理，把刚选的图片草稿清掉 */
    fun pickPreset(preset: AvatarPreset) {
        _uiState.value = _uiState.value.copy(pickedUri = null, pickedPreset = preset)
    }

    /**
     * 放弃草稿：把界面退回上一次保存的样子。
     *
     * 「基础信息」那两项不在草稿里（点了就是落地），所以这里只回滚昵称、签名和头像；
     * 头像文件没写进私有目录，回滚只是丢掉一个还没保存的 uri，磁盘上什么都没变。
     */
    fun discardEdits() {
        _uiState.value = _uiState.value.copy(
            nickname = settings.nickname.value,
            signature = settings.signature.value,
            pickedUri = null,
            pickedPreset = null
        )
    }

    /** 性别：点了立刻落盘，没有"保存"这一步 */
    fun setGender(value: Gender?) {
        settings.setGender(value)
        _uiState.value = _uiState.value.copy(gender = value)
    }

    /** MBTI：同上 */
    fun setMbti(value: MbtiType?) {
        settings.setMbti(value)
        _uiState.value = _uiState.value.copy(mbti = value)
    }

    /** 保存：先把头像文件落地，再一次性写进设置，最后回到上一页 */
    fun save(onSaved: () -> Unit) {
        val current = _uiState.value
        viewModelScope.launch {
            val avatar = withContext(Dispatchers.IO) {
                when {
                    current.pickedUri != null -> {
                        val path = runCatching {
                            imageStore.replace(current.pickedUri, current.savedAvatarPath)
                        }.getOrNull()
                        // 复制失败就保持原来那张：宁可没换，也不能存一个指向空文件的路径
                        if (path != null) {
                            SavedAvatar(path = path, preset = null)
                        } else {
                            SavedAvatar(current.savedAvatarPath, current.savedAvatarPreset)
                        }
                    }

                    current.pickedPreset != null -> {
                        // 换成内置头像之后，那张上传的图就没有任何引用了
                        imageStore.delete(current.savedAvatarPath)
                        SavedAvatar(path = null, preset = current.pickedPreset)
                    }

                    else -> SavedAvatar(current.savedAvatarPath, current.savedAvatarPreset)
                }
            }
            settings.setProfile(
                nickname = current.nickname,
                signature = current.signature,
                avatarPath = avatar.path,
                avatarPreset = avatar.preset
            )
            onSaved()
        }
    }
}
