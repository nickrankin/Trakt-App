package com.nickrankin.traktapp.adapter.movies

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.movies.model.CollectedMovie
import com.nickrankin.traktapp.databinding.MoviePosterItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader


private const val TAG = "CollectedMoviesAdapter"
class CollectedMoviesAdapter(private val glide: RequestManager,
                             private val imageLoader: PosterImageLoader,
                             private val callback: (selectedShow: CollectedMovie, action: Int) -> Unit
) : ListAdapter<CollectedMovie, CollectedMoviesAdapter.ViewHolder>(
    COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedMoviesAdapter.ViewHolder {
        return ViewHolder(
            MoviePosterItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CollectedMoviesAdapter.ViewHolder, position: Int) {
        //holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        holder.bindings.apply {
           movieitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)
//
            movieitemTitle.text = currentItem.title

//
                imageLoader.loadMoviePosterImage(
                    currentItem.trakt_id,
                    currentItem.tmdb_id,
                    currentItem?.language,
                    true,
                    callback = { posterImage ->
                        if (posterImage.poster_path != null && posterImage.trakt_id == currentItem.trakt_id) {
                            glide
                                .load(AppConstants.TMDB_POSTER_URL + posterImage.poster_path)
                                .placeholder(R.drawable.ic_trakt_svgrepo_com)
//                                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                                .skipMemoryCache(true)
                                .into(movieitemPoster)
                        }
                    })



            root.setOnClickListener {
                callback(currentItem, ACTION_NAVIGATE_SHOW)
            }
//
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        // Prevent Glide flickering images
        // https://github.com/bumptech/glide/issues/729
        val p: ViewGroup = holder.bindings.root
        val count = p.childCount
        for (i in 0 until count) {
            val child = p.getChildAt(i)
            // optionally you can check `if (child instanceof ImageView)` if you have many children
            // but Glide should handle non-loaded/non-image Views
            if(child is ImageView) {
                glide.clear(child) // stop loading and get rid of loaded image
            }
        }
        //p.removeAllViews()
    }

    inner class ViewHolder(val bindings: MoviePosterItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_COLLECTION = 1

        val COMPARATOR = object : DiffUtil.ItemCallback<CollectedMovie>() {
            override fun areItemsTheSame(oldItem: CollectedMovie, newItem: CollectedMovie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CollectedMovie,
                newItem: CollectedMovie
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }
    }
}