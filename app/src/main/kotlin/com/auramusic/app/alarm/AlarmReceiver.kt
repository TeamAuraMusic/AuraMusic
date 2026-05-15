/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Fires when an alarm matures. Posts a high-priority full-screen
 * notification that launches MainActivity (which queues and plays
 * the user-selected songs) and re-arms the next-day alarm if the
 * alarm is configured to repeat.
 */
package com.auramusic.app.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.auramusic.app.MainActivity
import com.auramusic.app.R
import com.auramusic.app.playback.MusicService
import com.auramusic.app.constants.AlarmEnabledKey
import com.auramusic.app.constants.AlarmHourKey
import com.auramusic.app.constants.AlarmMinuteKey
import com.auramusic.app.constants.AlarmSnoozeMinutesKey
import com.auramusic.app.constants.AlarmVolumeKey
import com.auramusic.app.utils.dataStore
import com.auramusic.app.utils.get
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_FIRE -> handleFire(context)
            AlarmScheduler.ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, 5)
                cancelNotification(context)
                AlarmScheduler.snooze(context, minutes)
            }
            AlarmScheduler.ACTION_DISMISS -> {
                cancelNotification(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> {
                AlarmScheduler.rescheduleFromSettings(context)
            }
        }
    }

    private fun handleFire(context: Context) {
        ensureChannel(context)
        applyAlarmVolume(context)
        showAlarmNotification(context)
        startAlarmPlayback(context)
        AlarmScheduler.rescheduleNextDayIfNeeded(context)

        // Disable the one-shot flag immediately so a force-stop doesn't
        // leave a stale "armed" alarm in the UI.
        if (!isRepeating(context)) {
            runBlocking {
                context.dataStore.edit { it[AlarmEnabledKey] = false }
            }
        }
    }

    private fun isRepeating(context: Context): Boolean =
        context.dataStore.get(com.auramusic.app.constants.AlarmRepeatDailyKey, false)

    /**
     * Start the music service in the foreground so the user-selected alarm
     * songs begin playing without requiring the app to be opened. This
     * relies on the AlarmManager-broadcast exemption for foreground-service
     * starts on Android 12+.
     */
    private fun startAlarmPlayback(context: Context) {
        try {
            val serviceIntent = Intent(context, MusicService::class.java)
                .setAction(MusicService.ACTION_PLAY_ALARM)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Timber.w(e, "Failed to start alarm playback service")
        }
    }

    private fun applyAlarmVolume(context: Context) {
        val ds = context.dataStore
        val volume = ds.get(AlarmVolumeKey, 0.85f)
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (max * volume).toInt().coerceIn(1, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (e: Exception) {
            Timber.w(e, "Failed to set alarm volume")
        }
    }

    private fun showAlarmNotification(context: Context) {
        val ds = context.dataStore
        val hour = ds.get(AlarmHourKey, 7)
        val minute = ds.get(AlarmMinuteKey, 0)
        val timeText = "%02d:%02d".format(hour, minute)
        val snoozeMinutes = ds.get(AlarmSnoozeMinutesKey, 5)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ALARM
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPi = PendingIntent.getActivity(
            context, 1100, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePi = PendingIntent.getBroadcast(
            context, 1101, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmScheduler.ACTION_DISMISS)
        val dismissPi = PendingIntent.getBroadcast(
            context, 1102, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.alarm)
            .setContentTitle(context.getString(R.string.alarm_title))
            .setContentText(context.getString(R.string.alarm_subtitle, timeText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(openPi, true)
            .setContentIntent(openPi)
            .addAction(
                R.drawable.bedtime,
                context.getString(R.string.alarm_snooze, snoozeMinutes),
                snoozePi,
            )
            .addAction(
                R.drawable.close,
                context.getString(R.string.alarm_dismiss),
                dismissPi,
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, notif)
    }

    private fun cancelNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_desc)
            enableVibration(true)
            enableLights(true)
            setBypassDnd(true)
        }
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "alarm"
        const val NOTIFICATION_ID = 4242
        const val ACTION_OPEN_ALARM = "com.auramusic.app.alarm.OPEN"
    }
}
