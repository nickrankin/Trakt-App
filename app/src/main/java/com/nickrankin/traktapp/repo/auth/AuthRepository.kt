package com.nickrankin.traktapp.repo.auth

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.ApiKeys
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.api.services.trakt.model.stats.UserStats
import com.nickrankin.traktapp.dao.auth.AuthDatabase
import com.nickrankin.traktapp.dao.auth.model.AuthUser
import com.nickrankin.traktapp.dao.auth.model.Stats
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.AccessToken
import com.uwetrottmann.trakt5.entities.AccessTokenRequest
import com.uwetrottmann.trakt5.entities.Settings
import com.uwetrottmann.trakt5.entities.UserSlug
import kotlinx.coroutines.flow.flow
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

private const val TAG = "AuthRepository"

class AuthRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val sharedPreferences: SharedPreferences,
    private val authDatabase: AuthDatabase
) {
    private val authUserDao = authDatabase.authUserDao()
    private val userStatsDao = authDatabase.userStatsDao()

    suspend fun exchangeCodeForAccessToken(code: String, isStaging: Boolean) = flow {
        emit(Resource.Loading())
        try {

            if(isStaging) {
                val accessToken = traktApi.tmAuth().exchangeCodeForAccessToken(
                    AccessTokenRequest(
                        code,
                        ApiKeys.STAGING_TRAKT_API_KEY,
                        ApiKeys.STAGING_TRAKT_API_SECRET, TraktApi.CALLBACK_URL
                    )
                )

                emit(Resource.Success(accessToken))
            } else {
                val accessToken = traktApi.tmAuth().exchangeCodeForAccessToken(
                    AccessTokenRequest(
                        code,
                        ApiKeys.TRAKT_API_KEY,
                        ApiKeys.TRAKT_API_SECRET, TraktApi.CALLBACK_URL
                    )
                )

                emit(Resource.Success(accessToken))
            }


        } catch (t: Throwable) {
            emit(Resource.Error(t, null))
        }
    }

    suspend fun getUserSlug() = flow {
        emit(Resource.Loading())
        try {
            val settings = traktApi.tmUsers().settings()


            emit(Resource.Success(settings.user?.ids?.slug))
        } catch (t: Throwable) {
            emit(Resource.Error(t, null))
        }
    }

    suspend fun getUserSettings(shouldRefresh: Boolean) = networkBoundResource(
        query = {
                val userSlug = sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "")
                authUserDao.getUser(userSlug!!)
        },
        fetch = {
                traktApi.tmUsers().settings()
        },
        shouldFetch = { user ->
                      shouldRefresh || user == null
        },
        saveFetchResult = { settings ->
            authDatabase.withTransaction {
                authUserDao.insertUser(getUser(settings))
            }
        }
    )

    private fun getUser(settings: Settings): AuthUser {
        return AuthUser(
            settings.user?.ids?.slug ?: "null",
            settings.user?.images?.avatar?.full,
            settings.account?.cover_image ?: "",
            settings.account?.timezone ?: "",
            settings.sharing_text?.watching,
            settings.sharing_text?.watched,
            settings.user?.about,
            settings.user?.age ?: 0,
            settings.user?.gender ?: "",
            settings.user?.isPrivate ?: false,
            settings.user?.joined_at ?: OffsetDateTime.now(),
            settings.user?.location ?: "",
            settings.user?.name ?: "",
            settings.user?.username ?: "",
            settings.user?.vip ?: false,
            settings.user?.vip_ep ?: false
        )
    }

    fun getUserStats(shouldRefresh: Boolean) = networkBoundResource(
        query = {
            userStatsDao.getUserStats()
        },
        fetch = {
            traktApi.tmUsers().stats(UserSlug(sharedPreferences.getString(AuthActivity.USER_SLUG_KEY, "null") ?: "null"))
        },
        shouldFetch = { stats ->
            stats == null || shouldRefresh
        },
        saveFetchResult = { userStats ->
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
    )
}