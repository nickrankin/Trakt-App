package com.nickrankin.traktapp.dao.show

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.calendars.ShowCalendarEntryDao
import com.nickrankin.traktapp.dao.calendars.model.HiddenShowCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.dao.stats.*
import com.nickrankin.traktapp.dao.stats.model.*

@Database(
    entities = [ShowCalendarEntry::class,
        CollectedShow::class,
        WatchedEpisode::class,
        WatchedEpisodePageKey::class,
        TmShow::class,
        TmSeason::class,
        TmEpisode::class,
        TrackedShow::class,
        TrackedEpisode::class,
        LastRefreshedShow::class,
        WatchedShowsStats::class,
        WatchedSeasonStats::class,
        WatchedEpisodeStats::class,
        CollectedShowsStats::class,
        RatingsShowsStats::class,
        RatingsEpisodesStats::class,
        HiddenShowCalendarEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ShowTypeConverter::class, TmdbShowTypeConverter::class)
abstract class ShowsDatabase : RoomDatabase() {
    abstract fun showCalendarentriesDao(): ShowCalendarEntryDao
    abstract fun collectedShowsDao(): CollectedShowDao
    abstract fun tmShowDao(): TmShowsDao
    abstract fun TmSeasonsDao(): TmSeasonsDao
    abstract fun TmEpisodesDao(): TmEpisodesDao
    abstract fun trackedShowDao(): TrackedShowDao
    abstract fun trackedEpisodeDao(): TrackedEpisodeDao
    abstract fun lastRefreshedShowDao(): LastRefreshedShowDao

    abstract fun watchedEpisodesDao(): WatchedEpisodesDao
    abstract fun watchedEpisodePageKeyDao(): WatchedEpisodePageKeyDao

    abstract fun collectedShowsStatsDao(): CollectedShowsStatsDao
    abstract fun ratedShowsStatsDao(): RatingsShowsStatsDao
    abstract fun ratedEpisodesStatsDao(): RatingsEpisodesStatsDao
    abstract fun watchedShowsStatsDao(): WatchedShowsStatsDao
    abstract fun watchedSeasonStatsDao(): WatchedSeasonStatsDao
    abstract fun watchedEpisodesStatsDao(): WatchedEpisodesStatsDao


    companion object {
        @Volatile
        private var INSTANCE: ShowsDatabase? = null

        fun getDatabase(context: Context): ShowsDatabase {
            return INSTANCE ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    ShowsDatabase::class.java,
                    "shows"
                ).build()

                INSTANCE = newInstance

                newInstance
            }
        }
    }
}