package com.Anchored.mylife.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Anchored.mylife.data.settings.AppSettings

/**
 * 每日提醒的接收者。
 *
 * 闹钟本身是"每天一次"的重复闹钟，周末也会到点，所以这里再按频率过滤一次：
 * 工作日模式碰到周末就直接返回，等下一个重复周期。
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = AppSettings(context)
        if (!settings.reminderEnabled.value) return
        if (!ReminderScheduler.shouldFireToday(settings.reminderFrequency.value)) return

        ReminderNotifications.show(context)
    }
}
