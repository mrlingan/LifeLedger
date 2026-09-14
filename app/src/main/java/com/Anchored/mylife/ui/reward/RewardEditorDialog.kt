package com.Anchored.mylife.ui.reward

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.RewardItem
import com.Anchored.mylife.data.reward.RewardCatalog
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppChip
import com.Anchored.mylife.ui.components.AppDialog
import com.Anchored.mylife.ui.components.AppTextField
import com.Anchored.mylife.ui.components.CategoryPickerField
import com.Anchored.mylife.ui.xpText
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 自定义奖励的编辑弹窗：名称 / 图标 / 描述 / 分类 / 积分价格。
 *
 * 只有自定义奖励进这里——内置那六条是样板，想改成自己的就自己加一条
 * （要删内置的也可以，在卡片上点进去就有）。
 *
 * 内容整体可滚动：五个字段加一行图标，在小屏（或者用户把字号调大）上会超过
 * 弹窗能给的高度，滚一下就能看全，不会被裁掉。
 */
@Composable
internal fun RewardEditorDialog(
    initial: RewardItem?,
    balance: Int,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, icon: String, category: String, price: Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.icon ?: RewardCatalog.FALLBACK_ICON) }
    var category by remember { mutableStateOf(initial?.category ?: RewardCatalog.CATEGORY_LIFE) }
    var price by remember { mutableStateOf((initial?.price ?: DEFAULT_PRICE).toString()) }

    val priceValue = price.toIntOrNull() ?: 0
    val valid = title.isNotBlank() && priceValue > 0

    // 分类标签在这里先取好：CategoryPickerField 的 labelOf 是一个普通回调，
    // 里面不能再调 @Composable 的 stringResource
    val categoryLabels = categories.map { key -> key to rewardCategoryLabel(key) }.toMap()

    AppDialog(
        title = stringResource(
            if (initial == null) R.string.store_editor_new else R.string.store_editor_edit
        ),
        onDismissRequest = onDismiss,
        onConfirm = {
            if (valid) {
                onSave(
                    title,
                    description,
                    icon,
                    category,
                    priceValue.coerceAtMost(RewardCatalog.PRICE_MAX)
                )
            }
        },
        confirmText = stringResource(R.string.common_save),
        dismissText = stringResource(R.string.common_cancel),
        confirmEnabled = valid
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            AppTextField(
                value = title,
                onValueChange = { title = it.take(TITLE_MAX) },
                label = stringResource(R.string.store_field_title),
                placeholder = stringResource(R.string.store_field_title_hint),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel(stringResource(R.string.store_field_icon))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                RewardEmojiChoices.forEach { emoji ->
                    AppChip(
                        label = emoji,
                        selected = emoji == icon,
                        onClick = { icon = emoji }
                    )
                }
            }

            AppTextField(
                value = description,
                onValueChange = { description = it.take(DESCRIPTION_MAX) },
                label = stringResource(R.string.store_field_desc),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            AppTextField(
                value = price,
                onValueChange = { input -> price = input.filter { it.isDigit() }.take(PRICE_DIGITS) },
                label = stringResource(R.string.store_field_price),
                supportingText = stringResource(
                    R.string.store_field_price_support,
                    xpText(balance)
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel(stringResource(R.string.store_field_category))
            CategoryPickerField(
                categories = categories,
                selected = category,
                labelOf = { key -> categoryLabels[key] ?: key },
                onSelect = { chosen -> if (chosen.isNotBlank()) category = chosen },
                onCreate = { created -> category = created }
            )

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                AppButton(
                    text = stringResource(R.string.store_delete_reward),
                    onClick = onDelete,
                    variant = AppButtonVariant.Destructive
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = AppTheme.type.caption,
        color = AppTheme.colors.textSecondary
    )
}

/** 新建时的默认价格：一杯咖啡的钱，改一个数字就能用 */
private const val DEFAULT_PRICE = 100

/** 名称别太长：两列卡片里一行放不下几个字 */
private const val TITLE_MAX = 20

private const val DESCRIPTION_MAX = 40

/** 价格位数：六位是一百万，配合 RewardCatalog.PRICE_MAX 一起挡住误输入 */
private const val PRICE_DIGITS = 6
