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
import javax.inject.Inject

const val TAG = "PosterImageLoader"
class PosterImageLoader @Inject constructor(private val tmdbApi: TmdbApi, private val imagesDatabase: ImagesDatabase) {
    private val showPosterImagesDao = imagesDatabase.showPosterImagesDao()
    val scope = CoroutineScope(Dispatchers.IO)
    val mainThreadLopper = Handler(Looper.getMainLooper())
    fun loadImage(tmbId: Int, language: String?, callback: (posterPath: String) -> Unit) {
        scope.launch {

            val cachedPosters = showPosterImagesDao.getPoster(tmbId)

            if(cachedPosters != null) {
                Log.d(TAG, "loadImage: Poster $tmbId found in cache!")
                    mainThreadLopper.post {
                        callback(cachedPosters.poster_path)
                    }
            } else {
                try {
                    val images = tmdbApi.tmTvService().images(tmbId, getTmdbLanguage(language))

                    if(images.posters?.isNotEmpty() == true) {
                        showPosterImagesDao.insert(
                            ShowPosterImage(
                                tmbId,
                                images.posters?.first()?.file_path ?: ""
                            )
                        )

                        mainThreadLopper.post {
                            callback(images.posters?.first()?.file_path ?: "")
                        }
                    }

                } catch(e: Exception) {
                    e.printStackTrace()
                }
            }


        }

    }
}