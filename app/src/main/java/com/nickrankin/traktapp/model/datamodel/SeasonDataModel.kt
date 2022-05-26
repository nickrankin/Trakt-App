package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SeasonDataModel(val traktId: Int, val tmdbId: Int?, val seasonNumber: Int, val showTitle: String?) : Parcelable