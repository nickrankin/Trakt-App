package com.nickrankin.traktapp.adapter.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.databinding.ShowSearchResultListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.SearchResult

class ShowSearchResultsAdapter(private val tmdbImageLoader: TmdbImageLoader, private val callback: (results: SearchResult?) -> Unit): PagingDataAdapter<SearchResult, ShowSearchResultsAdapter.ViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ShowSearchResultListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSearchItem = getItem(position) ?: return

        when(currentSearchItem?.type) {
            "movie" -> {
                holder.bindings.apply {
                    val expandableTextView = collectedentryitemOverview
                    expandableTextView.collapse()

                    collectedentryitemBackdrop.setImageResource(R.drawable.ic_baseline_local_movies_24)
                    collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

                    collectedentryitemTitle.text = currentSearchItem.movie?.title
                    collectedentryitemCollectedDate.visibility = View.GONE
                    collectedentryitemOverview.text = currentSearchItem.movie?.overview

                    tmdbImageLoader.loadImages(currentSearchItem.movie?.ids?.trakt ?: 0, ImageItemType.MOVIE, currentSearchItem.movie?.ids?.tmdb ?: 0, currentSearchItem.movie?.title ?: "", currentSearchItem.movie?.language, false, collectedentryitemPoster, collectedentryitemBackdrop, false)

                    root.setOnClickListener {
                        callback(currentSearchItem)
                    }

                    collectedentryitemOverview.setOnClickListener {
                        expandableTextView.toggle()
                    }
                }
            }
            "show" -> {
                holder.bindings.apply {
                    val expandableTextView = collectedentryitemOverview
                    expandableTextView.collapse()

                    collectedentryitemBackdrop.setImageResource(R.drawable.ic_baseline_tv_24)
                    collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

                    collectedentryitemTitle.text = currentSearchItem.show?.title
                    collectedentryitemCollectedDate.visibility = View.GONE
                    collectedentryitemOverview.text = currentSearchItem.show?.overview

                    tmdbImageLoader.loadImages(currentSearchItem.show?.ids?.trakt ?: 0, ImageItemType.SHOW, currentSearchItem.show?.ids?.tmdb ?: 0, currentSearchItem.show?.title ?: "", currentSearchItem.show?.language, false, collectedentryitemPoster, collectedentryitemBackdrop, false)

                    root.setOnClickListener {
                        callback(currentSearchItem)
                    }

                    collectedentryitemOverview.setOnClickListener {
                        expandableTextView.toggle()
                    }
                }
            }
        }

    }

    inner class ViewHolder(val bindings: ShowSearchResultListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<SearchResult>() {
            override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult): Boolean {
                return  oldItem.show?.ids?.trakt ?: 0 == newItem.show?.ids?.trakt ?: 0
            }
        }
    }
}