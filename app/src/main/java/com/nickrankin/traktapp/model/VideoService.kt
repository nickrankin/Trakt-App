package com.nickrankin.traktapp.model

data class VideoService(val tmdb_id: Int, val title: String, val tmdbPoviderId: Int, val providerTitle: String, val displayPriority: Int, val providerType: Int) {
    companion object {
        const val TYPE_TRAILER = 0
        const val TYPE_STREAM = 1
        const val TYPE_BUY = 2

        const val PROVIDER_NETFLIX = 8
        const val PROVIDER_AMAZON = 10
        const val PROVIDER_APPLE = 2
        const val PROVIDER_GOOGLE = 3
        const val PROVIDER_DISNEY = 390
        const val PROVIDER_YOUTUBE = 188
    }
}