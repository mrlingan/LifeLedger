package com.Anchored.mylife.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.Anchored.mylife.R
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppIcons
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.LifeLedgerTheme
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 快捷入口：写记录 / 目标 / 图鉴 / 成就。
 *
 * 四块一样宽的方块，图标压在一个浅色圆角方里，下面一行字。
 * 它们全是"从首页直接去做某件事"，不做二次展开——首页往下翻就是数据，
 * 想动手的时候不该再让人去找入口。
 *
 * 四个入口都指向已经存在的页面或弹层：写记录开的是「记录新成就」那个二选一，
 * 另外三个是底栏那几个 tab 的直达。这里没有新造任何流程。
 */
@Composable
internal fun QuickActionsSection(
    onWrite: () -> Unit,
    onGoals: () -> Unit,
    onCodex: () -> Unit,
    onAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        QuickActionTile(
            icon = Icons.Outlined.Edit,
            label = stringResource(R.string.home_quick_write),
            onClick = onWrite,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            icon = Icons.Outlined.Star,
            label = stringResource(R.string.home_quick_goal),
            onClick = onGoals,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            icon = AppIcons.Compass,
            label = stringResource(R.string.nav_codex),
            onClick = onCodex,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            icon = Icons.AutoMirrored.Outlined.List,
            label = stringResource(R.string.nav_achievements),
            onClick = onAchievements,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 一个方块：一枚浅底图标 + 一行标签。窄屏上四块并排，标签只留一行，放不下就省略 */
@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors

    AppCard(
        modifier = modifier,
        tone = AppCardTone.Glass,
        contentPadding = PaddingValues(horizontal = Spacing.xs, vertical = Spacing.md),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.homeQuickIcon)
                    .clip(RoundedCornerShape(Sizes.homeQuickRadius))
                    .background(colors.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(Sizes.iconMd)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = label,
                style = AppTheme.type.caption,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, name = "首页 · 快捷入口")
@Composable
private fun QuickActionsPreview() {
    LifeLedgerTheme {
        QuickActionsSection(
            onWrite = {},
            onGoals = {},
            onCodex = {},
            onAchievements = {}
        )
    }
}
