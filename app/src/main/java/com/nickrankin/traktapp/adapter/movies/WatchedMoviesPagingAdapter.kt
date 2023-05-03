package com.nickrankin.traktapp.adapter.movies

import android.content.SharedPreferences
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBasePagingAdapter
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedMoviesPagingAdapter(controls: AdaptorActionControls<WatchedMovieAndStats>, private val sharedPreferences: SharedPreferences, private val glide: RequestManager, private val tmdbImageLoader: TmdbImageLoader): MediaEntryBasePagingAdapter<WatchedMovieAndStats>(controls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, holder.absoluteAdapterPosition)

        val currentItem = getItem(holder.absoluteAdapterPosition)

        when(holder) {
            is CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = currentItem?.watchedMovie?.title
                    itemOverview.text = currentItem?.watchedMovie?.overview
                    if(currentItem?.watchedMovie?.watched_at != null) {
                        itemWatchedDate.text = "Watched: " + getFormattedDateTime(currentItem.watchedMovie.watched_at, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)+ " ", sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))
                    }
                    tmdbImageLoader.loadImages(currentItem?.watchedMovie?.trakt_id ?: 0, ImageItemType.MOVIE,currentItem?.watchedMovie?.tmdb_id ?: 0,  currentItem?.watchedMovie?.title, currentItem?.watchedMovie?.language,true, itemPoster, itemBackdropImageview, false)
                }

            }
            is PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = currentItem?.watchedMovie?.title

                    if(controls?.buttonIconResource != null) {
                        itemIcon.visibility = View.VISIBLE
                        itemIcon.setImageDrawable(controls.buttonIconResource)

                    }

                    if(currentItem?.watchedMovie?.watched_at != null) {
                        itemTimestamp.visibility = View.VISIBLE
                        itemTimestamp.text = getFormattedDateTime(currentItem.watchedMovie.watched_at, sharedPreferences.getString(AppConstants.DATE_FORMAT, AppConstants.DEFAULT_DATE_FORMAT)+ " ", sharedPreferences.getString(AppConstants.TIME_FORMAT, AppConstants.DEFAULT_TIME_FORMAT))
                    }

                    tmdbImageLoader.loadImages(currentItem?.watchedMovie?.trakt_id ?: 0, ImageItemType.MOVIE,currentItem?.watchedMovie?.tmdb_id ?: 0,  currentItem?.watchedMovie?.title, currentItem?.watchedMovie?.language,true, itemPoster, null, false)

                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}")
            }
        }
    }

    override fun reloadImages(
        selectedItem: WatchedMovieAndStats,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem.watchedMovie.trakt_id, ImageItemType.MOVIE,selectedItem.watchedMovie.tmdb_id,  selectedItem.watchedMovie.title, selectedItem.watchedMovie.language,true, posterImageView, backdropImageView, true)
    }

    companion object {
        const val ACTION_NAVIGATE_MOVIE = 0
        const val ACTION_REMOVE_HISTORY = 1


        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedMovieAndStats>() {
            override fun areItemsTheSame(
                oldItem: WatchedMovieAndStats,
                newItem: WatchedMovieAndStats
            ): Boolean {
                return oldItem.watchedMovie.id == newItem.watchedMovie.id
            }

            override fun areContentsTheSame(
                oldItem: WatchedMovieAndStats,
                newItem: WatchedMovieAndStats
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}