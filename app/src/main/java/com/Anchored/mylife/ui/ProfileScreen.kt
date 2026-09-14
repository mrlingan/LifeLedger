package com.Anchored.mylife.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.profile.AvatarPreset
import com.Anchored.mylife.data.profile.Gender
import com.Anchored.mylife.data.profile.MbtiType
import com.Anchored.mylife.ui.components.AppAvatar
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.components.AppSectionHeader
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.AppTopBarStyle
import com.Anchored.mylife.ui.components.LevelPill
import com.Anchored.mylife.ui.components.LocalBottomBarClearance
import com.Anchored.mylife.ui.components.PageBackdrop
import com.Anchored.mylife.ui.components.PresetAvatar
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/** 内置头像排四列：八个正好两行，什么屏幕上都不用横滑 */
private const val PRESET_COLUMNS = 4

/**
 * 资料页上的符号。
 *
 * 和设置页同一个规矩：用的是符号字体里那一组单线字形（几何图形、杂项符号），
 * 不是线性图标——它们更像账本上的记号，也不用为每个概念画一条矢量路径。
 * 挑字符时请留在这些区段里：落到 emoji 区会被彩色字体接管，符号就不再受 tint 管。
 */
private object ProfileGlyph {
    /** 「基础信息」这一段的段首 */
    const val Basic = "♙"
    const val Gender = "♂"
    const val Birthday = "♨"
    const val Mbti = "◎"
    const val Location = "◈"
}

/**
 * 个人资料。
 *
 * 一页回答两件事，从上到下就是这两段：
 *
 * 1. **我是谁**——头像、昵称、签名，都在最上面那张资料卡里，点一下开弹层改。
 *    这三样是"草稿 + 保存"（头像在保存前只落一个 uri，见 [ProfileViewModel]），
 *    所以改动集中在弹层里，不会一边填一边生效。
 * 2. **我的一些信息**——性别、MBTI 这些"基础信息"。它们点了就生效，没有保存这一步，
 *    和上面的资料卡刻意区别开：一个是编辑一份资料，一个是拨两个开关。
 *
 * 页面底色用 [PageBackdrop]：这一页的卡片也是玻璃，压在很淡的蓝光上才浮得起来。
 */
