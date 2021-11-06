package com.nickrankin.traktapp.repo.shows.watched

import android.content.SharedPreferences
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.UserSlug
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WatchedEpisodesRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences, private val showsDatabase: ShowsDatabase) {
    @OptIn(ExperimentalPagingApi::class)

    fun watchedEpisodes(shouldRefresh: Boolean) = Pager(
        config = PagingConfig(8),
        remoteMediator = WatchedEpisodesRemoteMediator(traktApi, shouldRefresh, showsDatabase, sharedPreferences)
    ) {
        showsDatabase.watchedEpisodesDao().getWatchedEpisodes()
    }.flow
}