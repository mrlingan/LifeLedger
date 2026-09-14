package com.Anchored.mylife.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.Anchored.mylife.R
import com.Anchored.mylife.data.settings.HomeSection
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppDivider
import com.Anchored.mylife.ui.components.AppSettingRow
import com.Anchored.mylife.ui.components.AppSwitch
import com.Anchored.mylife.ui.components.AppTopBar
import com.Anchored.mylife.ui.components.CategoryNameDialog
import com.Anchored.mylife.ui.components.ImageCropDialog
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.theme.CategoryColorChoices
import kotlin.math.floor

/**
 * 首页板块：决定首页显示哪些内容、按什么顺序。
 *
 * 只做开关和排序，不碰任何业务数据——关掉一个板块只是不显示，记录本身一条都不会少。
 */
@Composable
fun HomeLayoutRoute(
    navController: NavHostController,
    viewModel: HomeLayoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeLayoutScreen(
        uiState = uiState,
        homeImagePath = viewModel.homeImagePath.collectAsStateWithLifecycle().value,
        onBack = { navController.popBackStack() },
        onToggle = viewModel::toggle,
        onMove = viewModel::move,
        onShowAll = viewModel::showAllHidden,
        onReset = viewModel::reset,
        onCategoryColor = viewModel::setCategoryColor,
        onToggleHomeCategory = viewModel::toggleHomeCategory,
        onAddCategory = viewModel::addCategory,
        onRemoveCategory = viewModel::removeCategory,
        onSetHomeImage = viewModel::setHomeImage,
        onClearHomeImage = viewModel::clearHomeImage
    )
}

