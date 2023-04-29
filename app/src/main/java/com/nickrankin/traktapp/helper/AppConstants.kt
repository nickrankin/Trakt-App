package com.nickrankin.traktapp.helper

import org.threeten.bp.OffsetDateTime

class AppConstants {
    companion object {
        const val APP_TITLE = "Trakt Manager"
        const val APP_VERSION = "0.1"
        const val APP_DATE = "17/11/2021"
        const val DATE_FORMAT = "date_format"
        const val TIME_FORMAT = "time_format"
        const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"
        const val DEFAULT_TIME_FORMAT = "HH:mm"

        const val DEFAULT_DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"

        const val TMDB_MOVIE_URL = "https://www.themoviedb.org/movie/"
        const val TMDB_SHOW_URL = "https://www.themoviedb.org/tv/"
        const val TMDB_POSTER_URL = "https://image.tmdb.org/t/p/w500"
    }
}