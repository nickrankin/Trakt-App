package com.nickrankin.traktapp.model.datamodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class PersonDataModel(override val traktId: Int,
                            override val tmdbId: Int?, val name: String?) : BaseDataModel,
    Parcelable