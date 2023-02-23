package com.nickrankin.traktapp.dao.history.model

import org.threeten.bp.OffsetDateTime

interface HistoryEntry {
    val history_id: Long
    val trakt_id: Int
    val tmdb_id: Int?
    val title: String
    val watched_date: OffsetDateTime
    val cached_at: OffsetDateTime
}