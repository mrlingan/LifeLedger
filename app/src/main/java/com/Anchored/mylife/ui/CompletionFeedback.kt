package com.Anchored.mylife.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.Anchored.mylife.R

/**
 * 「完成一件成就」时的触感与音效。
 *
 * 刻意不用 Vibrator 服务：`View.performHapticFeedback` 不需要 VIBRATE 权限，
 * 而且会自动遵守系统的触感开关——用户可以一键关掉，应用不用自己判断。
 * 音效同理，跟随系统的「触摸提示音」设置。
 *
 * 返回的是一个函数，在"这条成就从没完成变成完成"的那一刻调用一次；
 * 取消完成不响，避免把日常操作变得吵闹。
 */
@Composable
fun rememberCompletionFeedback(): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current

    return remember(context, view) {
        {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API 30+ 有专门的"确认"触感，比长按更轻、更短
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            )
            CompletionSound.play(context)
        }
    }
}

/**
 * 完成音效。
 *
 * SoundPool 只加载一小段 wav，第一次调用时异步 load，加载好之后常驻，
 * 之后每次完成都只是 play 一下，没有解码和 IO 开销。
 */
private object CompletionSound {

    private const val VOLUME = 0.8f

    private var pool: SoundPool? = null
    private var soundId = 0
    private var loaded = false

    /** 声音还没加载完就先记下来，加载完立刻补放一次 */
    private var playWhenLoaded = false

    @Synchronized
    fun play(context: Context) {
        if (!soundEffectsEnabled(context)) return

        val soundPool = pool ?: create(context)
        if (loaded) {
            soundPool.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
        } else {
            playWhenLoaded = true
        }
    }

    private fun create(context: Context): SoundPool {
        val soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        soundPool.setOnLoadCompleteListener { pool, _, status ->
            loaded = status == 0
            if (loaded && playWhenLoaded) {
                playWhenLoaded = false
                pool.play(soundId, VOLUME, VOLUME, 1, 0, 1f)
            }
        }

        soundId = soundPool.load(context.applicationContext, R.raw.complete, 1)
        pool = soundPool
        return soundPool
    }

    /** 跟随系统的「触摸提示音」开关；读不到就当作开着 */
    private fun soundEffectsEnabled(context: Context): Boolean = runCatching {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED
        ) == 1
    }.getOrDefault(true)
}
