package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SeasonDataModel(override val traktId: Int, override val tmdbId: Int?, val seasonNumber: Int, val showTitle: String?) : BaseDataModel, Parcelable