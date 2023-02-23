package com.nickrankin.traktapp.model.movies

import androidx.lifecycle.ViewModel
import com.nickrankin.traktapp.repo.movies.SimilarMoviesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SimilarMoviesViewModel @Inject constructor(private val repository: SimilarMoviesRepository): ViewModel() {
    suspend fun getSimilarMovies(tmdbId: Int, movieLanguage: String?) = repository.getRecommendedMovies(tmdbId, movieLanguage)

    suspend fun getTraktIdFromTmdbId(tmdbId: Int) = repository.getTraktIdFromTmdbId(tmdbId)
}