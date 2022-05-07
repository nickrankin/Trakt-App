package com.nickrankin.traktapp.adapter.shows

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.databinding.ReccomendedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.Show

class RecommendedShowsAdapter(private val tmdbImageLoader: TmdbImageLoader, private val callback: (results: Show?, action: Int, position: Int) -> Unit): ListAdapter<Show, RecommendedShowsAdapter.ViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ReccomendedShowEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            val expandableTextView = collectedentryitemOverview
            expandableTextView.collapse()

            collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
            collectedentryitemBackdrop.setImageResource(R.drawable.ic_baseline_tv_24)

            collectedentryitemTitle.text = currentItem?.title
            collectedentryitemCollectedDate.visibility = View.GONE
            collectedentryitemOverview.text = currentItem?.overview

            tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.SHOW,currentItem?.ids?.tmdb ?: 0,currentItem?.title ?: "", currentItem?.year, currentItem.language, false, collectedentryitemPoster, collectedentryitemBackdrop)

            root.setOnClickListener {
                callback(currentItem, ACTION_VIEW, position)
            }

            collectedentryitemRemovePlayBtn.setOnClickListener {
                callback(currentItem, ACTION_REMOVE, position)
            }

            collectedentryitemOverview.setOnClickListener {
                expandableTextView.toggle()
            }
        }
    }

    inner class ViewHolder(val bindings: ReccomendedShowEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_VIEW = 0
        const val ACTION_REMOVE = 1

        val COMPARATOR = object: DiffUtil.ItemCallback<Show>() {
            override fun areItemsTheSame(oldItem: Show, newItem: Show): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Show, newItem: Show): Boolean {
                return  oldItem.ids?.trakt ?: 0 == newItem.ids?.trakt ?: 0
            }
        }
    }
}