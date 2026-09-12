package com.Anchored.mylife.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

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
            avatarPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onRemoveAvatar = viewModel::removeAvatar,
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
    onRemoveAvatar: () -> Unit,
    onSave: () -> Unit
) {
    val colors = AppTheme.colors
    val pickedImage = uiState.pickedUri?.let { uri ->
        rememberUriThumbnail(uri = uri, sizePx = (Sizes.heroIcon.value * 3).toInt())
    }

    Scaffold(
        containerColor = colors.background,
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
                    size = Sizes.heroIcon,
                    imageOverride = pickedImage
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                AppButton(
                    text = stringResource(R.string.profile_change_avatar),
                    onClick = onPickAvatar,
                    variant = AppButtonVariant.Secondary
                )
                if (uiState.hasAvatar) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    AppButton(
                        text = stringResource(R.string.profile_remove_avatar),
                        onClick = onRemoveAvatar,
                        variant = AppButtonVariant.Text
                    )
                }
            }

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
