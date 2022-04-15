package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.ReccomendedShowEntryListItemBinding
import com.nickrankin.traktapp.databinding.TrendingMovieEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import com.uwetrottmann.trakt5.entities.Movie
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.TrendingMovie

class TrendingMoviesAdaptor(private val glide: RequestManager, private val imageLoader: PosterImageLoader, private val callback: (results: TrendingMovie?) -> Unit): ListAdapter<TrendingMovie, TrendingMoviesAdaptor.ViewHolder>(
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

            imageLoader.loadMoviePosterImage(currentItem.movie?.ids?.trakt ?: 0, currentItem.movie?.ids?.tmdb ?: 0, currentItem.movie?.language, true, callback = { posterImage ->
                if(posterImage.poster_path != null && posterImage.trakt_id == currentItem.movie.ids?.trakt) {
                    glide
                        .load(AppConstants.TMDB_POSTER_URL + posterImage.poster_path)
                        .into(trendingitemPoster)
                }
            })

            root.setOnClickListener {
                callback(currentItem)
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