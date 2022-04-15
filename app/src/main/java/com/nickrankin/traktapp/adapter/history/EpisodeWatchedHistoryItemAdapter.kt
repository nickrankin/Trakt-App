package com.nickrankin.traktapp.adapter.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.show.model.WatchedEpisode
import com.nickrankin.traktapp.databinding.WatchedHistoryItemLayoutBinding
import com.nickrankin.traktapp.helper.AppConstants
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

class EpisodeWatchedHistoryItemAdapter(private val callback: (WatchedEpisode) -> Unit): ListAdapter<WatchedEpisode, EpisodeWatchedHistoryItemAdapter.WatchedHistoryItemViewHolder>(
    COMPARATOR) {


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WatchedHistoryItemViewHolder {
        return WatchedHistoryItemViewHolder(WatchedHistoryItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: WatchedHistoryItemViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            watchedHistoryItemTitle.text = currentItem.episode_title + "(S${currentItem?.episode_season}E${currentItem?.episode_number})"
            watchedHistoryItemWatchedAt.text = "Last Watched at: " + currentItem.watched_at?.atZoneSameInstant(
                ZoneId.systemDefault())?.format(
                DateTimeFormatter.ofPattern(AppConstants.DEFAULT_DATE_TIME_FORMAT))
            root.setOnClickListener { callback(currentItem) }
        }
    }

    inner class WatchedHistoryItemViewHolder(val bindings: WatchedHistoryItemLayoutBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<WatchedEpisode>() {
            override fun areItemsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: WatchedEpisode,
                newItem: WatchedEpisode
            ): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}