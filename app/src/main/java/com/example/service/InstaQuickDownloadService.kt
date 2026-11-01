package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.MainActivity
import com.example.data.DownloadedMediaEntity
import com.example.util.InstaDownloadManager
import com.example.util.InstagramScraperEngine
import com.example.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class InstaQuickDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val channelId = "instadl_quick_download_channel"
    private val notificationId = 8821

    private lateinit var notificationManager: NotificationManager
    private lateinit var scraperEngine: InstagramScraperEngine
    private lateinit var downloadManager: InstaDownloadManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        scraperEngine = InstagramScraperEngine(this)
        downloadManager = InstaDownloadManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification("Downloading Instagram media...", "Starting background download")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notificationId, initialNotification)
        }

        serviceScope.launch {
            try {
                updateNotification("Extracting stream...", url)
                val result = scraperEngine.fetchMedia(url)
                if (result.isSuccess) {
                    val media = result.getOrNull()!!
                    updateNotification("Downloading high-res media...", "@${media.author}")
                    
                    val results = downloadManager.downloadAll(media) { current, total ->
                        updateProgressNotification("Downloading slide $current of $total", current, total)
                    }

                    if (results.isNotEmpty()) {
                        showSuccess(results.first(), results.size)
                    } else {
                        showFailure("Failed to save media files")
                    }
                } else {
                    showFailure("Unable to parse post link")
                }
            } catch (e: Exception) {
                showFailure("Download error: ${e.message}")
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun showSuccess(firstItem: DownloadedMediaEntity, totalItems: Int) {
        val msg = if (totalItems > 1) {
            "$totalItems items saved to Gallery!"
        } else if (firstItem.isVideo) {
            "Reel saved to Gallery!"
        } else {
            "Photo saved to Gallery!"
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "InstaDL: $msg", Toast.LENGTH_SHORT).show()
            VibrationHelper.click(applicationContext, 25L)
        }

        val file = File(firstItem.localFilePath)
        val uri = try {
            FileProvider.getUriForFile(
                applicationContext,
                "${packageName}.fileprovider",
                file
            )
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, if (firstItem.isVideo) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            firstItem.id.hashCode(),
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completionNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete • @${firstItem.author}")
            .setContentText(msg)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(firstItem.id.hashCode(), completionNotification)
    }

    private fun showFailure(error: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "InstaDL: $error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNotification(title: String, text: String) {
        val notification = buildNotification(title, text)
        notificationManager.notify(notificationId, notification)
    }

    private fun updateProgressNotification(title: String, current: Int, total: Int) {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("InstaDL Background Download")
            .setContentText(title)
            .setProgress(total, current, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun buildNotification(title: String, text: String) =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setProgress(100, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Background Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress when sharing Instagram reels to InstaDL"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val EXTRA_URL = "extra_instagram_url"

        fun start(context: Context, url: String) {
            val intent = Intent(context, InstaQuickDownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
