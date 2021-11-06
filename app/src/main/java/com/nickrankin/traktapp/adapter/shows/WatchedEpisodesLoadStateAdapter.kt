package com.nickrankin.traktapp.adapter.shows

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import com.nickrankin.traktapp.adapter.shows.holder.NetworkStateItemViewHolder

class WatchedEpisodesLoadStateAdapter(private val adapter: WatchedEpisodesPagingAdapter): LoadStateAdapter<NetworkStateItemViewHolder>() {
    override fun onBindViewHolder(holder: NetworkStateItemViewHolder, loadState: LoadState) {
        holder.bindTo(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): NetworkStateItemViewHolder {
        return NetworkStateItemViewHolder(parent) { adapter.retry() }
    }
}