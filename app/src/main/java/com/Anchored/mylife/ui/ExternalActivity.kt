package com.Anchored.mylife.ui

import androidx.activity.result.ActivityResultLauncher

/**
 * 应用自己拉起的外部界面（相册、文件选择）的标记。
 *
 * 应用锁的规矩是"离开前台就锁"。但用户去相册挑张图、挑完回来还要重新解锁一次，
 * 实在太烦，所以这些入口在 launch 之前打个标记，锁看到标记就这次不上锁。
 *
 * 标记用一次就清掉：回到前台（ON_START）时 [AppLockGate] 会调 [clear]，
 * 之后再切出去照样立刻锁。另外 [WINDOW_MILLIS] 是兜底 —— 万一某个 ROM
 * 不回调 ON_START，过期的标记也不会让锁一直敞着。
 */
object ExternalActivity {

    private const val WINDOW_MILLIS = 3 * 60_000L

    @Volatile
    private var launchedAt = 0L

    fun markLaunched() {
        launchedAt = System.currentTimeMillis()
    }

    fun isFresh(): Boolean =
        launchedAt > 0L && System.currentTimeMillis() - launchedAt < WINDOW_MILLIS

    fun clear() {
        launchedAt = 0L
    }
}

/** 拉起外部界面（相册 / 文件选择）前先打个标记，见 [ExternalActivity] */
fun <I> ActivityResultLauncher<I>.launchExternal(input: I) {
    ExternalActivity.markLaunched()
    launch(input)
}
