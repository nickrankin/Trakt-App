package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.ReccomendedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.Show

class ReccomendedMoviesAdaptor(private val glide: RequestManager, private val imageLoader: PosterImageLoader, private val callback: (results: Movie?) -> Unit): ListAdapter<Movie, ReccomendedMoviesAdaptor.ViewHolder>(
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

            imageLoader.loadMoviePosterImage(currentItem?.ids?.trakt ?: 0, currentItem?.ids?.tmdb ?: 0, currentItem?.language, true, callback = { posterImage ->
                if(posterImage.poster_path != null && posterImage.trakt_id == currentItem.ids?.trakt) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterImage.poster_path)
                        .into(collectedentryitemPoster)
                }
            })

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }



    inner class ViewHolder(val bindings: ReccomendedShowEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
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