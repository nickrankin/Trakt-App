package com.nickrankin.traktapp.adapter.shows

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.show.model.TrackedEpisode
import com.nickrankin.traktapp.databinding.ShowLayoutItemCondensedBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.TmdbImageLoader
import org.threeten.bp.format.DateTimeFormatter

class TrackedEpisodesAdapter(private val sharedPreferences: SharedPreferences, private val tmdbImageLoader: TmdbImageLoader, private val callback: (trackedEpisode: TrackedEpisode) -> Unit): ListAdapter<TrackedEpisode, TrackedEpisodesAdapter.TrackedEpisodesViewHolder>(COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackedEpisodesViewHolder {
        return TrackedEpisodesViewHolder(ShowLayoutItemCondensedBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: TrackedEpisodesViewHolder, position: Int) {

        val currentEpisode = getItem(position)

            holder.bindings.apply {
                collectedshowlayoutTitle.text = currentEpisode.title + " (S${currentEpisode.season}E${currentEpisode.episode})"
                collectedshowlayoutCollectedAy.text = "Airs: ${currentEpisode.airs_date?.format(DateTimeFormatter.ofPattern(sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_TIME_FORMAT)))} on ${ currentEpisode?.network ?: "Unknown" }"

                root.setOnClickListener {
                    callback(currentEpisode)
                }

            }

    }

    inner class TrackedEpisodesViewHolder(val bindings: ShowLayoutItemCondensedBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TrackedEpisode>() {
            override fun areItemsTheSame(
                oldItem: TrackedEpisode,
                newItem: TrackedEpisode
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: TrackedEpisode,
                newItem: TrackedEpisode
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }
    }
}