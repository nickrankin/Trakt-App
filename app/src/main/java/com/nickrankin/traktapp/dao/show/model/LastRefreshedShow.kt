package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "show_last_refresh")
data class LastRefreshedShow(@PrimaryKey val tmdbId: Int, val lastRefreshDate: OffsetDateTime)
