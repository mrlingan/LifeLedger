package com.Anchored.mylife.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp

/**
 * 手画的图标。
 *
 * 底栏的「图鉴」需要一个指南针，而 Compose 的核心图标包里没有：explore 在
 * material-icons-extended 里，为这一个图标把上千个矢量图带进安装包不划算
 * （release 又没开混淆，瘦不掉）。所以照 Material Symbols 的 explore 造型画一个，
 * 尺寸与线宽跟核心图标对齐：24dp 画布、内容落在 2..22 之间，颜色交给 Icon 的 tint。
 */
object AppIcons {

    /** 指南针：外圈 + 斜着的指针 */
    val Compass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // 外圈：两个同心圆，EvenOdd 把中间挖掉，得到一圈 2dp 宽的环
            addPath(
                pathData = PathData {
                    circle(centerX = 12f, centerY = 12f, radius = 10f)
                    circle(centerX = 12f, centerY = 12f, radius = 8f)
                },
                pathFillType = PathFillType.EvenOdd,
                fill = SolidColor(Color.Black)
            )

            // 指针：西南—东北斜着的平行四边形，两头正好抵住内圈
            addPath(
                pathData = PathData {
                    moveTo(6.5f, 17.5f)
                    lineTo(14.01f, 14.01f)
                    lineTo(17.5f, 6.5f)
                    lineTo(9.99f, 9.99f)
                    close()
                },
                fill = SolidColor(Color.Black)
            )
        }.build()
    }
}

/** 往路径里加一个圆：两段半圆弧拼出来 */
private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    moveTo(centerX - radius, centerY)
    arcTo(
        horizontalEllipseRadius = radius,
        verticalEllipseRadius = radius,
        theta = 0f,
        isMoreThanHalf = true,
        isPositiveArc = true,
        x1 = centerX + radius,
        y1 = centerY
    )
    arcTo(
        horizontalEllipseRadius = radius,
        verticalEllipseRadius = radius,
        theta = 0f,
        isMoreThanHalf = true,
        isPositiveArc = true,
        x1 = centerX - radius,
        y1 = centerY
    )
    close()
}
