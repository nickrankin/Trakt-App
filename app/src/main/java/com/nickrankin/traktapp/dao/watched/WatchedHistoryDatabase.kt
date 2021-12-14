package com.nickrankin.traktapp.dao.watched

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.calendars.ShowCalendarEntryDao
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.dao.show.model.*

@Database(
    entities = [
        WatchedEpisode::class
],
    version = 1,
    exportSchema = false
)
@TypeConverters(ShowTypeConverter::class, TmdbShowTypeConverter::class)
abstract class WatchedHistoryDatabase : RoomDatabase() {
    abstract fun watchedHistoryShowsDao(): WatchedHistoryShowsDao

    companion object {
        @Volatile
        private var INSTANCE: WatchedHistoryDatabase? = null

        fun getDatabase(context: Context): WatchedHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    WatchedHistoryDatabase::class.java,
                    "watched_history"
                ).build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}