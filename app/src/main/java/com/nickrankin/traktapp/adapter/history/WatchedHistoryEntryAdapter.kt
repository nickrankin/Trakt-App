package com.nickrankin.traktapp.adapter.history

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.history.model.EpisodeWatchedHistoryEntry
import com.nickrankin.traktapp.dao.history.model.HistoryEntry
import com.nickrankin.traktapp.databinding.LayoutItemPlayBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.getFormattedDateTime

class WatchedHistoryEntryAdapter constructor(val sharedPreferences: SharedPreferences, val onRemovePlayPressed: (historyEntry: HistoryEntry) -> Unit): ListAdapter<HistoryEntry, WatchedHistoryEntryAdapter.HistoryItemViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItemViewHolder {
        return HistoryItemViewHolder(
            LayoutItemPlayBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: HistoryItemViewHolder, position: Int) {
        val currentEntry = getItem(position)

        holder.bindings.apply {
            if(currentEntry is EpisodeWatchedHistoryEntry) {
                itemplayDate.text = "Watched (S${currentEntry.season}E${currentEntry.episode}): ${getFormattedDateTime(currentEntry.watched_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"

            } else {
                itemplayDate.text = "Watched: ${getFormattedDateTime(currentEntry.watched_date, sharedPreferences.getString("date_format", AppConstants.DEFAULT_DATE_FORMAT), sharedPreferences.getString("time_format", AppConstants.DEFAULT_TIME_FORMAT))}"
            }

            itemplayRemove.setOnClickListener {
                onRemovePlayPressed(currentEntry)
            }
        }
    }

    class HistoryItemViewHolder(val bindings: LayoutItemPlayBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<HistoryEntry>() {
            override fun areItemsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return  oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: HistoryEntry, newItem: HistoryEntry): Boolean {
                return oldItem.history_id == newItem.history_id
            }
        }
    }
}