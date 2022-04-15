package com.nickrankin.traktapp.repo.movies

import android.content.SharedPreferences
import android.util.Log
import com.nickrankin.traktapp.api.TraktApi
import com.nickrankin.traktapp.helper.Resource
import com.uwetrottmann.trakt5.entities.RatedMovie
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.RatingsFilter
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private const val TAG = "MovieDetailsActionButto"
class MovieDetailsActionButtonRepository @Inject constructor(private val traktApi: TraktApi, private val sharedPreferences: SharedPreferences) {

}