/**
 * 首页板块编辑。
 *
 * 一列到底，顺序就是首页的顺序，收起来的板块排在下面（灰掉）。
 * **长按任意一行进入编辑模式**：左边长出 ⊖ / ⊕（藏起来 / 放回去），
 * 右边的开关换成 ≡ 拖拽柄，按住上下拖就能调整先后——和 iOS 列表的编辑态一个路子。
 *
 * 为什么不用上/下箭头：五个板块要挪一格得点四次，还得先想清楚"上移"是谁的上移。
 * 直接拖到你想要的位置，一次到位；编辑态只在需要时出现，平时列表就是它最终的样子。
 *
 * 拖拽只发生在"要显示的那些"之间：隐藏的板块不参与排序（它们的顺序没有意义，
 * 放回来时追加在末尾），所以它们没有拖拽柄。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLayoutScreen(
    uiState: HomeLayoutUiState,
    homeImagePath: String? = null,
    onBack: () -> Unit,
    onToggle: (HomeSection) -> Unit,
    onMove: (Int, Int) -> Unit,
    onShowAll: () -> Unit,
    onReset: () -> Unit,
    onCategoryColor: (String, String?) -> Unit,
    onToggleHomeCategory: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onSetHomeImage: (android.graphics.Bitmap) -> Unit = {},
    onClearHomeImage: () -> Unit = {}
) {
    val colors = AppTheme.colors
    val visible = uiState.visible
    val hidden = uiState.hidden
    val presetTexts = rememberPresetTexts()

    var editing by remember { mutableStateOf(false) }
    // 正在挑颜色的分类（null = 没开挑色弹窗）
    var pickingColorFor by remember { mutableStateOf<String?>(null) }
    // 正在新建分类
    var creatingCategory by remember { mutableStateOf(false) }
    // 分类那一组默认收起来：十几个分类全铺开，这一页就没法看了
    var colorsExpanded by remember { mutableStateOf(false) }
    // 刚选中、还没裁剪的图
    var croppingUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) croppingUri = uri }

    // 裁剪框的比例 = 首页那条横幅的比例（屏幕宽度扣掉页面留白和卡片内边距）
    val configuration = LocalConfiguration.current
    val bannerAspect = remember(configuration.screenWidthDp) {
        val cardWidth = (
            configuration.screenWidthDp -
                Sizes.gutter.value * 2 -
                Spacing.lg.value * 2
            ).coerceAtLeast(Sizes.homeBanner.value)
        cardWidth / Sizes.homeBanner.value
    }

    // 拖拽状态：谁在被拖、拖了多远、以及这一列的几何（拖拽只跟这两个值算，不看实时测量）
    var dragging by remember { mutableStateOf<HomeSection?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragListTop by remember { mutableFloatStateOf(0f) }
    var dragRowHeight by remember { mutableFloatStateOf(0f) }

    // 每一行在卡片里的竖直位置：进编辑模式时用它反推整列的顶部和行高
    val rowSpans = remember { mutableStateMapOf<HomeSection, ClosedFloatingPointRange<Float>>() }

    fun endDrag() {
        dragging = null
        dragOffset = 0f
    }

    Scaffold(
        containerColor = AppTheme.pageColor,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.home_layout_title),
                onBack = onBack,
                actions = {
                    if (editing) {
                        AppButton(
                            text = stringResource(R.string.common_done),
                            onClick = {
                                editing = false
                                endDrag()
                            },
                            variant = AppButtonVariant.Text
                        )
                    }
                }
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
            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = stringResource(R.string.home_layout_intro),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            SettingsGroupLabel(
                text = stringResource(
                    if (editing) {
                        R.string.home_layout_editing_hint
                    } else {
                        R.string.home_layout_order
                    }
                )
            )

            AppCard(tone = AppCardTone.Glass, contentPadding = PaddingValues(0.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (visible.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_layout_none_visible),
                            style = AppTheme.type.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(Spacing.lg)
                        )
                    }

                    visible.forEachIndexed { index, section ->
                        if (index > 0) AppDivider()

                        val isDragging = dragging == section
                        val title = stringResource(section.titleRes())

                        SectionRow(
                            title = title,
                            subtitle = stringResource(section.descRes()),
                            dimmed = false,
                            modifier = Modifier
                                .onGloballyPositioned { coordinates ->
                                    val top = coordinates.positionInParent().y
                                    rowSpans[section] =
                                        top..(top + coordinates.size.height)
                                }
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationY = dragOffset
                                        val lift = 1.02f
                                        scaleX = lift
                                        scaleY = lift
                                    }
                                }
                                // 被拖的那一条要压在其他行上面，不然会被下半截盖住
                                .zIndex(if (isDragging) 1f else 0f)
                                .then(
                                    if (editing) {
                                        Modifier
                                    } else {
                                        Modifier.combinedClickable(
                                            onClick = { onToggle(section) },
                                            onLongClick = { editing = true }
                                        )
                                    }
                                ),
                            leading = if (editing) {
                                {
                                    CircleToggleControl(
                                        added = false,
                                        tint = colors.error,
                                        contentDescription = stringResource(
                                            R.string.home_layout_hide,
                                            title
                                        ),
                                        onClick = { onToggle(section) }
                                    )
                                }
                            } else {
                                null
                            },
                            trailing = if (editing) {
                                {
                                    DragHandle(
                                        onDragStart = {
                                            // 用"首行到末行"反推行距（含分隔线），比拿单行高度准：
                                            // 行与行之间还有一条 1dp 的分隔线，按行高算会越走越偏
                                            val first = visible.firstOrNull()
                                                ?.let { rowSpans[it]?.start }
                                            val last = visible.lastOrNull()
                                                ?.let { rowSpans[it]?.start }
                                            if (first != null && last != null && visible.size > 1) {
                                                dragListTop = first
                                                dragRowHeight =
                                                    (last - first) / (visible.size - 1)
                                                dragging = section
                                                dragOffset = 0f
                                            }
                                        },
                                        onDrag = { dy ->
                                            val moved = dragging ?: return@DragHandle
                                            val current = visible.indexOf(moved)
                                            if (current >= 0 && dragRowHeight > 0f) {
                                                val lastIndex = visible.lastIndex
                                                dragOffset = (
                                                    dragOffset + dy
                                                    ).coerceIn(
                                                    -current * dragRowHeight,
                                                    (lastIndex - current) * dragRowHeight
                                                )
                                                val center = dragListTop +
                                                    current * dragRowHeight +
                                                    dragRowHeight / 2f +
                                                    dragOffset
                                                val target = floor(
                                                    (center - dragListTop) / dragRowHeight
                                                ).toInt().coerceIn(0, lastIndex)
                                                if (target != current) {
                                                    onMove(current, target)
                                                    // 换位之后这一条的落点变了，把位移补回来，
                                                    // 手指底下那一条才不会跳
                                                    dragOffset -= (target - current) *
                                                        dragRowHeight
                                                }
                                            }
                                        },
                                        onDragEnd = ::endDrag
                                    )
                                }
                            } else {
                                {
                                    AppSwitch(
                                        checked = true,
                                        onCheckedChange = { onToggle(section) }
                                    )
                                }
                            }
                        )

                        // 「自定义图片」这一段：开关一开，图片选择就展开在它下面——
                        // 开关和图片本来就是同一件事的两半，分开摆只会让人
                        // 在两个地方之间来回找
                        if (section == HomeSection.CUSTOM_IMAGE && !editing) {
                            AppDivider()
                            CustomImagePicker(
                                path = homeImagePath,
                                onPick = {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                onRemove = onClearHomeImage
                            )
                        }

                        // 「核心数据」的子选项：分类颜色。它属于上面那一块，
                        // 所以挂在同一张卡里往下缩一档，而不是另起一页/另起一张卡
                        if (
                            section == HomeSection.OVERVIEW &&
                            !editing &&
                            uiState.categories.isNotEmpty()
                        ) {
                            AppDivider()
                            CategoryColorOption(
                                uiState = uiState,
                                presetTexts = presetTexts,
                                expanded = colorsExpanded,
                                onToggleExpanded = { colorsExpanded = !colorsExpanded },
                                onPickColor = { category -> pickingColorFor = category },
                                onToggleHomeCategory = onToggleHomeCategory,
                                onAddCategory = { creatingCategory = true }
                            )
                        }
                    }

                    if (hidden.isNotEmpty()) {
                        AppDivider()

                        Text(
                            text = stringResource(R.string.home_layout_hidden_hint),
                            style = AppTheme.type.caption,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(
                                start = Spacing.lg,
                                end = Spacing.lg,
                                top = Spacing.md,
                                bottom = Spacing.xs
                            )
                        )

                        hidden.forEach { section ->
                            val title = stringResource(section.titleRes())

                            SectionRow(
                                title = title,
                                subtitle = stringResource(section.descRes()),
                                dimmed = true,
                                modifier = if (editing) {
                                    Modifier
                                } else {
                                    Modifier.clickable { onToggle(section) }
                                },
                                leading = if (editing) {
                                    {
                                        CircleToggleControl(
                                            added = true,
                                            tint = colors.accent,
                                            contentDescription = stringResource(
                                                R.string.home_layout_show,
                                                title
                                            ),
                                            onClick = { onToggle(section) }
                                        )
                                    }
                                } else {
                                    null
                                },
                                // 隐藏的板块不参与排序，所以没有拖拽柄
                                trailing = if (editing) {
                                    null
                                } else {
                                    {
                                        AppSwitch(
                                            checked = false,
                                            onCheckedChange = { onToggle(section) }
                                        )
                                    }
                                }
                            )

                            // 板块收起来了，它的子选项跟着走——不然「核心数据」一关，
                            // 分类颜色就没地方改了（颜色管的是首页那几个圆环，和这一行在不在无关）。
                            // 编辑态不展开子项：这时整列都在挪位置，点开只会添乱
                            if (
                                section == HomeSection.OVERVIEW &&
                                !editing &&
                                uiState.categories.isNotEmpty()
                            ) {
                                AppDivider()
                                CategoryColorOption(
                                    uiState = uiState,
                                    presetTexts = presetTexts,
                                    expanded = colorsExpanded,
                                    onToggleExpanded = { colorsExpanded = !colorsExpanded },
                                    onPickColor = { category -> pickingColorFor = category },
                                    onToggleHomeCategory = onToggleHomeCategory,
                                    onAddCategory = { creatingCategory = true }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            Text(
                text = stringResource(
                    if (editing) {
                        R.string.home_layout_editing_note
                    } else {
                        R.string.home_layout_note
                    }
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )

            if (!uiState.isDefault) {
                Spacer(modifier = Modifier.height(Spacing.xl))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    // 两档按钮高度一样，但写清楚是居中：并排时不留"谁跟谁对齐"的疑问
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppButton(
                        text = stringResource(R.string.home_layout_show_all),
                        onClick = onShowAll,
                        variant = AppButtonVariant.Secondary
                    )
                    AppButton(
                        text = stringResource(R.string.home_layout_reset),
                        onClick = onReset,
                        variant = AppButtonVariant.Text
                    )
                }
            }

        }
    }

    val pickingCategory = pickingColorFor
    if (pickingCategory != null) {
        CategoryColorDialog(
            categoryLabel = presetTexts.categoryOf(pickingCategory),
            selected = uiState.categoryColors[pickingCategory],
            onPick = { color ->
                onCategoryColor(pickingCategory, color)
                pickingColorFor = null
            },
            onDelete = if (pickingCategory in uiState.customCategories) {
                {
                    onRemoveCategory(pickingCategory)
                    pickingColorFor = null
                }
            } else {
                null
            },
            onDismiss = { pickingColorFor = null }
        )
    }

    if (creatingCategory) {
        CategoryNameDialog(
            title = stringResource(R.string.category_new_title),
            initial = "",
            onConfirm = { name ->
                creatingCategory = false
                onAddCategory(name)
            },
            onDismiss = { creatingCategory = false }
        )
    }

    val cropSource = croppingUri
    if (cropSource != null) {
        ImageCropDialog(
            source = cropSource,
            // 裁剪框的比例 = 首页那条横幅的比例（卡片宽度扣掉左右留白）
            aspect = bannerAspect,
            onCropped = { bitmap ->
                croppingUri = null
                onSetHomeImage(bitmap)
            },
            onDismiss = { croppingUri = null }
        )
    }
}

/**
 * 「核心数据」下面挂着的子选项：分类颜色。
 *
 * 十几个分类全铺开，这一页就没法看了，所以先收成一行：左边缩一档表示从属关系，
 * 右边一句「首页显示几个」，点开才列出每个分类。它和「自定义图片」是同一个路子——
 * 属于上面那个板块的东西就长在它下面，不另起一张卡。
 */
