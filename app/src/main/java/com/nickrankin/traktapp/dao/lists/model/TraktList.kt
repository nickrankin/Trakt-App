package com.nickrankin.traktapp.dao.lists.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.uwetrottmann.trakt5.entities.User
import com.uwetrottmann.trakt5.enums.ListPrivacy
import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "lists")
data class TraktList(
    @PrimaryKey val trakt_id: Int,
    val list_slug: String,
    val name: String,
    val description: String?,
    val created_at: OffsetDateTime,
    val updated_at: OffsetDateTime?,
    val allow_comments: Boolean,
    val comments_count: Int,
    val display_numbers: Boolean,
    val item_count: Int,
    val likes: Int,
    val privacy: ListPrivacy,
    val sortBy: SortBy,
    val sortHow: SortHow,
    val user: User
)