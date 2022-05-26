package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MovieDataModel(val traktId: Int, val tmdbId: Int?, val movieTitle: String?) : Parcelable