@Composable
fun ProfileRoute(
    navController: NavHostController,
    topLevel: Boolean = false,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val level by viewModel.level.collectAsStateWithLifecycle()

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.pickAvatar(uri)
    }

    ProfileScreen(
        uiState = uiState,
        level = level,
        onBack = if (topLevel) null else ({ navController.popBackStack() }),
        onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
        onNicknameChange = viewModel::setNickname,
        onSignatureChange = viewModel::setSignature,
        onPickAvatar = {
            avatarPicker.launchExternal(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onPickPreset = viewModel::pickPreset,
        onDiscardEdits = viewModel::discardEdits,
        onGenderChange = viewModel::setGender,
        onMbtiChange = viewModel::setMbti,
        onSave = { viewModel.save { if (!topLevel) navController.popBackStack() } }
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    level: Int,
    onBack: (() -> Unit)?,
    onOpenSettings: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onPickPreset: (AvatarPreset) -> Unit,
    onDiscardEdits: () -> Unit,
    onGenderChange: (Gender?) -> Unit,
    onMbtiChange: (MbtiType?) -> Unit,
    onSave: () -> Unit
) {
    val colors = AppTheme.colors
    val pickedImage = uiState.pickedUri?.let { uri ->
        rememberUriThumbnail(uri = uri, sizePx = (Sizes.heroIcon.value * 3).toInt())
    }

    // 三个弹层各自的开合都记在这一层：关掉弹层不该影响页面上的任何状态
    var editingProfile by remember { mutableStateOf(false) }
    var choosingGender by remember { mutableStateOf(false) }
    var choosingMbti by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        )
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            PageBackdrop()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    // 底栏是浮在内容上的：末尾再多留出它压住的高度
                    .padding(bottom = Spacing.xxl + LocalBottomBarClearance.current)
            ) {
                AppTopBar(
                    title = stringResource(R.string.profile_title),
                    style = AppTopBarStyle.Large,
                    subtitle = stringResource(R.string.profile_subtitle),
                    onBack = onBack,
                    // 大标题这一条是通栏的（它自己带左右留白），从资料卡往下才回到栏内
                    actions = {
                        AppIconButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            onClick = onOpenSettings,
                            tint = colors.textSecondary
                        )
                    }
                )

                ProfileCard(
                    uiState = uiState,
                    level = level,
                    imageOverride = pickedImage,
                    modifier = Modifier.padding(horizontal = Sizes.gutter),
                    onClick = { editingProfile = true }
                )

                BasicInfoCard(
                    gender = uiState.gender,
                    mbti = uiState.mbti,
                    onEditGender = { choosingGender = true },
                    onEditMbti = { choosingMbti = true },
                    modifier = Modifier
                        .padding(horizontal = Sizes.gutter)
                        .padding(top = Spacing.lg)
                )

                Text(
                    text = stringResource(R.string.profile_privacy_note),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(
                        start = Sizes.gutter + Spacing.xs,
                        end = Sizes.gutter + Spacing.xs,
                        top = Spacing.md
                    )
                )
            }
        }
    }

    if (editingProfile) {
        ProfileEditSheet(
            uiState = uiState,
            imageOverride = pickedImage,
            onNicknameChange = onNicknameChange,
            onSignatureChange = onSignatureChange,
            onPickAvatar = onPickAvatar,
            onPickPreset = onPickPreset,
            onDiscard = {
                onDiscardEdits()
                editingProfile = false
            },
            onSave = {
                // 先把弹层收掉再保存：保存成功会退出这一页，
                // 让弹层跟着一起被弹走会闪一下
                editingProfile = false
                onSave()
            },
            onDismiss = { editingProfile = false }
        )
    }

    if (choosingGender) {
        ProfileChoiceSheet(
            title = stringResource(R.string.profile_gender),
            hint = stringResource(R.string.profile_gender_hint),
            options = Gender.entries.toList(),
            selected = uiState.gender,
            labelOf = { gender -> gender?.let { stringResource(it.labelRes()) }.orEmpty() },
            onPick = onGenderChange,
            onDismiss = { choosingGender = false }
        )
    }

    if (choosingMbti) {
        ProfileChoiceSheet(
            title = stringResource(R.string.profile_mbti),
            hint = stringResource(R.string.profile_mbti_hint),
            // 第一项是「未设置」：MBTI 是自评，想撤回来得有个去处
            options = listOf<MbtiType?>(null) + MbtiType.entries,
            selected = uiState.mbti,
            labelOf = { mbti -> mbti?.name ?: stringResource(R.string.profile_unset) },
            onPick = onMbtiChange,
            onDismiss = { choosingMbti = false }
        )
    }
}

// ---------------------------------------------------------------------------
// 资料卡：头像 + 昵称 + 阶段 + 签名
// ---------------------------------------------------------------------------

/**
 * 资料卡。
 *
 * 整张卡可点，点开「编辑资料」弹层——所以卡上既有一支铅笔（改的就是这三样），
 * 也有一枚右箭头（右边还有一层），两者说的是同一件事。
 *
 * 显示的是**草稿**（[ProfileUiState.previewAvatarPath] 那一套）：在弹层里换一张头像，
 * 关掉弹层就能在这张卡上看到效果——它同时充当那个弹层的预览。
 */