@Composable
private fun CategoryColorOption(
    uiState: HomeLayoutUiState,
    presetTexts: PresetTextResolver,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onPickColor: (String) -> Unit,
    onToggleHomeCategory: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    val colors = AppTheme.colors
    // 子项统一缩一档：和父行之间靠这条竖线拉开层级
    val indent = Spacing.xxl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpanded)
            .padding(
                start = indent,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_category_colors),
                style = AppTheme.type.body,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = stringResource(
                    R.string.home_category_shown,
                    uiState.homeCategories.size,
                    HOME_CATEGORY_LIMIT
                ),
                style = AppTheme.type.caption,
                color = colors.textTertiary
            )
        }

        Icon(
            imageVector = if (expanded) {
                Icons.Outlined.KeyboardArrowDown
            } else {
                Icons.AutoMirrored.Outlined.KeyboardArrowRight
            },
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(Sizes.iconMd)
        )
    }

    if (!expanded) return

    Column(
        modifier = Modifier.padding(
            start = indent,
            end = Spacing.lg,
            bottom = Spacing.md
        )
    ) {
        Text(
            text = stringResource(R.string.home_category_colors_desc),
            style = AppTheme.type.bodySmall,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Text(
            text = stringResource(R.string.home_category_hint),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )
    }

    uiState.categories.forEach { category ->
        AppDivider()

        val custom = storedColorToColor(uiState.categoryColors[category])
        val shownOnHome = category in uiState.homeCategories
        val canAddMore = uiState.homeCategories.size < HOME_CATEGORY_LIMIT

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickColor(category) }
                .padding(
                    start = indent,
                    end = Spacing.lg,
                    top = Spacing.md,
                    bottom = Spacing.md
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.categorySwatch)
                    .clip(CircleShape)
                    .background(custom ?: colors.accent)
                    .border(Sizes.hairline, colors.border, CircleShape)
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Text(
                text = presetTexts.categoryOf(category),
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            AppSwitch(
                checked = shownOnHome,
                onCheckedChange = { onToggleHomeCategory(category) },
                // 已经满了五个时，剩下的开关点不动——但已经选中的那个还能关掉
                enabled = shownOnHome || canAddMore
            )
        }
    }

    AppDivider()

    AppSettingRow(
        title = stringResource(R.string.category_new_title),
        subtitle = stringResource(R.string.category_new_desc),
        showChevron = true,
        onClick = onAddCategory
    )
}

