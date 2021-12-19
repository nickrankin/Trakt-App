package com.nickrankin.traktapp.ui.shows

interface OnNavigateToEpisode {
    fun navigateToEpisode(showTraktId: Int, showTmdbId: Int, seasonNumber: Int, episodeNumber: Int, language: String?)
}