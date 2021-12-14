package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.WatchedEpisodeEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "WatchedEpisodesPagingAd"
class WatchedEpisodesPagingAdapter(private val sharedPreferences: SharedPreferences, private val imageLoader: PosterImageLoader, private val glide: RequestManager, private val callback: (selectedShow: WatchedEpisode?) -> Unit): PagingDataAdapter<WatchedEpisode, WatchedEpisodesPagingAdapter.WatchedEpisodeViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchedEpisodeViewHolder {
        return WatchedEpisodeViewHolder(WatchedEpisodeEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WatchedEpisodeViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            // clear poster value to prevent flickering overlapping
            watchedentryitemPoster.setImageDrawable(null)

            watchedentryitemTitle.text = currentItem?.episode_title
            watchedentryitemShowTitle.text = currentItem?.show_title
            watchedentryitemSeasonEpisodeNumber.text = "Season ${currentItem?.episode_season} Episode ${currentItem?.episode_number}"
            watchedentryitemWatchedDate.text = "Watched: " + currentItem?.watched_at?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))

            imageLoader.loadImage(currentItem?.show_tmdb_id ?: 0, currentItem?.language, callback = {posterPath ->
                if(posterPath.isNotEmpty()) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterPath)
                        .into(watchedentryitemPoster)
                }
            })

            root.setOnClickListener { callback(currentItem) }
        }

    }


    inner class WatchedEpisodeViewHolder(val bindings: WatchedEpisodeEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedEpisode>() {
            override fun areItemsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}