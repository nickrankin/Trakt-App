package com.nickrankin.traktapp.adapter.search

import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.adapter.shows.WatchedEpisodesPagingAdapter
import com.nickrankin.traktapp.adapter.shows.holder.NetworkStateItemViewHolder

class ShowSearchLoadStateAdapter<T : Any, V : RecyclerView.ViewHolder>(private val adapter: PagingDataAdapter<T, V>): LoadStateAdapter<NetworkStateItemViewHolder>() {
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