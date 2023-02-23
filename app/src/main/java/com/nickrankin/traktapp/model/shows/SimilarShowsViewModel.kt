package com.nickrankin.traktapp.model.shows

import androidx.lifecycle.ViewModel
import com.nickrankin.traktapp.repo.shows.showdetails.SimilarShowsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SimilarShowsViewModel @Inject constructor(private val repository: SimilarShowsRepository): ViewModel() {
    suspend fun getSimilarShows(tmdbId: Int, language: String?) = repository.getRecommendedMovies(tmdbId, language)

    suspend fun getTraktIdFromTmdbId(tmdbId: Int) = repository.getTraktIdFromTmdbId(tmdbId)
}