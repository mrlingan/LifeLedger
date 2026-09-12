package com.Anchored.mylife.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.Anchored.mylife.data.settings.AppSettings

/** 重启之后系统会清掉所有闹钟，这里按用户的设置重新排一次 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val settings = AppSettings(context)
        if (settings.reminderEnabled.value) {
            ReminderScheduler.schedule(
                context = context,
                hour = settings.reminderHour.value,
                minute = settings.reminderMinute.value
            )
        }
    }
}
