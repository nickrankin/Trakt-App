package com.nickrankin.traktapp.dao.show

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.show.model.*

@Database(
    entities = [
        WatchedEpisode::class,
        WatchedEpisodePageKey::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ShowTypeConverter::class, TmdbShowTypeConverter::class)
abstract class WatchedShowsMediatorDatabase : RoomDatabase() {


    companion object {
        @Volatile
        private var INSTANCE: WatchedShowsMediatorDatabase? = null

        fun getDatabase(context: Context): WatchedShowsMediatorDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchedShowsMediatorDatabase::class.java,
                    "watched_shows"
                ).build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}