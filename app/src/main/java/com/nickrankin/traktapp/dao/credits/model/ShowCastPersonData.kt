package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "show_cast")
data class ShowCastPersonData(@PrimaryKey val person_show_trakt_id: String, val person_trakt_id: Int, val show_trakt_id: Int, val show_tmdb_id: Int?, val ordering: Int, val is_guest_star: Boolean, val character: String?)