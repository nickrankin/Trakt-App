package com.nickrankin.traktapp.api.services.trakt.model

import com.uwetrottmann.trakt5.entities.CastMember
import com.uwetrottmann.trakt5.entities.Crew

class TmCredits {
    val cast: List<CastMember>? = null
    val guest_stars: List<CastMember>? = null
    val crew: Crew? = null
}