package com.Anchored.mylife.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.Anchored.mylife.ui.theme.AppTheme
import com.Anchored.mylife.ui.theme.Radius
import com.Anchored.mylife.ui.theme.Sizes
import com.Anchored.mylife.ui.theme.Spacing

/**
 * 提示条。
 *
 * 宿主沿用 Material 的 SnackbarHost（负责排队和计时），
 * 但外观完全自己画：深色圆角块，没有 Material 的默认样式。
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Box(
            modifier = Modifier
                .padding(horizontal = Sizes.gutter, vertical = Spacing.lg)
                .clip(RoundedCornerShape(Radius.md))
                .background(AppTheme.colors.textPrimary)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Text(
                text = data.visuals.message,
                style = AppTheme.type.body,
                color = AppTheme.colors.background
            )
        }
    }
}
