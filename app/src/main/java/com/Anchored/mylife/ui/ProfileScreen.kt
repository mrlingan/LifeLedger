package com.Anchored.mylife.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.PresetAvatar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 内置头像排四列：八个正好两行，什么屏幕上都不用横滑 */
private const val PRESET_COLUMNS = 4

/**
 * 个人资料：昵称、个人签名、头像。
 *
 * 这三样都会出现在别的地方（问候语、首页副标题、设置页顶部），
 * 所以这一页只负责编辑，不做任何展示性的统计。
 */
@Composable
fun ProfileRoute(
    navController: NavHostController,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.pickAvatar(uri)
    }

    ProfileScreen(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onNicknameChange = viewModel::setNickname,
        onSignatureChange = viewModel::setSignature,
        onPickAvatar = {
            avatarPicker.launchExternal(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPickPreset = viewModel::pickPreset,
        onSave = { viewModel.save { navController.popBackStack() } }
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onPickPreset: (AvatarPreset) -> Unit,
    onSave: () -> Unit
) {
    val colors = AppTheme.colors
    val pickedImage = uiState.pickedUri?.let { uri ->
        rememberUriThumbnail(uri = uri, sizePx = (Sizes.heroIcon.value * 3).toInt())
    }

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_title),
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
            Spacer(modifier = Modifier.height(Spacing.xl))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AppAvatar(
                    name = uiState.nickname,
                    path = uiState.previewAvatarPath,
                    preset = uiState.previewAvatarPreset,
                    size = Sizes.heroIcon,
                    imageOverride = pickedImage
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Text(
                text = stringResource(R.string.profile_avatar_preset),
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(R.string.profile_avatar_preset_desc),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            PresetAvatarPicker(
                selected = uiState.previewAvatarPreset,
                onPick = onPickPreset
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 上传的图与内置头像共用一个位置：有图时按钮写「更换」，没有时写「上传」
            AppButton(
                text = stringResource(
                    if (uiState.hasUploadedAvatar) {
                        R.string.profile_change_avatar
                    } else {
                        R.string.profile_upload_avatar
                    }
                ),
                onClick = onPickAvatar,
                variant = AppButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            AppTextField(
                value = uiState.nickname,
                onValueChange = onNicknameChange,
                label = stringResource(R.string.profile_nickname),
                placeholder = stringResource(R.string.profile_nickname_placeholder),
                supportingText = stringResource(R.string.profile_nickname_hint),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            AppTextField(
                value = uiState.signature,
                onValueChange = onSignatureChange,
                label = stringResource(R.string.profile_signature),
                placeholder = stringResource(R.string.profile_signature_placeholder),
                supportingText = stringResource(R.string.profile_signature_hint),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            AppButton(
                text = stringResource(R.string.common_save),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 内置头像选择器：四列，选中的那个外面套一圈描边。
 *
 * 这里没有「不设头像」这一项——拿掉头像这件事本身已经取消了。不挑内置头像、也不上传图片时，
 * 头像就是昵称的第一个字（见 AppAvatar），空圈不会出现在任何地方。
 */
@Composable
private fun PresetAvatarPicker(
    selected: AvatarPreset?,
    onPick: (AvatarPreset) -> Unit
) {
    val colors = AppTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        AvatarPreset.entries.chunked(PRESET_COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                row.forEach { preset ->
                    val isSelected = preset == selected
                    val label = stringResource(
                        R.string.profile_avatar_preset_option,
                        preset.ordinal + 1
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onPick(preset) }
                            )
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center
                    ) {
                        // 描边套在头像外面，中间空出一点：贴着画会啃掉图案的边缘
                        Box(
                            modifier = Modifier
                                .size(Sizes.avatarMd)
                                .clip(CircleShape)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(
                                            width = Sizes.editStroke,
                                            color = colors.textPrimary,
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(if (isSelected) Spacing.xxs else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            PresetAvatar(
                                preset = preset,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 最后一行不满时补齐空位，铺满的那几行才不会被 weight 撑大
                repeat(PRESET_COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200, name = "个人资料")
@Composable
private fun ProfilePreview() {
    LifeLedgerTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                nickname = "追光者",
                signature = "每一个小小的坚持，都在构建更好的你",
                savedAvatarPreset = AvatarPreset.MOUNTAIN,
                isLoaded = true
            ),
            onBack = {},
            onNicknameChange = {},
            onSignatureChange = {},
            onPickAvatar = {},
            onPickPreset = {},
            onSave = {}
        )
    }
}
