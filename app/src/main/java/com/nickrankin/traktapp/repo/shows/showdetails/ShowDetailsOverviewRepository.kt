package com.nickrankin.traktapp.repo.shows.showdetails

import android.content.SharedPreferences
import androidx.room.withTransaction
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.dao.watched.WatchedHistoryDatabase
import com.nickrankin.traktapp.helper.ShowCreditsHelper
import com.nickrankin.traktapp.helper.ShowDataHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import javax.inject.Inject

class ShowDetailsOverviewRepository @Inject constructor(
    private val creditsHelper: ShowCreditsHelper,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase,
): CreditsRepository(creditsHelper, showsDatabase, creditsDatabase) {


}