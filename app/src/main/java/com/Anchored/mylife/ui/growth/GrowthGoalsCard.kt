package com.Anchored.mylife.ui.growth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.Anchored.mylife.R
import com.Anchored.mylife.data.database.Goal
import com.Anchored.mylife.data.database.GoalTask
import com.Anchored.mylife.ui.components.AppButton
import com.Anchored.mylife.ui.components.AppButtonVariant
import com.Anchored.mylife.ui.components.AppCard
import com.Anchored.mylife.ui.components.AppCardTone
import com.Anchored.mylife.ui.components.AppProgressBar
import com.Anchored.mylife.ui.components.AppTextLink
import com.Anchored.mylife.ui.components.EmptyState
import com.Anchored.mylife.ui.components.SectionHeader
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Spacing
import com.Anchored.mylife.ui.xpText

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * 长期目标：一个大方向，拆成几个今天能做完的小任务。
 *
 * 目标本身不直接给分——**任务**做完才给（每完成一个，账上多一笔）。
 * 所以卡片里最显眼的是"几项里做完了几项"，不是日期：日期只是背景，
 * 真正让人往前走的是下一件小事。
 *
 * 目标一个一张卡：它们是彼此独立的事情，塞进同一张卡会变成一个看不出边界的清单。
 * 没有目标时给一个空状态和「创建目标」，不留一张空白卡片。
 */
@Composable
internal fun GrowthGoalsSection(
    goals: List<Goal>,
    tasks: List<GoalTask>,
    onCreateGoal: () -> Unit,
    onAddTask: (Long) -> Unit,
    onCompleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.growth_goals_title),
            action = {
                AppTextLink(
                    text = stringResource(R.string.growth_goals_new),
                    onClick = onCreateGoal,
                    showChevron = false
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (goals.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.growth_goals_empty_title),
                description = stringResource(R.string.growth_goals_empty_desc),
                action = {
                    AppButton(
                        text = stringResource(R.string.growth_goals_empty_action),
                        onClick = onCreateGoal
                    )
                }
            )
        } else {
            Column {
                goals.forEach { goal ->
                    GoalCard(
                        goal = goal,
                        tasks = tasks.filter { it.goalId == goal.id },
                        onAddTask = { onAddTask(goal.id) },
                        onCompleteTask = onCompleteTask
                    )
                    if (goal != goals.last()) Spacer(modifier = Modifier.height(Spacing.lg))
                }
            }
        }
    }
}

/** 一个目标：标题 + 说明 + 进度 + 剩下的天数 + 它下面的任务 */
@Composable
private fun GoalCard(
    goal: Goal,
    tasks: List<GoalTask>,
    onAddTask: () -> Unit,
    onCompleteTask: (Long) -> Unit
) {
    val colors = AppTheme.colors
    val done = tasks.count { it.isCompleted }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val daysLeft = ((goal.dueDate - System.currentTimeMillis()) / MILLIS_PER_DAY).coerceAtLeast(0)

    AppCard(tone = AppCardTone.Glass) {
        Text(
            text = goal.title,
            style = AppTheme.type.h3,
            color = colors.textPrimary
        )

        if (goal.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = goal.description,
                style = AppTheme.type.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppProgressBar(progress = progress, color = colors.accent)

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = stringResource(R.string.growth_goal_days, daysLeft, done, tasks.size),
            style = AppTheme.type.caption,
            color = colors.textTertiary
        )

        if (tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))

            Column {
                tasks.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (task.isCompleted) "✓" else "○"}  ${task.title}",
                            style = AppTheme.type.body,
                            color = if (task.isCompleted) colors.textTertiary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (!task.isCompleted) {
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            AppButton(
                                text = "+${xpText(task.reward)} ${stringResource(R.string.store_xp_unit)}",
                                onClick = { onCompleteTask(task.id) },
                                variant = AppButtonVariant.Text
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        AppButton(
            text = stringResource(R.string.growth_goal_add_task),
            onClick = onAddTask,
            variant = AppButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
