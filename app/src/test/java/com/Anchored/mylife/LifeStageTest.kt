package com.Anchored.mylife

import com.Anchored.mylife.ui.LEVEL_STEP
import com.Anchored.mylife.ui.levelOf
import com.Anchored.mylife.ui.levelStepProgress
import com.Anchored.mylife.ui.toNextLevelCount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 阶段（人生进度）的几个数。
 *
 * 首页、我的、成长、商城四处都从这几个函数取数，所以"刚好升一级"和"走满整个阶段"
 * 这两种边界必须稳定：0 条是 Lv.1、还差 5 条；5 条是 Lv.2 的起点，也要说还差 5 条。
 */
class LifeStageTest {

    @Test
    fun aFreshAccountStartsAtTheFirstStage() {
        assertEquals(1, levelOf(0))
        assertEquals(LEVEL_STEP, toNextLevelCount(0))
        assertEquals(0, levelStepProgress(0))
    }

    @Test
    fun reachingTheStepMovesUpAndTheCountResets() {
        assertEquals(2, levelOf(LEVEL_STEP))
        assertEquals(LEVEL_STEP, toNextLevelCount(LEVEL_STEP))
        assertEquals(0, levelStepProgress(LEVEL_STEP))
    }

    @Test
    fun progressAndTheRemainingCountAddUpToTheStep() {
        val completed = 16
        assertEquals(4, toNextLevelCount(completed))
        assertEquals(1, levelStepProgress(completed))
        assertEquals(
            LEVEL_STEP,
            toNextLevelCount(completed) + levelStepProgress(completed)
        )
    }
}