@Composable
private fun ProfileCard(
    uiState: ProfileUiState,
    level: Int,
    imageOverride: ImageBitmap?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Glass,
        shape = RoundedCornerShape(Radius.xl),
        cornerRadius = Radius.xl,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppAvatar(
                name = uiState.nickname,
                path = uiState.previewAvatarPath,
                preset = uiState.previewAvatarPreset,
                size = Sizes.heroIcon,
                imageOverride = imageOverride
            )

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uiState.nickname.ifBlank { stringResource(R.string.my_default_name) },
                        style = AppTheme.type.h2,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        // 名字长的时候先挤名字：铅笔和阶段牌子要留在行里
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.profile_edit_action),
                        tint = colors.accent,
                        modifier = Modifier.size(Sizes.iconMd)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                LevelPill(level = level)

                Spacer(modifier = Modifier.height(Spacing.sm))

                Text(
                    text = uiState.signature.ifBlank { stringResource(R.string.my_default_bio) },
                    style = AppTheme.type.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                // 跟着强调色走：这是"整张卡可以进去"的提示，
                // 比设置行尾那些"这一项还有下一页"的箭头更值得被看见
                tint = colors.accent,
                modifier = Modifier.size(Sizes.iconLg)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 基础信息：性别 / 生日 / MBTI / 所在地
// ---------------------------------------------------------------------------

/**
 * 基础信息。
 *
 * 性别和 MBTI 是能改的：点了弹出一板标签，选中哪个当场生效（没有"保存"）。
 * 生日和所在地只占一个位置——功能还没做，按设置页的老规矩标「即将支持」并置灰，
 * 而不是点一下弹一句"暂未开放"：一个按不动但写明白了的行，比一个会弹提示的行更好懂。
 *
 * 卡片下面那句小字是有意留的：这几个值都只在本机，说清楚比让人猜要好。
 */
@Composable
private fun BasicInfoCard(
    gender: Gender?,
    mbti: MbtiType?,
    onEditGender: () -> Unit,
    onEditMbti: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        tone = AppCardTone.Glass,
        shape = RoundedCornerShape(Radius.xl),
        cornerRadius = Radius.xl,
        contentPadding = PaddingValues(0.dp)
    ) {
        AppSectionHeader(
            title = stringResource(R.string.profile_section_basic),
            glyph = ProfileGlyph.Basic,
            hint = stringResource(R.string.profile_section_basic_hint)
        )

        AppDivider()

        AppSettingRow(
            title = stringResource(R.string.profile_gender),
            leadingGlyph = ProfileGlyph.Gender,
            trailingText = gender?.let { stringResource(it.labelRes()) }
                ?: stringResource(R.string.profile_unset),
            showChevron = true,
            onClick = onEditGender
        )

        AppDivider()

        // 生日：位置先占着，功能还没做。按设置页的规矩标「即将支持」并置灰
        AppSettingRow(
            title = stringResource(R.string.profile_birthday),
            leadingGlyph = ProfileGlyph.Birthday,
            trailingText = stringResource(R.string.common_coming_soon),
            enabled = false
        )

        AppDivider()

        AppSettingRow(
            title = stringResource(R.string.profile_mbti),
            leadingGlyph = ProfileGlyph.Mbti,
            trailingText = mbti?.name ?: stringResource(R.string.profile_unset),
            showChevron = true,
            onClick = onEditMbti
        )

        AppDivider()

        // 所在地：同上，只占位
        AppSettingRow(
            title = stringResource(R.string.profile_location),
            leadingGlyph = ProfileGlyph.Location,
            trailingText = stringResource(R.string.common_coming_soon),
            enabled = false
        )
    }
}

// ---------------------------------------------------------------------------
// 编辑资料弹层
// ---------------------------------------------------------------------------

