package com.nickrankin.traktapp.ui.shows

interface OnNavigateToShow {
    fun navigateToShow(traktId: Int, tmdbId: Int?, title: String?)
}