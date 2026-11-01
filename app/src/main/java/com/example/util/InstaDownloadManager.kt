package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.data.DownloadedMediaEntity
import com.example.data.InstaDownloadDatabase
import com.example.model.InstagramMediaMetadata
import com.example.model.MediaItemDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class InstaDownloadManager(private val context: Context) {

    private val tag = "InstaDL_Downloader"
    private val db = InstaDownloadDatabase.getDatabase(context).downloadedMediaDao()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "instadl_downloads"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "InstaDL Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Instagram media download completion"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun downloadItem(
        parentMeta: InstagramMediaMetadata,
        item: MediaItemDetail,
        onProgress: (Float) -> Unit = {}
    ): Result<DownloadedMediaEntity> = withContext(Dispatchers.IO) {
        val isVideo = item.isVideo
        val ext = if (isVideo) "mp4" else "jpg"
        val authorClean = parentMeta.author.replace("[^a-zA-Z0-9_]".toRegex(), "")
        val idClean = parentMeta.id.replace("[^a-zA-Z0-9_]".toRegex(), "")
        val fileName = "InstaDL_${authorClean}_${idClean}_${item.index + 1}.$ext"
        val uniqueEntityId = "InstaDL_${authorClean}_${idClean}_${item.index + 1}"

        // Check if already downloaded
        val existing = db.getDownloadById(uniqueEntityId)
        if (existing != null && File(existing.localFilePath).exists()) {
            return@withContext Result.success(existing)
        }

        // Determine target directory
        val baseDir = if (isVideo) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        }
        val targetFolder = File(baseDir, "InstaDL")
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        val targetFile = File(targetFolder, fileName)

        try {
            val request = Request.Builder()
                .url(item.url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code} downloading file"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty download response body"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalRead.toFloat() / contentLength).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            // Index in system gallery
            val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(mimeType)
            ) { path, uri ->
                Log.d(tag, "Scanned $path -> $uri")
            }

            val fileUri = Uri.fromFile(targetFile).toString()
            val entity = DownloadedMediaEntity(
                id = uniqueEntityId,
                mediaId = parentMeta.id,
                author = parentMeta.author,
                caption = parentMeta.caption,
                mediaType = if (isVideo) "video" else "image",
                localFilePath = targetFile.absolutePath,
                fileUri = fileUri,
                fileName = fileName,
                fileSizeBytes = targetFile.length(),
                thumbnailUrl = item.thumbnail.ifBlank { parentMeta.thumbnailUrl },
                downloadTimestamp = System.currentTimeMillis(),
                sourceUrl = item.url
            )

            db.insertDownload(entity)
            showCompletionNotification(entity)

            Result.success(entity)
        } catch (e: Exception) {
            Log.e(tag, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun downloadAll(
        parentMeta: InstagramMediaMetadata,
        onOverallProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<DownloadedMediaEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DownloadedMediaEntity>()
        val total = parentMeta.mediaItems.size
        parentMeta.mediaItems.forEachIndexed { index, item ->
            val res = downloadItem(parentMeta, item)
            if (res.isSuccess) {
                res.getOrNull()?.let { results.add(it) }
            }
            onOverallProgress(index + 1, total)
        }
        results
    }

    private fun showCompletionNotification(entity: DownloadedMediaEntity) {
        try {
            val file = File(entity.localFilePath)
            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, if (entity.isVideo) "video/*" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                entity.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete • @${entity.author}")
                .setContentText(entity.fileName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(entity.id.hashCode(), notification)
        } catch (e: Exception) {
            Log.w(tag, "Notification dispatch failed: ${e.message}")
        }
    }
}
