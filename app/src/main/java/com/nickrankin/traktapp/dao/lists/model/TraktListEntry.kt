package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Embedded
import androidx.room.Relation

data class TraktListEntry(
    @Embedded val entryData: ListEntry,

    @Relation(
        parentColumn = "list_entry_trakt_id",
        entityColumn = "trakt_id"
    )
    val movie: MovieEntry?,

    @Relation(
        parentColumn = "list_entry_trakt_id",
        entityColumn = "trakt_id"
    )
    val show: ShowEntry?,

    @Relation(
        parentColumn = "list_entry_trakt_id",
        entityColumn = "trakt_id"
    )
    val person: PersonEntry?,

    @Relation(
        parentColumn = "list_entry_trakt_id",
        entityColumn = "trakt_id"
    )
    val episode: EpisodeEntry?,
    // An episode Entry will also have a Show
    @Relation(
        parentColumn = "show_trakt_id",
        entityColumn = "trakt_id"
    )
    val episodeShow: ShowEntry?,
)