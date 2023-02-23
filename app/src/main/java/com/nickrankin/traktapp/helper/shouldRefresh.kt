package com.nickrankin.traktapp.helper

import com.nickrankin.traktapp.dao.refresh.LastRefreshedAt
import org.threeten.bp.OffsetDateTime
private const val REFRESH_INTERVAL = 24L
fun shouldRefresh(lastRefreshedAt: LastRefreshedAt?, interval: Long?): Boolean {
    return lastRefreshedAt?.last_refreshed_at?.plusHours(
        interval ?: REFRESH_INTERVAL
    )?.isBefore(
        OffsetDateTime.now()) ?: true
}