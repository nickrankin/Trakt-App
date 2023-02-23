package com.nickrankin.traktapp.model

import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.helper.Resource
import kotlinx.coroutines.flow.Flow

interface ICreditsPersons {
    val cast:  Flow<Resource<List<ShowCastPerson>>>

    fun filterCast(showGuestStars: Boolean)
}