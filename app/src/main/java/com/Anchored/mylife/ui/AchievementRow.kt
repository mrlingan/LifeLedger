package com.Anchored.mylife.ui

import com.Anchored.mylife.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.Anchored.mylife.data.database.Achievement
import com.Anchored.mylife.ui.components.AchievementCard
import com.Anchored.mylife.ui.components.AchievementStatus
import com.Anchored.mylife.ui.components.AppIconButton
import com.Anchored.mylife.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 成就列表项：左侧图标、中间标题与时间、右侧一键切换完成状态。
 *
 * 从 HomeScreen 里搬出来的，因为首页不再放列表，
 * 「全部成就」页要用同一套外观，两处必须是同一个实现。
 */
@Composable
internal fun AchievementRow(
    achievement: Achievement,
    onClick: () -> Unit,
    onToggleCompleted: () -> Unit
) {
    val colors = AppTheme.colors

    AchievementCard(
        title = achievement.title,
        description = achievement.description.takeIf { it.isNotBlank() },
        icon = {
            Text(
                text = achievement.iconEmoji,
                style = AppTheme.type.numberMedium
            )
        },
        status = if (achievement.isCompleted) {
            AchievementStatus.Completed
        } else {
            AchievementStatus.InProgress
        },
        meta = achievement.metaText(),
        onClick = onClick,
        trailing = {
            AppIconButton(
                icon = Icons.Outlined.CheckCircle,
                contentDescription = if (achievement.isCompleted) {
                    stringResource(R.string.detail_undo_complete)
                } else {
                    stringResource(R.string.detail_timeline_done)
                },
                onClick = onToggleCompleted,
                tint = if (achievement.isCompleted) colors.accent else colors.textTertiary
            )
        }
    )
}

/** 列表里的日期格式，全站统一 yyyy.MM.dd */
internal fun formatDay(millis: Long): String =
    SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(millis))

@Composable
internal fun Achievement.metaText(): String = if (isCompleted && completedDate != null) {
    stringResource(R.string.home_meta_completed, formatDay(completedDate))
} else {
    stringResource(R.string.home_meta_created, formatDay(createdDate))
}
