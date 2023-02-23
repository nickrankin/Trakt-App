package com.nickrankin.traktapp.adapter.similar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.tmdb2.entities.BaseMovie

class SimilarMoviesAdapter constructor(private val posterImageLoader: TmdbImageLoader, private val callback: (selectedItem: BaseMovie) -> Unit): ListAdapter<BaseMovie, SimilarMoviesAdapter.PosterViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        return PosterViewHolder(ViewPosterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            posterImageLoader.loadImages(0, ImageItemType.MOVIE, currentItem.id, currentItem.title, null, true, itemPoster, null, true)

            itemTitle.text = currentItem.title

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    class PosterViewHolder(val bindings: ViewPosterItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<BaseMovie>() {
            override fun areItemsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: BaseMovie, newItem: BaseMovie): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}