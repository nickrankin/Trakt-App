package com.nickrankin.traktapp.repo.auth.shows

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.calendars.model.ShowCalendarEntry
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.helper.shouldRefreshContents
import com.uwetrottmann.trakt5.entities.CalendarShowEntry
import com.uwetrottmann.trakt5.enums.Status
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

private const val DEFAULT_TRAKT_DATE_FORMAT = "yyyy-MM-dd"
private const val REFRESH_INTERVAL = 24L
private const val NUM_DAYS = 14
class ShowsOverviewRepository @Inject constructor(private val traktApi: TraktApi, private val tmdbApi: TmdbApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase) {
    private val showCalendarEntryDao = showsDatabase.showCalendarentriesDao()

    suspend fun getMyShows(shouldRefresh: Boolean) = networkBoundResource(
        query = {showCalendarEntryDao.getShowCalendarEntries()},
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

    private fun convertEntries(entries: List<CalendarShowEntry>): List<ShowCalendarEntry> {
        val showCalendarEntries: MutableList<ShowCalendarEntry> = mutableListOf()

        entries.map { entry ->
            showCalendarEntries.add(
                ShowCalendarEntry(
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
                    entry.show?.title ?: ""

                )
            )

        }

        return showCalendarEntries

    }

    companion object {
        const val SHOWS_OVERVIEW_LAST_REFRESHED_KEY = "shows_overview_last_refreshed"
    }
}