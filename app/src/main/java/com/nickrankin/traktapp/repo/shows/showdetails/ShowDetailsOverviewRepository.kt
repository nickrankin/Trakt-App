package com.nickrankin.traktapp.repo.shows.showdetails

import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.show.ShowsDatabase
import com.nickrankin.traktapp.helper.ShowCreditsHelper
import com.nickrankin.traktapp.repo.shows.CreditsRepository
import javax.inject.Inject

class ShowDetailsOverviewRepository @Inject constructor(
    private val creditsHelper: ShowCreditsHelper,
    private val showsDatabase: ShowsDatabase,
    private val creditsDatabase: CreditsDatabase,
): CreditsRepository(creditsHelper, showsDatabase, creditsDatabase) {
    private val showsDao = showsDatabase.tmShowDao()

    fun getShow(traktId: Int) = showsDao.getShow(traktId)


}