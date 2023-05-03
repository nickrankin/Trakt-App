package com.nickrankin.traktapp.dao.calendars.model

import org.threeten.bp.OffsetDateTime

open class BaseCalendarEntry(open val episode_trakt_id: Int, open val first_aired: OffsetDateTime?)