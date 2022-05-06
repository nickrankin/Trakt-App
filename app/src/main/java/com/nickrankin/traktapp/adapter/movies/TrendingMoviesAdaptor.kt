package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.blogc.android.views.ExpandableTextView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.TrendingMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.entities.TrendingMovie

class TrendingMoviesAdaptor(private val tmdbImageLoader: TmdbImageLoader, private val callback: (results: TrendingMovie?) -> Unit): ListAdapter<TrendingMovie, TrendingMoviesAdaptor.ViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TrendingMovieEntryListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            trendingitemPoster.setImageDrawable(null)

            trendingitemTitle.text = currentItem.movie?.title
            trendingitemWatchingTotal.text = "${currentItem?.watchers ?: 0} watching this right now"
            trendingitemOverview.text = currentItem.movie?.overview

            tmdbImageLoader.loadImages(currentItem.movie?.ids?.trakt ?: 0, ImageItemType.MOVIE, currentItem.movie?.ids?.tmdb ?: 0,  currentItem.movie?.title, null, true, trendingitemPoster, trendingitemBackdrop)

            root.setOnClickListener {
                callback(currentItem)
            }

            trendingitemOverview.setOnClickListener { v ->
                val expandableTextView = v as ExpandableTextView

                expandableTextView.toggle()
            }
        }
    }



    inner class ViewHolder(val bindings: TrendingMovieEntryListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<TrendingMovie>() {
            override fun areItemsTheSame(oldItem: TrendingMovie, newItem: TrendingMovie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TrendingMovie, newItem: TrendingMovie): Boolean {
                return  oldItem.movie?.ids?.trakt ?: 0 == newItem.movie?.ids?.trakt ?: 0
            }
        }
    }
}