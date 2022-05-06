package com.nickrankin.traktapp.helper

import java.util.*

/**
 * Try to use the users default language for TMDB content, fall back to the contents original language or null to get default
 * https://developers.themoviedb.org/3/getting-started/languages
 *
 * @param contentLanguage The contents original language
 *
 * **/
fun getTmdbLanguage(): String {
    val systemLanguage = Locale.getDefault().language

        return "$systemLanguage,null"
}