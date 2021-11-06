package com.nickrankin.traktapp.dao.show.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watched_episode_page_keys")
data class WatchedEpisodePageKey(
    @PrimaryKey val episodeTraktId: Int,
    val prevPage: Int?,
    val nextPage: Int?
)