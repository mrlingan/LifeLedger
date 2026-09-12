package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 「记录新成就」的二选一底部弹层：从图鉴挑一条，或者自己写。
 *
 * 首页顶栏和「全部成就」页顶栏共用同一个入口，所以从 HomeScreen 里搬出来单独放。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddOptionsSheet(
    onDismiss: () -> Unit,
    onPickFromCodex: () -> Unit,
    onWriteCustom: () -> Unit
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
                text = stringResource(R.string.home_sheet_title),
                style = AppTheme.type.h3,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.home_sheet_desc),
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            AppCard(onClick = onPickFromCodex, tone = AppCardTone.Soft) {
                OptionRow(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.home_sheet_codex),
                    subtitle = stringResource(R.string.home_sheet_codex_desc)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            AppCard(onClick = onWriteCustom, tone = AppCardTone.Soft) {
                OptionRow(
                    icon = Icons.Outlined.Add,
                    title = stringResource(R.string.home_sheet_custom),
                    subtitle = stringResource(R.string.home_sheet_custom_desc)
                )
            }
        }
    }
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val colors = AppTheme.colors

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(Sizes.iconLg)
        )

        Spacer(modifier = Modifier.width(Spacing.md))

        Column {
            Text(
                text = title,
                style = AppTheme.type.bodyLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = subtitle,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }
    }
}
