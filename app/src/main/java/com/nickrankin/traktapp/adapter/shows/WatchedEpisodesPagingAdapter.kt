package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.adapter.AdaptorActionControls
import com.nickrankin.traktapp.adapter.MediaEntryBasePagingAdapter
import com.nickrankin.traktapp.dao.show.model.WatchedEpisodeAndStats
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.nickrankin.traktapp.helper.getFormattedDateTime

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedEpisodesPagingAdapter(controls: AdaptorActionControls<WatchedEpisodeAndStats>, private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader): MediaEntryBasePagingAdapter<WatchedEpisodeAndStats>(controls, COMPARATOR) {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val currentItem = getItem(position)

        when(holder) {
            is PosterViewHolder -> {
                holder.bindings.apply {
                    itemTitle.text = "${currentItem?.watchedEpisode?.show_title} (S${currentItem?.watchedEpisode?.episode_season}E${currentItem?.watchedEpisode?.episode_number})"

                    tmdbImageLoader.loadEpisodeImages(currentItem?.watchedEpisode?.episode_trakt_id ?: 0, currentItem?.watchedEpisode?.show_tmdb_id ?: 0, currentItem?.watchedEpisode?.show_trakt_id ?: 0,
                        currentItem?.watchedEpisode?.episode_season, currentItem?.watchedEpisode?.episode_number, currentItem?.watchedEpisode?.show_title ?: "", currentItem?.watchedEpisode?.language, true, itemPoster, null, false)

                    if(currentItem?.watchedEpisode?.watched_at != null) {
                        itemTimestamp.visibility = View.VISIBLE
                        itemTimestamp.text = "Watched: ${getFormattedDateTime(currentItem.watchedEpisode.watched_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }
                }
            }
            is CardViewHolder -> {
                holder.bindings.apply {

                    itemTitle.text = currentItem?.watchedEpisode?.episode_title
                    itemSubTitle.visibility = View.VISIBLE
                    itemSubTitle.text = "${currentItem?.watchedEpisode?.show_title} - (S${currentItem?.watchedEpisode?.episode_season}E${currentItem?.watchedEpisode?.episode_number})"

                    itemOverview.text = currentItem?.watchedEpisode?.episode_overview


                    if(currentItem?.watchedEpisode?.watched_at != null) {
                        itemWatchedDate.visibility = View.VISIBLE
                        itemWatchedDate.text = "Watched: ${getFormattedDateTime(currentItem.watchedEpisode.watched_at, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
                    }


                    tmdbImageLoader.loadEpisodeImages(currentItem?.watchedEpisode?.episode_trakt_id ?: 0, currentItem?.watchedEpisode?.show_tmdb_id ?: 0, currentItem?.watchedEpisode?.show_trakt_id ?: 0,
                        currentItem?.watchedEpisode?.episode_season, currentItem?.watchedEpisode?.episode_number, currentItem?.watchedEpisode?.show_title ?: "", currentItem?.watchedEpisode?.language, true, itemPoster, itemBackdropImageview, false)

                }
            }
        }
    }

    override fun reloadImages(
        selectedItem: WatchedEpisodeAndStats,
        posterImageView: ImageView,
        backdropImageView: ImageView?
    ) {
        tmdbImageLoader.loadEpisodeImages(selectedItem?.watchedEpisode?.episode_trakt_id ?: 0, selectedItem?.watchedEpisode?.show_tmdb_id ?: 0, selectedItem?.watchedEpisode?.show_trakt_id ?: 0,
            selectedItem?.watchedEpisode?.episode_season, selectedItem?.watchedEpisode?.episode_number, selectedItem?.watchedEpisode?.show_title ?: "", selectedItem?.watchedEpisode?.language, true, posterImageView, backdropImageView, true)
    }

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_HISTORY = 1
        const val ACTION_NAVIGATE_EPISODE = 2


        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedEpisodeAndStats>() {
            override fun areItemsTheSame(
                oldItem: WatchedEpisodeAndStats,
                newItem: WatchedEpisodeAndStats
            ): Boolean {
                return oldItem.watchedEpisode.id == newItem.watchedEpisode.id
            }

            override fun areContentsTheSame(
                oldItem: WatchedEpisodeAndStats,
                newItem: WatchedEpisodeAndStats
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}