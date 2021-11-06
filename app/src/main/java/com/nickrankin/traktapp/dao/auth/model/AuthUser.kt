package com.nickrankin.traktapp.dao.auth.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "users")
data class AuthUser(
    @PrimaryKey() val slug: String,
    val avatar: String?,
    val cover_image: String?,
    val time_zone: String,
    val watching_sharing_text: String?,
    val watched_sharing_text: String?,
    val about: String?,
    val age: Int,
    val gender: String,
    val is_private: Boolean,
    val joined_at: OffsetDateTime,
    val location: String,
    val name: String,
    val username: String,
    val vip: Boolean,
    val vip_ep: Boolean
)