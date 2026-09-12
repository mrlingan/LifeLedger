package com.Anchored.mylife.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.Anchored.mylife.data.reminder.ReminderScheduler
import com.Anchored.mylife.data.repository.RepositoryProvider
import com.Anchored.mylife.data.settings.ReminderFrequency
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReminderUiState(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    val frequency: ReminderFrequency = ReminderFrequency.EVERY_DAY
)

/**
 * 每日提醒。
 *
 * 设置一变就重排闹钟，界面层不留状态：设置页、开机广播、提醒本身
 * 都直接读同一份 SharedPreferences，不存在两份真相。
 */
class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = RepositoryProvider.get(application).settings

    val uiState: StateFlow<ReminderUiState> = combine(
        settings.reminderEnabled,
        settings.reminderHour,
        settings.reminderMinute,
        settings.reminderFrequency
    ) { enabled, hour, minute, frequency ->
        ReminderUiState(enabled = enabled, hour = hour, minute = minute, frequency = frequency)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReminderUiState()
    )

    fun setEnabled(enabled: Boolean) {
        settings.setReminderEnabled(enabled)
        if (enabled) {
            ReminderScheduler.schedule(
                context = getApplication(),
                hour = settings.reminderHour.value,
                minute = settings.reminderMinute.value
            )
        } else {
            ReminderScheduler.cancel(getApplication())
        }
    }

    fun setTime(hour: Int, minute: Int) {
        settings.setReminderTime(hour, minute)
        if (settings.reminderEnabled.value) {
            ReminderScheduler.schedule(context = getApplication(), hour = hour, minute = minute)
        }
    }

    fun setFrequency(value: ReminderFrequency) = settings.setReminderFrequency(value)
}
