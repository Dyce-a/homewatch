package com.ilya.homewatch

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ilya.homewatch.data.IotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Отложенное включение/выключение устройства через AlarmManager —
 * сработает, даже если приложение закрыто, а часы спят.
 */
object DelayedActions {
    fun schedule(context: Context, deviceId: String, turnOn: Boolean, atMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context, deviceId, turnOn)
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (exactAllowed) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancel(context: Context, deviceId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, deviceId, turnOn = false))
    }

    private fun pendingIntent(context: Context, deviceId: String, turnOn: Boolean): PendingIntent {
        val intent = Intent(context, DelayedActionReceiver::class.java)
            .putExtra("device_id", deviceId)
            .putExtra("turn_on", turnOn)
        return PendingIntent.getBroadcast(
            context,
            deviceId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class DelayedActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val deviceId = intent.getStringExtra("device_id") ?: return
        val turnOn = intent.getBooleanExtra("turn_on", false)
        val token = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("access_token", null) ?: return
        context.getSharedPreferences("timers", Context.MODE_PRIVATE)
            .edit().remove(deviceId).apply()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                IotRepository().sendAction(token, deviceId, "devices.capabilities.on_off", "on", turnOn)
            } catch (_: Exception) {
            } finally {
                pending.finish()
            }
        }
    }
}
