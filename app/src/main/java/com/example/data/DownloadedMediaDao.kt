package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedMediaDao {

    @Query("SELECT * FROM downloaded_media ORDER BY downloadTimestamp DESC")
    fun getAllDownloads(): Flow<List<DownloadedMediaEntity>>

    @Query("SELECT * FROM downloaded_media WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: String): DownloadedMediaEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_media WHERE id = :id)")
    suspend fun isAlreadyDownloaded(id: String): Boolean

    @Query("SELECT * FROM downloaded_media WHERE mediaId = :mediaId")
    suspend fun getDownloadsForMedia(mediaId: String): List<DownloadedMediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(media: DownloadedMediaEntity)

    @Delete
    suspend fun deleteDownload(media: DownloadedMediaEntity)

    @Query("DELETE FROM downloaded_media WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM downloaded_media")
    fun getDownloadCount(): Flow<Int>
}
