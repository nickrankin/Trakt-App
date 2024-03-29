package com.nickrankin.traktapp.repo.auth.shows

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.calendars.model.HiddenShowCalendarEntry
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.calendars.model.ShowBaseCalendarEntry
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.uwetrottmann.trakt5.entities.CalendarShowEntry
import com.uwetrottmann.trakt5.enums.Status
import kotlinx.coroutines.flow.first
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val TAG = "ShowsOverviewRepository"
private const val DEFAULT_TRAKT_DATE_FORMAT = "yyyy-MM-dd"
private const val REFRESH_INTERVAL = 48L
private const val NUM_DAYS = 180
class ShowsOverviewRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase) {
    private val showCalendarEntryDao = showsDatabase.showCalendarentriesDao()

    suspend fun getMyShows(shouldRefresh: Boolean) = networkBoundResource(
        query = { showCalendarEntryDao.getShowCalendarEntries() },
        fetch = {
                traktApi.tmCalendars().myShows(OffsetDateTime.now().format(DateTimeFormatter.ofPattern(
                    DEFAULT_TRAKT_DATE_FORMAT)), NUM_DAYS)
        },
        shouldFetch = { calendarShowEntries ->
            shouldRefreshContents(sharedPreferences.getString(SHOWS_OVERVIEW_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL) || calendarShowEntries.isEmpty() || shouldRefresh
        },
        saveFetchResult = { calendarShowEntries ->

            sharedPreferences.edit()
                .putString(SHOWS_OVERVIEW_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                .apply()

            showsDatabase.withTransaction {
                showCalendarEntryDao.deleteShowCalendarEntries()
                showCalendarEntryDao.insert(
                    removeAlreadyAiredEpisodes(convertEntries(calendarShowEntries))
                )
            }
        }
    )

    suspend fun getLatestShows(shouldRefresh: Boolean) = networkBoundResource(
        query = { showCalendarEntryDao.getLatestShowCalendarEntries() },
        fetch = {
            traktApi.tmCalendars().myShows(OffsetDateTime.now().format(DateTimeFormatter.ofPattern(
                DEFAULT_TRAKT_DATE_FORMAT)), NUM_DAYS)
        },
        shouldFetch = { calendarShowEntries ->
            shouldRefreshContents(sharedPreferences.getString(SHOWS_OVERVIEW_LAST_REFRESHED_KEY, "") ?: "", REFRESH_INTERVAL) || calendarShowEntries.isEmpty() || shouldRefresh
        },
        saveFetchResult = { calendarShowEntries ->

            sharedPreferences.edit()
                .putString(SHOWS_OVERVIEW_LAST_REFRESHED_KEY, OffsetDateTime.now().toString())
                .apply()

            showsDatabase.withTransaction {
                showCalendarEntryDao.insert(convertEntries(calendarShowEntries))
            }
        }
    )

    suspend fun setShowHiddenState(showTmdbId: Int, isHidden: Boolean) {
        showsDatabase.withTransaction {
            showCalendarEntryDao.updateHiddenState(
                HiddenShowCalendarEntry(showTmdbId, isHidden)
            )
        }
    }

    fun removeAlreadyAiredEpisodes(shows: List<ShowBaseCalendarEntry>): List<ShowBaseCalendarEntry> {
        Log.e(TAG, "removeAlreadyAiredEpisodes: ${shows.size}")
        val dateNow = OffsetDateTime.now()
        val filteredShows: MutableList<ShowBaseCalendarEntry> = mutableListOf()

        shows.map { calendarEntry ->
            Log.d(TAG, "removeAlreadyAiredEpisodes: Entry ${calendarEntry.episode_title}. Aired ${calendarEntry.first_aired?.atZoneSameInstant(ZoneId.systemDefault())} Now ${dateNow?.atZoneSameInstant(ZoneId.systemDefault())}. Is before ${calendarEntry.first_aired?.isBefore(dateNow)}")
            if(calendarEntry.first_aired?.atZoneSameInstant(ZoneId.systemDefault())?.isBefore(dateNow?.atZoneSameInstant(ZoneId.systemDefault())) == false) {
                filteredShows.add(calendarEntry)
            }
        }

        return filteredShows
    }

    private fun convertEntries(entries: List<CalendarShowEntry>): List<ShowBaseCalendarEntry> {
        val showCalendarEntries: MutableList<ShowBaseCalendarEntry> = mutableListOf()

        entries.map { entry ->
            showCalendarEntries.add(
                ShowBaseCalendarEntry(
                    entry.episode?.ids?.trakt ?: 0,
                    entry.episode?.ids?.tmdb ?: 0,
                    entry.show?.language ?: "en",
                    entry.show?.ids?.trakt ?: 0,
                    entry.show?.ids?.tmdb ?: 0,
                    entry.first_aired,
                    entry.episode?.season ?: 0,
                    entry.episode?.number ?: 0,
                    entry.episode?.number_abs ?: 0,
                    entry.episode?.overview,
                    entry.episode?.runtime,
                    entry.episode?.title,
                    entry.show?.status ?: Status.CANCELED,
                    entry.show?.title ?: "",
                    false
                )
            )

        }

        return showCalendarEntries
    }

    suspend fun getHiddenStatus(showHidden: Boolean, showEntries: List<ShowBaseCalendarEntry>): Resource<List<ShowBaseCalendarEntry>> {
        val hiddenShows = showCalendarEntryDao.getShowHiddenStatus().first()

        showEntries.map { showCalendarEntry ->
            val foundShow = hiddenShows.find { showCalendarEntry.show_tmdb_id == it.showTmdbId }

            if(foundShow?.isHidden == true) {
                showCalendarEntry.hidden = true
            }
        }

        if(!showHidden) {
            return Resource.Success(showEntries.filter {
                !it.hidden
            })
        }

        return Resource.Success(showEntries)
    }

    companion object {
        const val SHOWS_OVERVIEW_LAST_REFRESHED_KEY = "shows_overview_last_refreshed"
    }
}