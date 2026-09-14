package com.Anchored.mylife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.Anchored.mylife.ui.theme.AppTheme

/**
 * 有"数据感"的那几页共用的底色：页面上缘两团很淡的蓝光，往下淡出。
 *
 * 首页和成长页用它——一个是今天的人生仪表盘，一个是攒了多少的账本，
 * 两页都是"看数据"的地方，站在同一个底色上才像同一套东西。
 *
 * 这是整页唯一一块"画上去的颜色"。它不参与布局、不吃点击，也不跟着列表滚——
 * 位置固定在屏幕顶部，所以往下翻的时候，光是留在原地慢慢被卡片盖住的，
 * 不会跟着内容跑。
 *
 * 为什么画在页面里而不是改主题底色：主题底色（[AppTheme.pageColor]）是所有页面
 * 共用的，只有这两页需要这个蓝调；用户设了背景图时页面底色是透明的，
 * 那时的光晕也只是压在壁纸上一层很淡的蓝，不会把图盖住。
 *
 * 透明度刻意压得很低（0.06 ~ 0.18）：它是"页面有一点偏蓝"，
 * 不是一块看得见的渐变，深色主题下同一条光晕同样成立。
 */
@Composable
internal fun PageBackdrop(modifier: Modifier = Modifier) {
    val accent = AppTheme.colors.accent

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 上缘整体偏蓝，到三分之一处就淡没了——它托的是页头与第一张卡片
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to accent.copy(alpha = 0.14f),
                    0.34f to accent.copy(alpha = 0.04f),
                    1f to Color.Transparent
                )
            )
        )

        // 左上角那团光最亮，正对着页头
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(width * 0.08f, height * 0.02f),
                radius = width * 0.75f
            ),
            radius = width * 0.75f,
            center = Offset(width * 0.08f, height * 0.02f)
        )

        // 右上角再来一团小一点的，页面顶上才不会一边亮一边平
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(width * 0.92f, height * 0.06f),
                radius = width * 0.6f
            ),
            radius = width * 0.6f,
            center = Offset(width * 0.92f, height * 0.06f)
        )
    }
}
