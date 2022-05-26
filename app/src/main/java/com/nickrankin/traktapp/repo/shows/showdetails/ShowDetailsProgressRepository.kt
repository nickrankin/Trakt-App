package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.ui.auth.AuthActivity
import com.uwetrottmann.trakt5.entities.*
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.enums.ProgressLastActivity
import kotlinx.coroutines.channels.Channel
import org.threeten.bp.OffsetDateTime
import java.lang.Exception
import javax.inject.Inject

class ShowDetailsProgressRepository @Inject constructor(
    private val traktApi: TraktApi,
    private val showsDatabase: ShowsDatabase,
    private val sharedPreferences: SharedPreferences
) {

    val progressChannel = Channel<Resource<BaseShow>>()

}