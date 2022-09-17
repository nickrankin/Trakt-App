package com.nickrankin.traktapp.repo.stats

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.auth.AuthDatabase
import com.nickrankin.traktapp.dao.auth.model.Stats
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.stats.model.*
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

private const val REFRESH_INTERVAL = 24L
private const val TAG = "StatsRepository"

class StatsRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val moviesDatabase: MoviesDatabase,
    private val showsDatabase: ShowsDatabase,
private val authDatabase: AuthDatabase) {

    private val userStatsDao = authDatabase.userStatsDao()










    suspend fun refreshUserStats() {
        val userStats = traktApi.tmUsers().stats(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null") ?: "null"))

        Log.d(TAG, "getUserStats: Refreshing user stats")

        authDatabase.withTransaction {
            userStatsDao.insertUserStats(
                Stats(
                    1,
                    userStats.movies.collected,
                    userStats.movies.plays,
                    userStats.movies.watched,
                    userStats.movies.minutes,
                    userStats.shows.collected,
                    userStats.episodes.plays,
                    userStats.shows.watched,
                    userStats.episodes.minutes
                )
            )
        }
    }







}