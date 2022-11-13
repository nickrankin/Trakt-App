package com.nickrankin.traktapp.adapter.movies

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBasePagingAdapter
import com.nickrankin.traktapp.adapter.shows.ICON_MARGIN
import com.nickrankin.traktapp.dao.movies.model.WatchedMovie
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.dao.stats.model.CollectedMoviesStats
import com.nickrankin.traktapp.dao.stats.model.RatingsMoviesStats
import com.nickrankin.traktapp.databinding.WatchedMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedMoviesPagingAdapter(controls: AdaptorActionControls<WatchedMovieAndStats>, private val sharedPreferences: SharedPreferences, private val glide: RequestManager, private val tmdbImageLoader: TmdbImageLoader): MediaEntryBasePagingAdapter<WatchedMovieAndStats>(controls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, holder.absoluteAdapterPosition)

        val currentItem = getItem(holder.absoluteAdapterPosition)

        val traktId = currentItem?.watchedMovie?.trakt_id ?: 0

        when(holder) {
            is CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = currentItem?.watchedMovie?.title
                    itemOverview.text = currentItem?.watchedMovie?.overview
                    if(currentItem?.watchedMovie?.watched_at != null) {
                        itemWatchedDate.text = "Watched: " + getFormattedDate(currentItem.watchedMovie.watched_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)+ " ", sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))
                    }
                    tmdbImageLoader.loadImages(currentItem?.watchedMovie?.trakt_id ?: 0, ImageItemType.MOVIE,currentItem?.watchedMovie?.tmdb_id ?: 0,  currentItem?.watchedMovie?.title, currentItem?.watchedMovie?.language,true, itemPoster, itemBackdropImageview, false)

                    val ratingStat = currentItem?.ratedMovieStats

//                    if(ratingStat != null) {
//                        watchedentryitemRating.visibility = View.VISIBLE
//                        watchedentryitemRating.text = "You rated: ${ratingStat.rating}"
//                    } else {
//                        watchedentryitemRating.visibility = View.GONE
//                    }
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
                        itemTimestamp.text = getFormattedDate(currentItem.watchedMovie.watched_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT)+ " ", sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))
                    }

                    tmdbImageLoader.loadImages(currentItem?.watchedMovie?.trakt_id ?: 0, ImageItemType.MOVIE,currentItem?.watchedMovie?.tmdb_id ?: 0,  currentItem?.watchedMovie?.title, currentItem?.watchedMovie?.language,true, itemPoster, null, false)

                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}", )
            }
        }
    }
//
//    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
////        super.onViewRecycled(holder)
//        Log.e(TAG, "onViewRecycled: Recyclered $holder", )
//    }
//
//
//
//    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
//        super.onViewDetachedFromWindow(holder)
//
//        glide.clear(holder.itemView.findViewById<ImageView>(R.id.item_poster))
//
//        //(holder as MediaEntryBasePagingAdapter<*>.CardViewHolder).bindings = null
//
//
//       // Log.e(TAG, "onViewDetachedFromWindow: Detached ${(holder as MediaEntryBasePagingAdapter<*>.PosterViewHolder).bindings.itemTitle.text} at pos ${holder.absoluteAdapterPosition}", )
//    }

    override fun reloadImages(
        selectedItem: WatchedMovieAndStats,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(selectedItem?.watchedMovie?.trakt_id ?: 0, ImageItemType.MOVIE,selectedItem?.watchedMovie?.tmdb_id ?: 0,  selectedItem?.watchedMovie?.title, selectedItem?.watchedMovie?.language,true, posterImageView, backdropImageView, true)
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