package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "show_season_progress")
data class SeasonProgress(
    @PrimaryKey val season_trakt_id: Int,
    val show_trakt_id: Int,
    val season_number: Int,
    val played_episodes: Int
    )