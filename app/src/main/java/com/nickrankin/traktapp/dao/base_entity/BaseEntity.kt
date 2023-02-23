package com.nickrankin.traktapp.dao.base_entity

import org.threeten.bp.LocalDate
import org.threeten.bp.OffsetDateTime

interface BaseEntity {
    val trakt_id: Int
    val tmdb_id: Int?
    val title: String
    val language: String?
}