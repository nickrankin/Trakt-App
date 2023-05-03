package com.nickrankin.traktapp.helper

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import androidx.room.withTransaction
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.api.TmdbApi
import com.nickrankin.traktapp.dao.images.ImagesDatabase
import com.nickrankin.traktapp.dao.images.model.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import retrofit2.HttpException
import java.lang.RuntimeException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PosterImageLoader"
private const val PREFIX_MOVIE = "mov_"
private const val PREFIX_SHOW = "show_"
private const val PREFIX_PERSON = "per_"
private const val PREFIX_EPISODE = "eps_"

enum class ImageItemType { MOVIE, SHOW, EPISODE, PERSON }

@Singleton
class TmdbImageLoader @Inject constructor(
    private val tmdbApi: TmdbApi,
    private val imagesDatabase: ImagesDatabase,
    private val glide: RequestManager
) {
    private val imagesDao = imagesDatabase.imeagesDao()


    val scope = CoroutineScope(Dispatchers.IO)
    val mainThreadLopper = Handler(Looper.getMainLooper())

    fun loadImages(
        traktId: Int,
        imageItemType: ImageItemType,
        tmbId: Int?,
        title: String?,
        originalLanguage: String?,
        shouldCache: Boolean,
        posterImageView: ImageView,
        backdropImageView: ImageView?,
        forceRefresh: Boolean
    ) {
        scope.launch {
            val cachedPosters = imagesDao.getImage(getPrefix(imageItemType)+traktId)

            if (cachedPosters != null && !forceRefresh) {
                //Log.d(TAG, "loadImages: Loading images from cache for $traktId // $title")
                loadImagesOnMainThread(cachedPosters, imageItemType, posterImageView, backdropImageView)
            } else {
//                Log.d(TAG, "loadImages: Refreshing images for $traktId // $title")

                if (imageItemType == ImageItemType.EPISODE) {
                    throw RuntimeException("For episodes, use loadEpisodeImages() method")

                } else {
                    val images = getTmdbImages(traktId, imageItemType, tmbId, null, null, originalLanguage)

                    if (shouldCache && !images.poster_path.isNullOrBlank()) {
//                        Log.d(TAG, "loadImages: Caching images for TraktId $traktId // $title")
                        imagesDao.insert(images)
                    }

                    loadImagesOnMainThread(images, imageItemType, posterImageView, backdropImageView)
                }


            }
        }
    }

    fun loadEpisodeImages(
        traktId: Int,
        tmbId: Int?,
        showTraktId: Int,
        season: Int?,
        episode: Int?,
        showTitle: String?,
        originalLanguage: String?,
        shouldCache: Boolean,
        posterImageView: ImageView,
        backdropImageView: ImageView?,
        shouldRefresh: Boolean
    ) {
        // Episodes are a special item type, as episodes may have Still shots we should load. An episode image should include a still for backdrop, and the shows poster.

        scope.launch {
            // See if we have Episode images, if not get them:
            val cachedEpisodeImages = imagesDao.getImage(PREFIX_EPISODE+traktId)

            if(cachedEpisodeImages != null && !shouldRefresh) {
                //Log.d(TAG, "loadEpisodeImages: Loading image from cache $traktId - $showTitle")
                loadImagesOnMainThread(cachedEpisodeImages, ImageItemType.EPISODE, posterImageView, backdropImageView)

            } else {
//                Log.d(TAG, "loadEpisodeImages: Loading image from cache $traktId - $showTitle")

                // Check if the Show images are already cached:
                val cachedShowImages = imagesDao.getImage(PREFIX_SHOW+showTraktId)

                if(cachedShowImages != null) {
                    Log.d(TAG, "loadEpisodeImages: Show images for $tmbId located in cache successfully")
                    // Get the Episode Stills data
                    val stillsPath = getEpisodeStillImages( tmbId ?: 0, season, episode)

                    val episodeImages = Image(PREFIX_EPISODE+traktId, tmbId, cachedShowImages.poster_path, stillsPath ?: cachedShowImages.backdrop_path, OffsetDateTime.now())

                    // Insert the Episode data record
                    imagesDatabase.withTransaction {
                        imagesDao.insert(
                            episodeImages
                        )
                    }

                    loadImagesOnMainThread(episodeImages, ImageItemType.EPISODE, posterImageView, backdropImageView)

                } else {
//                    Log.d(TAG, "loadEpisodeImages: Getting show images for TMDBID $tmbId")
                    val showImages = getTmdbImages(showTraktId, ImageItemType.SHOW, tmbId, showTitle, null, originalLanguage)

                    // Insert the Show images to cache
                    if(shouldCache && !showImages.poster_path.isNullOrBlank()) {
                        imagesDatabase.withTransaction {
                            imagesDao.insert(showImages)
                        }
                    }

                    // Get the Episode Stills data
                    val stillsPath = getEpisodeStillImages( tmbId ?: 0, season, episode)

                    val episodeImages = Image(PREFIX_EPISODE+traktId, tmbId, showImages.poster_path, stillsPath ?: showImages.backdrop_path, OffsetDateTime.now())


                    // Insert the Episode data record
                    if(shouldCache && !showImages.poster_path.isNullOrBlank()) {
                        imagesDatabase.withTransaction {
                            imagesDao.insert(
                                episodeImages
                            )
                        }
                    }

                    loadImagesOnMainThread(episodeImages, ImageItemType.EPISODE, posterImageView, backdropImageView)
                }
            }
        }


    }

    private suspend fun getTmdbImages(
        traktId: Int, imageItemType: ImageItemType,
        tmbId: Int?, title: String?, year: Int?, originalLanguage: String?
    ): Image {

        if (imageItemType == ImageItemType.EPISODE) {
            throw RuntimeException("Not allowed use this method to get episode images")
        }

        var traktIdString = ""

        when(imageItemType) {
            ImageItemType.MOVIE -> {
                traktIdString = PREFIX_MOVIE+traktId
            }
            ImageItemType.SHOW -> {
                traktIdString = PREFIX_SHOW+traktId
            }
            ImageItemType.PERSON -> {
                traktIdString = PREFIX_PERSON+traktId
            }
            else -> {}
        }

        val images = Image(
            traktIdString, tmbId, null,
            null, OffsetDateTime.now()
        )

        var currentItemTmdbId = tmbId

        if (tmbId == null || tmbId == 0 || tmbId == -1) {
            Log.d(
                TAG,
                "getTmdbImages: TMDB ID null, searching for $title images (trakt id $traktId)"
            )
            currentItemTmdbId = findTmdbId(traktId, imageItemType, title, year, originalLanguage)

            if (currentItemTmdbId == null) {
                Log.e(TAG, "getTmdbImages: No images for trakt Id $traktId was found")
                return images
            }
        }

        try {
            when (imageItemType) {
                ImageItemType.MOVIE -> {
                    val response = tmdbApi.tmMovieService()
                        .images(currentItemTmdbId!!, getTmdbLanguage(originalLanguage))


                    if (response.posters?.isNotEmpty() == true) {
                        images.poster_path = response.posters?.first()?.file_path
                    }

                    if (response.backdrops?.isNotEmpty() == true) {
                        images.backdrop_path = response.backdrops?.first()?.file_path
                    }
                }
                ImageItemType.SHOW -> {
                    val response = tmdbApi.tmTvService()
                        .images(currentItemTmdbId!!, getTmdbLanguage(originalLanguage))


                    if (response.posters?.isNotEmpty() == true) {
                        images.poster_path = response.posters?.first()?.file_path
                    }

                    if (response.backdrops?.isNotEmpty() == true) {
                        images.backdrop_path = response.backdrops?.first()?.file_path
                    }
                }
                ImageItemType.EPISODE -> {

                }
                else -> {
                    Log.e(TAG, "getTmdbImages: Invalid item type")
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
        return images
    }

    private suspend fun getEpisodeStillImages(
        showTmdbId: Int,
        season: Int?,
        episode: Int?
    ): String? {
        try {
            if (season != null && episode != null) {
                Log.d(TAG, "getTmdbImages: Getting Episode images")
                val response = tmdbApi.tmTvEpisodesService()
                    .images(showTmdbId, season, episode)

                if (response.stills?.isNotEmpty() == true) {
                    return response.stills?.first()?.file_path
                }

            }
        } catch(e: Exception) {
            Log.e(TAG, "getEpisodeStillImages: Error getting episode images ${e.message}")
            e.printStackTrace()
        }

        return null
    }


    private suspend fun findTmdbId(
        traktId: Int, imageItemType: ImageItemType,
        title: String?, year: Int?, originalLanguage: String?
    ): Int? {
        Log.d(
            TAG,
            "findTmdbId: Finding $imageItemType based on title $title Year $year Trakt Id $traktId"
        )
        try {
            when (imageItemType) {
                ImageItemType.MOVIE -> {
                    val foundMovies = tmdbApi.tmSearchService().movie(
                        title,
                        1,
                        getTmdbLanguage(originalLanguage),
                        null,
                        false,
                        year,
                        null
                    )

                    Log.d(TAG, "findTmdbId: Found Movie ${foundMovies.results.size}")

                    if (foundMovies.results?.isNotEmpty() == true) {
                        val tmdbMovie = foundMovies.results?.first()

                        Log.d(
                            TAG,
                            "findTmdbId: returning TMDB ID: ${tmdbMovie?.id} (Found Item Title: ${tmdbMovie?.title})"
                        )

                        return tmdbMovie?.id
                    }
                }
                ImageItemType.SHOW -> {
                    val foundShows = tmdbApi.tmSearchService().tv(
                        title,
                        1,
                        getTmdbLanguage(originalLanguage),
                        year,
                        false
                    )

                    Log.d(TAG, "findTmdbId: Found shows ${foundShows.results.size}")

                    if (foundShows.results?.isNotEmpty() == true) {
                        val tmdbShow = foundShows.results?.first()

                        Log.d(
                            TAG,
                            "findTmdbId: Returning TMDB ID ${tmdbShow?.id} (Found Item Title ${tmdbShow?.name})"
                        )

                        return tmdbShow?.id
                    }
                }
                ImageItemType.EPISODE -> {
                    return null
                }
                else -> {}
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

    private fun loadImagesOnMainThread(
        image: Image,
        imageItemType: ImageItemType,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        mainThreadLopper.post {
            if (image.poster_path != null) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + image.poster_path)
                    .override(180, 250)
                    .into(posterImageView)
            } else {
                when(imageItemType) {
                    ImageItemType.MOVIE -> { posterImageView.setImageResource(R.drawable.ic_baseline_local_movies_24) }
                    ImageItemType.SHOW ->  { posterImageView.setImageResource(R.drawable.ic_baseline_tv_24) }
                    ImageItemType.EPISODE ->  { posterImageView.setImageResource(R.drawable.ic_baseline_tv_24) }
                    ImageItemType.PERSON ->  { posterImageView.setImageResource(R.drawable.ic_baseline_person_24) }
                }            }

            if (image.backdrop_path != null && backdropImageView != null) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + image.backdrop_path)
                    .into(backdropImageView)
            } else {
                backdropImageView?.setImageDrawable(null)
            }
        }
    }

    private fun getPrefix(imageItemType: ImageItemType): String {
        return when(imageItemType) {
            ImageItemType.MOVIE -> {
                PREFIX_MOVIE
            }
            ImageItemType.SHOW -> {
                PREFIX_SHOW
            }
            ImageItemType.EPISODE -> {
                PREFIX_EPISODE
            }
            ImageItemType.PERSON -> {
                PREFIX_PERSON
            }
            else -> {
                ""
            }
        }
    }
}