/**
 * 给一个分类挑圆环颜色。
 *
 * 挑完立刻生效、立刻关掉：这里没有"先预览再确认"的必要，
 * 首页就在这一步之后。
 */
@Composable
private fun CategoryColorDialog(
    categoryLabel: String,
    selected: String?,
    onPick: (String?) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors

    AppDialog(
        title = stringResource(R.string.home_category_color_title),
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        confirmText = stringResource(R.string.common_got_it),
        dismissText = null,
        content = {
            Column {
                Text(
                    text = stringResource(R.string.home_category_color_desc, categoryLabel),
                    style = AppTheme.type.body,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                CategoryColorChoices.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        row.forEach { color ->
                            val isSelected = selected == color.toStoredColor()
                            Box(
                                modifier = Modifier
                                    .size(Sizes.touchTarget)
                                    .clip(CircleShape)
                                    .clickable { onPick(color.toStoredColor()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(Sizes.categorySwatch)
                                        .clip(CircleShape)
                                        .background(color)
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
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                AppButton(
                    text = stringResource(R.string.home_category_color_default),
                    onClick = { onPick(null) },
                    variant = AppButtonVariant.Text
                )

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AppButton(
                        text = stringResource(R.string.category_delete),
                        onClick = onDelete,
                        variant = AppButtonVariant.Destructive
                    )
                }
            }
        }
    )
}

/** 首页那张自定义图片的小预览：和首页上的形状一样（一条窄横幅），所见即所得 */
@Composable
private fun CustomImagePicker(
    path: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = AppTheme.colors
    val thumbnail = path?.let { rememberMediaThumbnail(path = it, isVideo = false, sizePx = 600) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(
            text = stringResource(R.string.home_custom_image_hint),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // 预览就是首页上的形状（一条窄横幅），所见即所得
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.homeBanner)
                .clip(RoundedCornerShape(Radius.md))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = stringResource(R.string.home_image_empty),
                    style = AppTheme.type.caption,
                    color = colors.textTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppButton(
                text = stringResource(
                    if (path == null) {
                        R.string.home_image_upload
                    } else {
                        R.string.home_image_change
                    }
                ),
                onClick = onPick,
                variant = AppButtonVariant.Secondary
            )

            if (path != null) {
                AppButton(
                    text = stringResource(R.string.home_image_remove),
                    onClick = onRemove,
                    variant = AppButtonVariant.Text
                )
            }
        }
    }
}

/**
 * 一行。
 *
 * 普通态：标题 + 说明，右边一个开关，整行可点（点了就是开关）。
 * 编辑态：左边 ⊖ / ⊕，右边拖拽柄，整行不再响应点击——这是编辑，不是切换。
 */
@Composable
private fun SectionRow(
    title: String,
    subtitle: String,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = AppTheme.colors
    val titleColor = when {
        dimmed -> colors.textTertiary
        else -> colors.textPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = if (leading != null) Spacing.sm else Spacing.lg,
                end = if (trailing != null) Spacing.sm else Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(Spacing.xxs))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = subtitle,
                style = AppTheme.type.bodySmall,
                color = if (dimmed) colors.textTertiary else colors.textSecondary
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/**
 * 编辑态左边的 ⊖ / ⊕。
 *
 * 核心图标集里没有这两个符号（它们在 material-icons-extended 里），为一个控件
 * 引一整个图标库不划算，直接画：一个圈 + 一条横杠，⊕ 再加一条竖杠。
 * 圈是描边不是实底——这个界面的层级一向靠描边和表面色，不靠色块。
 *
 * 视觉只有 [Sizes.editControl]，点击区域仍然是整个 [Sizes.touchTarget]。
 */
@Composable
private fun CircleToggleControl(
    added: Boolean,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(Sizes.touchTarget)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(Sizes.editControl)
                .clip(CircleShape)
                .border(
                    width = Sizes.hairline,
                    color = tint.copy(alpha = 0.7f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = Sizes.editGlyph, height = Sizes.editStroke)
                    .background(tint)
            )
            if (added) {
                Box(
                    modifier = Modifier
                        .size(width = Sizes.editStroke, height = Sizes.editGlyph)
                        .background(tint)
                )
            }
        }
    }
}

/**
 * 编辑态右边的拖拽柄（≡）。
 *
 * 按住就拖，不用再长按一次——已经进编辑模式了，这里的意图没有歧义。
 * 拖动过程中把手势消费掉，外层那个纵向滚动不会跟着一起滚。
 */
@Composable
private fun DragHandle(
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val colors = AppTheme.colors
    // pointerInput 的 lambda 只在 key 变化时重启，回调得走 rememberUpdatedState 才是最新的
    val latestStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .size(Sizes.touchTarget)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { latestStart() },
                    onDragEnd = { latestEnd() },
                    onDragCancel = { latestEnd() },
                    onDrag = { change, amount ->
                        change.consume()
                        latestDrag(amount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(width = Sizes.editGlyph, height = Sizes.editStroke)
                        .background(colors.textTertiary)
                )
            }
        }
    }
}

