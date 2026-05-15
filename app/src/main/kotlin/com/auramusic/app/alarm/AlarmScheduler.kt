/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Schedules music alarms via the system AlarmManager. Uses
 * setAlarmClock() so the alarm survives Doze / app standby and shows
 * the system "next alarm" indicator.
 */
package com.auramusic.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.auramusic.app.constants.AlarmEnabledKey
import com.auramusic.app.constants.AlarmHourKey
import com.auramusic.app.constants.AlarmMinuteKey
import com.auramusic.app.constants.AlarmRepeatDailyKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_FIRE = "com.auramusic.app.alarm.ACTION_FIRE"
    const val ACTION_SNOOZE = "com.auramusic.app.alarm.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.auramusic.app.alarm.ACTION_DISMISS"
    const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    private const val REQUEST_CODE = 1042

    fun computeNextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun schedule(context: Context, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = firePendingIntent(context)
        val showPi = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            Intent(context, com.auramusic.app.MainActivity::class.java)
                .setAction(AlarmReceiver.ACTION_OPEN_ALARM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showPi), pi)
            }
            Timber.d("Alarm scheduled for %d", triggerAtMillis)
        } catch (e: SecurityException) {
            Timber.w(e, "Failed to schedule alarm")
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(firePendingIntent(context))
    }

    fun snooze(context: Context, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        schedule(context, triggerAt)
    }

    /**
     * Re-schedule based on the persisted alarm time (used by BootReceiver
     * and by [reschedule] when the user re-enables the alarm).
     */
    fun rescheduleFromSettings(context: Context) {
        val ds = context.dataStore
        val enabled = ds.get(AlarmEnabledKey, false)
        if (!enabled) {
            cancel(context)
            return
        }
        val hour = ds.get(AlarmHourKey, 7)
        val minute = ds.get(AlarmMinuteKey, 0)
        schedule(context, computeNextTrigger(hour, minute))
    }

    /** Called by [AlarmReceiver] after the alarm fires – re-arm if daily. */
    fun rescheduleNextDayIfNeeded(context: Context) {
        val ds = context.dataStore
        val repeats = ds.get(AlarmRepeatDailyKey, false)
        val enabled = ds.get(AlarmEnabledKey, false)
        if (!enabled) return
        if (repeats) {
            val hour = ds.get(AlarmHourKey, 7)
            val minute = ds.get(AlarmMinuteKey, 0)
            schedule(context, computeNextTrigger(hour, minute))
        } else {
            // One-shot: disable.
            runBlocking {
                ds.edit { it[AlarmEnabledKey] = false }
            }
        }
    }

    private fun firePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
