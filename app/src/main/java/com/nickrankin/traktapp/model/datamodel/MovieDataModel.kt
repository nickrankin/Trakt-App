package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class MovieDataModel(override val traktId: Int, override val tmdbId: Int?, val movieTitle: String?, val movieYear: Int?) : BaseDataModel, Parcelable