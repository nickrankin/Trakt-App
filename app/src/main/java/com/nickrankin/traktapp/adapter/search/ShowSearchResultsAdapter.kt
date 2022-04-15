package com.nickrankin.traktapp.adapter.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.uwetrottmann.trakt5.entities.SearchResult

class ShowSearchResultsAdapter(private val glide: RequestManager, private val imageLoader: PosterImageLoader, private val callback: (results: SearchResult?) -> Unit): PagingDataAdapter<SearchResult, ShowSearchResultsAdapter.ViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(CollectedShowEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentSearchItem = getItem(position)

        holder.bindings.apply {
            collectedentryitemPoster.setImageDrawable(null)

            collectedentryitemTitle.text = currentSearchItem?.show?.title
            collectedentryitemCollectedDate.visibility = View.GONE
            collectedentryitemOverview.text = currentSearchItem?.show?.overview

            imageLoader.loadShowPosterImage(currentSearchItem?.show?.ids?.trakt ?: 0, currentSearchItem?.show?.ids?.tmdb ?: 0, currentSearchItem?.show?.language,currentSearchItem?.show?.title ?: "", currentSearchItem?.show?.year, false, callback = { posterResponse ->
                if(posterResponse.poster_path != null) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterResponse.poster_path)
                        .into(collectedentryitemPoster)
                }
            })

            root.setOnClickListener {
                callback(currentSearchItem)
            }
        }
    }



    inner class ViewHolder(val bindings: CollectedShowEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

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