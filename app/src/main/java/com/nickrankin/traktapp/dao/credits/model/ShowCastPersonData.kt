package com.nickrankin.traktapp.dao.credits.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "show_cast")
data class ShowCastPersonData(@PrimaryKey val castPersonTraktId: Int, val showTraktId: Int, val ordering: Int, val isGuestStar: Boolean, val character: String?)