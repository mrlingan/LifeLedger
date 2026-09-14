package com.Anchored.mylife.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 分类选择：一串可点的标签 + 一个「新建」。
 *
 * 标签复用 [AppChip] 的选中样式（选中那块是任务色玻璃），
 * 和「成就设置 → 关注的分类」是同一套语言——同一件事在两处长得一样，用户不用重新认。
 *
 * 自动换行用 [FlowRow]：分类名长短差得多（内置的是"生活"这种两字词，
 * 用户自己起的可能是"三十五岁前要做到的事"），按固定列数切行会溢出。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerField(
    categories: List<String>,
    selected: String,
    labelOf: (String) -> String,
    onSelect: (String) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var creating by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            categories.forEach { category ->
                AppChip(
                    label = labelOf(category),
                    selected = category == selected,
                    onClick = {
                        // 再点一次选中的那个 = 取消分类
                        onSelect(if (category == selected) "" else category)
                    }
                )
            }

            AppChip(
                label = stringResource(R.string.category_new),
                selected = false,
                onClick = { creating = true }
            )
        }
    }

    if (creating) {
        CategoryNameDialog(
            title = stringResource(R.string.category_new_title),
            initial = "",
            onConfirm = { name ->
                creating = false
                onCreate(name)
            },
            onDismiss = { creating = false }
        )
    }
}

/**
 * 新建分类：就一个名字。
 *
 * 允许和已有的重名没有意义，所以重名直接当成"选中那个"处理——用户大概率
 * 是想用那个分类，只是没在列表里找到。
 */
@Composable
internal fun CategoryNameDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()

    AppDialog(
        title = title,
        onDismissRequest = onDismiss,
        onConfirm = { if (trimmed.isNotEmpty()) onConfirm(trimmed) },
        confirmText = stringResource(R.string.common_save),
        dismissText = stringResource(R.string.common_cancel),
        confirmEnabled = trimmed.isNotEmpty(),
        content = {
            Column {
                AppTextField(
                    value = name,
                    onValueChange = { name = it.take(CATEGORY_NAME_MAX) },
                    label = stringResource(R.string.category_name),
                    placeholder = stringResource(R.string.category_name_hint),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(Spacing.md))
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

/** 分类名不用太长：它要挤在首页五个圈下面那一行里 */
private const val CATEGORY_NAME_MAX = 8
