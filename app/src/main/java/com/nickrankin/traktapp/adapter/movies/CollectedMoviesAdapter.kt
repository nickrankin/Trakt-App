package com.nickrankin.traktapp.adapter.movies

import android.content.SharedPreferences
import android.util.Log
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBaseAdapter
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime

private const val TAG = "CollectedMoviesAdapter"
class CollectedMoviesAdapter(private val tmdbImageLoader: TmdbImageLoader,
                             private val sharedPreferences: SharedPreferences,
                             controls: AdaptorActionControls<CollectedMovie>
) : MediaEntryBaseAdapter<CollectedMovie>(controls,
    COMPARATOR
) {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //holder.setIsRecyclable(false)
        super.onBindViewHolder(holder, position)

        val currentItem = getItem(position)

        when(holder) {
            is MediaEntryBaseAdapter<*>.PosterViewHolder -> {
                holder.bindings.apply {
                    itemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

                    itemTitle.text = "${currentItem.title} (${currentItem.release_date?.year ?: "unknown"})"

                    tmdbImageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.MOVIE,
                        currentItem.tmdb_id,
                        currentItem.title,
                        currentItem.language,
                        true,
                        itemPoster,
                        null,
                    false)

                }
            }

            is MediaEntryBaseAdapter<*>.CardViewHolder -> {
                holder.bindings.apply {
                    itemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
//
                    itemTitle.text = "${currentItem.title} (${currentItem.release_date?.year ?: "unknown"})"

                    if(currentItem.collected_at != null) {
                        itemWatchedDate.text = "Collected: ${getFormattedDateTime(currentItem.collected_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format",AppConstants.DEFAULT_TIME_FORMAT))}"
                    }

                    itemOverview.text = currentItem.movie_overview


                    tmdbImageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.MOVIE,
                        currentItem.tmdb_id,
                        currentItem.title,
                        currentItem.language,
                        true,
                        itemPoster,
                        itemBackdropImageview,
                    false)
                }
            }
            else -> {
                Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}", )
            }
        }
    }

    override fun reloadImages(
        selectedItem: CollectedMovie,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadImages(
            selectedItem.trakt_id,
            ImageItemType.MOVIE,
            selectedItem.tmdb_id,
            selectedItem.title,
            selectedItem.language,
            true,
            posterImageView,
            backdropImageView,
            true)
    }

    companion object {
        const val ACTION_REMOVE_COLLECTION = 1

        val COMPARATOR = object : DiffUtil.ItemCallback<CollectedMovie>() {
            override fun areItemsTheSame(oldItem: CollectedMovie, newItem: CollectedMovie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CollectedMovie,
                newItem: CollectedMovie
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }

            override fun getChangePayload(oldItem: CollectedMovie, newItem: CollectedMovie): Any? {
                Log.e(TAG, "getChangePayload: Change detected ${oldItem.title} // ${newItem.title}", )
                return super.getChangePayload(oldItem, newItem)
            }
        }
    }
}