package com.Anchored.mylife.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * 形状系统：由 [Radius] 推导，页面不要再自己写 `RoundedCornerShape(18.dp)`。
 *
 * 对应关系：
 * - extraSmall / small  → 小控件
 * - medium              → 普通容器
 * - large               → 普通卡片
 * - extraLarge          → Hero 卡片
 */
internal val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.sm),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.hero)
)
