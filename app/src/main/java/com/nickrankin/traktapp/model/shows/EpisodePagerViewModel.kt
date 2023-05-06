package com.nickrankin.traktapp.model.shows

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.model.BaseViewModel
import com.nickrankin.traktapp.model.datamodel.EpisodeDataModel
import com.nickrankin.traktapp.repo.shows.SeasonEpisodesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpisodePagerViewModel @Inject constructor(private val savedStateHandle: SavedStateHandle, private val repository: SeasonEpisodesRepository): BaseViewModel() {
    private val episodeDataModelChangedChannel = Channel<EpisodeDataModel>()
    private val episodeDataModelChanged = episodeDataModelChangedChannel.receiveAsFlow()
        .shareIn(viewModelScope, SharingStarted.Lazily, 1)

    val seasonEpisodes = refreshEvent.flatMapLatest { shouldRefresh ->
        episodeDataModelChanged.flatMapLatest { episodeDataModel ->
            repository.getSeasonEpisodes(episodeDataModel.traktId, episodeDataModel.tmdbId, episodeDataModel.seasonNumber, shouldRefresh).mapLatest {
                Pair(episodeDataModel.episodeNumber, it)

            }
        }
    }

    fun switchEpisodeDataModel(episodeDataModel: EpisodeDataModel?) {
        if(episodeDataModel == null) {
            return
        }
        viewModelScope.launch {
            episodeDataModelChangedChannel.send(episodeDataModel)
        }
    }
}