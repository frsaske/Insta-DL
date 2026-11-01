package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadedMediaEntity::class], version = 1, exportSchema = false)
abstract class InstaDownloadDatabase : RoomDatabase() {
    abstract fun downloadedMediaDao(): DownloadedMediaDao

    companion object {
        @Volatile
        private var INSTANCE: InstaDownloadDatabase? = null

        fun getDatabase(context: Context): InstaDownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InstaDownloadDatabase::class.java,
                    "insta_download_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
