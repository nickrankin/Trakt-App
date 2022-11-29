package com.nickrankin.traktapp.dao.credits.model

import com.uwetrottmann.trakt5.enums.Type


interface CreditPerson {
    val id: String
    val person_trakt_id: Int
    val trakt_id: Int
    val tmdb_id: Int?
    val title: String?
    val year: Int?
    val ordering: Int
    val type: Type
}