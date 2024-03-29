package com.nickrankin.traktapp.dao.show

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nickrankin.traktapp.dao.calendars.ShowCalendarEntryDao
import com.nickrankin.traktapp.dao.calendars.model.HiddenShowCalendarEntry
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import com.nickrankin.traktapp.dao.history.EpisodeWatchedHistoryEntryDao
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import com.nickrankin.traktapp.dao.refresh.LastRefreshedAtDao
import com.nickrankin.traktapp.dao.show.model.*
import com.nickrankin.traktapp.dao.stats.*
import com.nickrankin.traktapp.dao.stats.model.*

@Database(
    entities = [ShowBaseCalendarEntry::class,
        CollectedShow::class,
        CollectedEpisode::class,
        WatchedEpisode::class,
        WatchedEpisodePageKey::class,
        TmShow::class,
        TmSeason::class,
        TmEpisode::class,
        TrackedShow::class,
        TrackedEpisode::class,
        WatchedShowsStats::class,
        WatchedSeasonStats::class,
        WatchedEpisodeStats::class,
        ShowsCollectedStats::class,
        EpisodesCollectedStats::class,
        RatingsShowsStats::class,
        RatingsEpisodesStats::class,
        HiddenShowCalendarEntry::class,
        LastRefreshedAt::class,
        EpisodeWatchedHistoryEntry::class,
               ShowsProgress::class,
               SeasonProgress::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ShowTypeConverter::class, TmdbShowTypeConverter::class)
abstract class ShowsDatabase : RoomDatabase() {
    abstract fun showCalendarentriesDao(): ShowCalendarEntryDao
    abstract fun collectedShowsDao(): CollectedShowDao
    abstract fun collectedEpisodeDao(): CollectedEpisodeDao
    abstract fun tmShowDao(): TmShowsDao
    abstract fun TmSeasonsDao(): TmSeasonsDao
    abstract fun TmEpisodesDao(): TmEpisodesDao
    abstract fun trackedShowDao(): TrackedShowDao
    abstract fun trackedEpisodeDao(): TrackedEpisodeDao

    abstract fun watchedEpisodesDao(): WatchedEpisodesDao
    abstract fun watchedEpisodePageKeyDao(): WatchedEpisodePageKeyDao

    abstract fun collectedShowsStatsDao(): CollectedShowsStatsDao
    abstract fun collectedEpisodesStatsDao(): EpisodesCollectedStatsDao
    abstract fun ratedShowsStatsDao(): RatingsShowsStatsDao
    abstract fun ratedEpisodesStatsDao(): RatingsEpisodesStatsDao
    abstract fun watchedShowsStatsDao(): WatchedShowsStatsDao
    abstract fun watchedSeasonStatsDao(): WatchedSeasonStatsDao
    abstract fun watchedEpisodesStatsDao(): WatchedEpisodesStatsDao

    abstract fun  lastRefreshedAtDao(): LastRefreshedAtDao

    abstract fun episodeWatchedHistoryEntryDao(): EpisodeWatchedHistoryEntryDao

    abstract fun showSeasonProgressDao(): ShowSeasonProgressDao


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