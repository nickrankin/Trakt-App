package com.nickrankin.traktapp.helper

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.dao.images.ImagesDatabase
import com.nickrankin.traktapp.dao.images.model.ShowPosterImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

private const val TAG = "PosterImageLoader"

class PosterImageLoader @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val imagesDatabase: ImagesDatabase
) {
    private val showPosterImagesDao = imagesDatabase.showPosterImagesDao()
    val scope = CoroutineScope(Dispatchers.IO)
    val mainThreadLopper = Handler(Looper.getMainLooper())
    fun loadImage(
        traktId: Int,
        tmbId: Int?,
        language: String?,
        title: String,
        year: Int?,
        shouldCache: Boolean,
        callback: (image: ShowPosterImage) -> Unit
    ) {
        scope.launch {
            val cachedPosters = showPosterImagesDao.getPoster(traktId)

            var currentTmdbId: Int?

            if (cachedPosters != null) {
                //Log.d(TAG, "loadImage: Found cached posters for $traktId. $title")
                mainThreadLopper.post {
                    callback(cachedPosters)
                }
            } else {
                currentTmdbId = if (tmbId != null && tmbId != -1 && tmbId != 0) {
                    tmbId
                } else {
                    Log.d(TAG, "loadImage: Try to find TMDB ID for $title")

                    // Try to find Show if Trakt has no TMDB ID maintained
                    findTmdbId(traktId, title, year)
                }

                if (currentTmdbId != null) {
                    val imagePath = getTmdbImage(currentTmdbId, language)

                    val posterImages = ShowPosterImage(
                        traktId,
                        imagePath,
                        OffsetDateTime.now()
                    )

                    if (imagePath != null) {
                        // if (shouldCache) {
                        Log.d(
                            TAG,
                            "loadImage: Saving poster for TraktId: $traktId. TMDB ID: $currentTmdbId. Title $title"
                        )
                        showPosterImagesDao.insert(
                            posterImages
                        )
                        //    }

                        mainThreadLopper.post {
                            callback(posterImages)
                        }
                    } else {
                        // If there is no poster on the TMDB set a blank Poster Image to prevent constant hammering the API
                        Log.d(TAG, "loadImage: Setting empty poster image for $traktId. $title")
                        val emptyPosterImage =
                            ShowPosterImage(
                                traktId,
                                null,
                                OffsetDateTime.now()
                            )
                        if (shouldCache) {
                            showPosterImagesDao.insert(
                                emptyPosterImage
                            )
                        }

                        mainThreadLopper.post {
                            callback(
                                emptyPosterImage
                            )
                        }
                    }
                } else {
                    // If there is no poster on the TMDB set a blank Poster Image to prevent constant hammering the API
                    Log.d(TAG, "loadImage: Setting empty poster image for $traktId. $title")
                    val emptyPosterImage =
                        ShowPosterImage(
                            traktId,
                            null,
                            OffsetDateTime.now()
                        )
                    if (shouldCache) {
                        showPosterImagesDao.insert(
                            emptyPosterImage
                        )
                    }

                    mainThreadLopper.post {
                        callback(
                            emptyPosterImage
                        )
                    }
                }
            }
        }
    }

    private suspend fun findTmdbId(traktId: Int, title: String, year: Int?): Int? {
        try {
            val foundShow = tmdbApi.tmSearchService().tv(
                title,
                1,
                getTmdbLanguage(Locale.getDefault().displayLanguage),
                year,
                false
            )

            if (foundShow.results?.isNotEmpty() == true) {
                val tmdbShow = foundShow.results?.first()
                Log.d(
                    TAG,
                    "loadImage: Found show $title on TMDB (TMDB ID: ${tmdbShow?.id})"
                )

                return tmdbShow?.id
            }

        } catch (e: HttpException) {
            Log.e(
                TAG,
                "loadImage: HttpException Error finding this show TMDB (Show Trakt ID: $traktId). Error code: ${e.code()}. ${e.message()}",
            )
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "loadImage: Error finding this show TMDB (Show Trakt ID: $traktId)")
            e.printStackTrace()
        }

        return null
    }

    private suspend fun getTmdbImage(tmbId: Int, language: String?): String? {
        try {
            val images = tmdbApi.tmTvService()
                .images(tmbId, getTmdbLanguage(language))

            if (images.posters?.isNotEmpty() == true) {
                return images.posters?.first()?.file_path
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}