/**
 * 「编辑资料」弹层：头像、昵称、签名。
 *
 * 这三样走"草稿 + 保存"，所以弹层底部给出「取消」和「保存」两枚按钮：
 * 取消 = 丢掉草稿退回上一次保存的样子（磁盘上什么都没动过），保存 = 落盘并回上一页。
 *
 * 把弹层直接拖下去不算取消：草稿留着，下次打开还在——页面上那张卡片显示的正是草稿，
 * 看见的就是它。真正丢掉草稿的时刻是离开这一页，或者点「取消」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditSheet(
    uiState: ProfileUiState,
    imageOverride: ImageBitmap?,
    onNicknameChange: (String) -> Unit,
    onSignatureChange: (String) -> Unit,
    onPickAvatar: () -> Unit,
    onPickPreset: (AvatarPreset) -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = Radius.hero, topEnd = Radius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // 这里有两个输入框：键盘弹起来时整层往上让，否则正填着的那一行会被挡住
                .imePadding()
                .padding(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    bottom = Spacing.xxl
                )
        ) {
            Text(
                text = stringResource(R.string.profile_edit_title),
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.profile_edit_hint),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

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
                    imageOverride = imageOverride
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

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

            Spacer(modifier = Modifier.height(Spacing.xl))

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

            Spacer(modifier = Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                AppButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onDiscard,
                    variant = AppButtonVariant.Text,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = stringResource(R.string.common_save),
                    onClick = onSave,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 单选项弹层
// ---------------------------------------------------------------------------

/**
 * 单选项弹层：一板可点的标签，选中那个是任务色玻璃。
 *
 * 用的是成就设置里挑分类那款 [AppChip]，所以"选中"在这一页和别处是同一种材质。
 * 选了就生效、也没有「保存」——弹层留在这儿让人接着看，读到想要的那个再把它拖下去；
 * 底部那枚「完成」只是给不想拖动的人一个明确的出口。
 *
 * @param options 选项；可以含一项 null，表示「未设置」（用来把已经选过的值撤回去）
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> ProfileChoiceSheet(
    title: String,
    hint: String,
    options: List<T?>,
    selected: T?,
    labelOf: @Composable (T?) -> String,
    onPick: (T?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surfaceElevated,
        shape = RoundedCornerShape(topStart = Radius.hero, topEnd = Radius.hero)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Sizes.gutter,
                    end = Sizes.gutter,
                    bottom = Spacing.xxl
                )
        ) {
            Text(
                text = title,
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = hint,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // 换行用 FlowRow：选项长短差得多（MBTI 是四个字母，性别是一个字），
            // 按固定列数切行会缺一块
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                options.forEach { option ->
                    AppChip(
                        label = labelOf(option),
                        selected = option == selected,
                        onClick = { onPick(option) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            AppButton(
                text = stringResource(R.string.common_done),
                onClick = onDismiss,
                variant = AppButtonVariant.Text,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 内置头像选择器
// ---------------------------------------------------------------------------

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

@StringRes
private fun Gender.labelRes(): Int = when (this) {
    Gender.MALE -> R.string.profile_gender_male
    Gender.FEMALE -> R.string.profile_gender_female
    Gender.UNDISCLOSED -> R.string.profile_gender_undisclosed
}

// ---------------------------------------------------------------------------
// 预览
// ---------------------------------------------------------------------------

@Preview(showBackground = true, heightDp = 1000, name = "个人资料")
@Composable
private fun ProfilePreview() {
    LifeLedgerTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                nickname = "追光者",
                signature = "每一个小小的坚持，都在构建更好的你",
                savedAvatarPreset = AvatarPreset.MOUNTAIN,
                gender = Gender.MALE,
                mbti = MbtiType.INTP,
                isLoaded = true
            ),
            level = 4,
            onBack = {},
            onOpenSettings = {},
            onNicknameChange = {},
            onSignatureChange = {},
            onPickAvatar = {},
            onPickPreset = {},
            onDiscardEdits = {},
            onGenderChange = {},
            onMbtiChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "个人资料 · 新用户")
@Composable
private fun ProfileEmptyPreview() {
    LifeLedgerTheme {
        ProfileScreen(
            uiState = ProfileUiState(isLoaded = true),
            level = 1,
            onBack = {},
            onOpenSettings = {},
            onNicknameChange = {},
            onSignatureChange = {},
            onPickAvatar = {},
            onPickPreset = {},
            onDiscardEdits = {},
            onGenderChange = {},
            onMbtiChange = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000, name = "个人资料 · 深色")
@Composable
private fun ProfileDarkPreview() {
    LifeLedgerTheme(darkTheme = true) {
        ProfileScreen(
            uiState = ProfileUiState(
                nickname = "追光者",
                signature = "每一个小小的坚持，都在构建更好的你",
                savedAvatarPreset = AvatarPreset.SPARKLE,
                gender = Gender.UNDISCLOSED,
                isLoaded = true
            ),
            level = 12,
            onBack = {},
            onOpenSettings = {},
            onNicknameChange = {},
            onSignatureChange = {},
            onPickAvatar = {},
            onPickPreset = {},
            onDiscardEdits = {},
            onGenderChange = {},
            onMbtiChange = {},
            onSave = {}
        )
    }
}
