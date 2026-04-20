package com.auramusic.app.voice.wakeword

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.auramusic.app.R
import com.auramusic.app.voice.VoiceCommandManager
import com.auramusic.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WakeWordService : Service() {

    @Inject
    lateinit var wakeWordDetector: VoskWakeWordDetector

    @Inject
    lateinit var voiceCommandManager: VoiceCommandManager

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "wake_word_channel"
        private const val CHANNEL_NAME = "Wake Word Detection"
        
        // Progress notification constants
        private const val PROGRESS_NOTIFICATION_ID = 1002
        
        @Volatile
        var instance: WakeWordService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        // Must call startForeground() immediately in onCreate() when started via
        // startForegroundService() to avoid ForegroundServiceDidNotStartInTimeException
        startForeground(NOTIFICATION_ID, buildNotification())
        
        wakeWordDetector.setOnWakeWordDetectedListener {
            android.util.Log.d("WakeWordService", "Wake word callback fired, forwarding to VoiceCommandManager")
            voiceCommandManager.onWakeWordDetected()
        }
        
        wakeWordDetector.setOnProgressListener { progress, bytesRead, totalBytes ->
            updateProgressNotification(progress, bytesRead, totalBytes)
        }
        
        wakeWordDetector.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        wakeWordDetector.start()
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        wakeWordDetector.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun updateProgress(progress: Int, bytesRead: Long, totalBytes: Long) {
        updateProgressNotification(progress, bytesRead, totalBytes)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aura is listening")
            .setContentText("Say 'Hey Aura' or 'Hello Aura' to activate")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildProgressNotification(progress: Int, bytesRead: Long, totalBytes: Long): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mbRead = bytesRead / (1024 * 1024)
        val mbTotal = totalBytes / (1024 * 1024)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading wake word model")
            .setContentText("$mbRead MB of $mbTotal MB ($progress%)")
            .setSmallIcon(R.drawable.download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()
    }

    private fun updateProgressNotification(progress: Int, bytesRead: Long, totalBytes: Long) {
        if (progress < 100) {
            val notification = buildProgressNotification(progress, bytesRead, totalBytes)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(PROGRESS_NOTIFICATION_ID, notification)
        } else {
            // Clear progress notification when done
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(PROGRESS_NOTIFICATION_ID)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps wake word detection active in background"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
