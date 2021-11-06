package com.nickrankin.traktapp.helper

import org.threeten.bp.OffsetDateTime

/*
*
* Function to determine if the provided last refresh date is after the current time minus refresh inteval
*
**/
fun shouldRefreshContents(lastRefreshed: String, refreshInterval: Long): Boolean {
    // Treat as the initial refresh
    if(lastRefreshed.isEmpty()) {
        return true
    }

    val lastRefreshedAt = OffsetDateTime.parse(lastRefreshed)

    return OffsetDateTime.now().minusHours(refreshInterval).isAfter(lastRefreshedAt)
}