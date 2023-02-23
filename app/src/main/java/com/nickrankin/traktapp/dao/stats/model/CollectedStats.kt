package com.nickrankin.traktapp.dao.stats.model

import org.threeten.bp.OffsetDateTime

interface CollectedStats {
    val trakt_id: Int
    val tmdb_id: Int?
    val collected_at: OffsetDateTime?
    val title: String
    val listedAt: OffsetDateTime?
}