package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ShowDataModel(val traktId: Int, val tmdbId: Int?, val showTitle: String?) : Parcelable