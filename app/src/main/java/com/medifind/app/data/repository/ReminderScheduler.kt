package com.medifind.app.data.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.medifind.app.data.remote.ReminderAlarmReceiver
import java.util.Calendar

object ReminderScheduler {

    fun scheduleDosageAlarms(
        context: Context,
        reminderId: String,
        medicineName: String,
        dosageTimes: List<String>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // On API 31+, exact alarms require user permission via canScheduleExactAlarms().
        // If not granted, fall back to inexact alarms (may fire a few minutes late but still works).
        val canScheduleExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Below API 31, exact alarms don't need special permission
        }

        dosageTimes.forEachIndexed { index, time ->
            val parts = time.split(":")
            if (parts.size != 2) return@forEachIndexed

            val hour = parts[0].toIntOrNull() ?: return@forEachIndexed
            val minute = parts[1].toIntOrNull() ?: return@forEachIndexed

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val notificationId = (reminderId + index).hashCode()

            val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                putExtra("medicine_name", medicineName)
                putExtra("notification_id", notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // Fallback: inexact alarm, fires within a window around the target time
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelDosageAlarms(
        context: Context,
        reminderId: String,
        dosageTimes: List<String>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        dosageTimes.forEachIndexed { index, _ ->
            val notificationId = (reminderId + index).hashCode()
            val intent = Intent(context, ReminderAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}