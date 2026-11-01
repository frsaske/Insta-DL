package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_media")
data class DownloadedMediaEntity(
    @PrimaryKey
    val id: String, // e.g. InstaDL_{username}_{id}_{index}
    val mediaId: String,
    val author: String,
    val caption: String,
    val mediaType: String, // "video" or "image"
    val localFilePath: String,
    val fileUri: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val thumbnailUrl: String,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val sourceUrl: String
) {
    val isVideo: Boolean get() = mediaType.equals("video", ignoreCase = true)
}
