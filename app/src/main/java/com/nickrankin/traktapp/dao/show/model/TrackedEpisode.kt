package com.nickrankin.traktapp.dao.show.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.tmdb2.entities.Network
import kotlinx.parcelize.Parcelize
import org.threeten.bp.OffsetDateTime
@Parcelize
@Entity(tableName = "notifications_episode")
data class TrackedEpisode(@PrimaryKey val trakt_id: Int, val tmdb_id: Int?, val show_trakt_id: Int, val show_tmdb_id: Int?, val airs_date: OffsetDateTime?, val network: String?, val title: String?, val show_title: String, val season: Int, val episode: Int, var lastRefreshed: OffsetDateTime, var dismiss_count: Int, val alreadyNotified: Boolean) :
    Parcelable