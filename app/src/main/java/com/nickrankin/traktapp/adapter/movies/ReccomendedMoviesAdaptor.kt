package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.ReccomendedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.Movie

class ReccomendedMoviesAdaptor(private val tmdbImageLoader: TmdbImageLoader, private val callback: (pos: Int, action: Int, results: Movie?) -> Unit): ListAdapter<Movie, ReccomendedMoviesAdaptor.ViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ReccomendedShowEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            collectedentryitemPoster.setImageDrawable(null)

            collectedentryitemTitle.text = currentItem?.title
            collectedentryitemCollectedDate.visibility = View.GONE
            collectedentryitemOverview.text = currentItem?.overview

            tmdbImageLoader.loadImages(currentItem?.ids?.trakt ?: 0, ImageItemType.MOVIE,currentItem?.ids?.tmdb ?: 0,  currentItem.title, null, currentItem.language, true, collectedentryitemPoster, collectedentryitemBackdrop)

            collectedentryitemOverview.setOnClickListener { v ->
                val expandingTextView = v as ExpandableTextView

                expandingTextView.toggle()
            }


            root.setOnClickListener {
                callback(position, ACTION_VIEW, currentItem)
            }

            collectedentryitemRemovePlayBtn.setOnClickListener {
                callback(position, ACTION_REMOVE, currentItem)
            }
        }
    }

    inner class ViewHolder(val bindings: ReccomendedShowEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_VIEW = 0
        const val ACTION_REMOVE = 1
        val COMPARATOR = object: DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
                return  oldItem.ids?.trakt ?: 0 == newItem.ids?.trakt ?: 0
            }
        }
    }
}