/** 分组小标题：和设置页的分组标题同一个样式 */
@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = AppTheme.type.caption,
        color = AppTheme.colors.textTertiary,
        modifier = Modifier.padding(
            start = Spacing.xs,
            top = Spacing.xl,
            bottom = Spacing.sm
        )
    )
}

@StringRes
private fun HomeSection.titleRes(): Int = when (this) {
    HomeSection.LIFE_PROGRESS -> R.string.home_life_progress
    HomeSection.OVERVIEW -> R.string.home_stats
    HomeSection.CATEGORIES -> R.string.home_category_progress
    HomeSection.CUSTOM_IMAGE -> R.string.home_custom_image
    HomeSection.RECENT -> R.string.home_recent
    HomeSection.FOOTER -> R.string.home_section_footer
}

@StringRes
private fun HomeSection.descRes(): Int = when (this) {
    HomeSection.LIFE_PROGRESS -> R.string.home_section_life_desc
    HomeSection.OVERVIEW -> R.string.home_section_overview_desc
    HomeSection.CATEGORIES -> R.string.home_section_categories_desc
    HomeSection.CUSTOM_IMAGE -> R.string.home_custom_image_desc
    HomeSection.RECENT -> R.string.home_section_recent_desc
    HomeSection.FOOTER -> R.string.home_section_footer_desc
}

@Preview(showBackground = true, heightDp = 900, name = "首页板块")
@Composable
private fun HomeLayoutPreview() {
    LifeLedgerTheme {
        HomeLayoutScreen(
            uiState = HomeLayoutUiState(
                visible = listOf(
                    HomeSection.OVERVIEW,
                    HomeSection.RECENT,
                    HomeSection.LIFE_PROGRESS
                ),
                hidden = listOf(HomeSection.CATEGORIES, HomeSection.FOOTER),
                isDefault = false,
                categories = listOf("技能", "兴趣", "旅行", "成长", "生活")
            ),
            onBack = {},
            onToggle = {},
            onMove = { _, _ -> },
            onShowAll = {},
            onReset = {},
            onCategoryColor = { _, _ -> },
            onToggleHomeCategory = {},
            onAddCategory = {},
            onRemoveCategory = {}
        )
    }
}
