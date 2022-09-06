package com.nickrankin.traktapp.repo.movies

import android.util.Log
import androidx.room.withTransaction
import com.nickrankin.traktapp.dao.credits.CreditsDatabase
import com.nickrankin.traktapp.dao.credits.MovieCastPeopleDao
import com.nickrankin.traktapp.dao.credits.MovieCastPerson
import com.nickrankin.traktapp.dao.movies.MoviesDatabase
import com.nickrankin.traktapp.dao.movies.model.TmMovie
import com.nickrankin.traktapp.helper.PersonCreditsHelper
import com.nickrankin.traktapp.helper.networkBoundResource
import com.nickrankin.traktapp.model.datamodel.MovieDataModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val TAG = "MovieDetailsOverviewRep"
class MovieDetailsOverviewRepository @Inject constructor(
    private val personCreditsHelper: PersonCreditsHelper,
    private val moviesDatabase: MoviesDatabase,
    private val creditsDatabase: CreditsDatabase
) {
    private val tmMovieDao = moviesDatabase.tmMovieDao()
    private val movieCastPeopleDao = creditsDatabase.movieCastPeopleDao()

    fun getCredits(movieDataModel: MovieDataModel?): Flow<List<MovieCastPerson>> {
        return movieCastPeopleDao.getMovieCast(movieDataModel?.traktId ?: 0)

    }

    fun getMovie(movieDataModel: MovieDataModel?): Flow<TmMovie?> {
        return tmMovieDao.getMovieById(movieDataModel?.traktId ?: 0)
    }

}