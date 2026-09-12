package com.Anchored.mylife.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.Anchored.mylife.data.settings.ReminderFrequency
import java.util.Calendar

/**
 * 每日提醒的排程。
 *
 * 用不精确的重复闹钟（setInexactRepeating）：不需要 SCHEDULE_EXACT_ALARM 权限，
 * 系统可能把它挪几分钟。对一个"今天记点什么"的提醒来说完全可以接受，
 * 换来的是不用向用户要敏感权限。
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 4301
    private const val ACTION_REMIND = "com.Anchored.mylife.action.DAILY_REMIND"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis(hour, minute),
                AlarmManager.INTERVAL_DAY,
                pending
            )
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, PendingIntent.FLAG_NO_CREATE) ?: return
        runCatching { alarmManager.cancel(pending) }
    }

    /** 下一次触发的时间：今天这个点还没到就今天，否则明天 */
    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** 频率是否允许今天提醒 */
    fun shouldFireToday(frequency: ReminderFrequency): Boolean = when (frequency) {
        ReminderFrequency.EVERY_DAY -> true
        ReminderFrequency.WEEKDAYS -> {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            day != Calendar.SATURDAY && day != Calendar.SUNDAY
        }
    }
}
