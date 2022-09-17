package com.nickrankin.traktapp.api.services.trakt.model

import com.uwetrottmann.trakt5.entities.Episode
import com.uwetrottmann.trakt5.entities.EpisodeIds
import org.threeten.bp.OffsetDateTime

data class CollectedBaseEpisode(val episode: Episode, val collected_at: OffsetDateTime?, val updated_at: OffsetDateTime?)

