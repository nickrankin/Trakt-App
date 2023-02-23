package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Embedded
import androidx.room.Relation
import com.nickrankin.traktapp.dao.base_entity.*

data class TraktListEntry(

    @Embedded val entryData: ListEntry,

    @Relation(
        parentColumn = "item_trakt_id",
        entityColumn = "trakt_id"
    )
    val movie: MovieBaseEntity?,

    @Relation(
        parentColumn = "item_trakt_id",
        entityColumn = "trakt_id"
    )
    val show: ShowBaseEntity?,

    @Relation(
        parentColumn = "item_trakt_id",
        entityColumn = "trakt_id"
    )
    val person: PersonBaseEntity?,

    @Relation(
        parentColumn = "item_trakt_id",
        entityColumn = "trakt_id"
    )
    val episode: EpisodeBaseEnity?,

    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "trakt_id"
    )
    val episodeShow: ShowBaseEntity?
)