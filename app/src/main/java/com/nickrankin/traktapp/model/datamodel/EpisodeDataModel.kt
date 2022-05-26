package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EpisodeDataModel(val traktId: Int, val tmdbId: Int?, val seasonNumber: Int, val episodeNumber: Int, val showTitle: String?) :
    Parcelable