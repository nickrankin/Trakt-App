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
import com.uwetrottmann.tmdb2.entities.BaseTvShow

class SimilarShowsAdapter constructor(private val posterImageLoader: TmdbImageLoader, private val callback: (selectedItem: BaseTvShow) -> Unit): ListAdapter<BaseTvShow, SimilarShowsAdapter.PosterViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        return PosterViewHolder(ViewPosterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            posterImageLoader.loadImages(0, ImageItemType.SHOW, currentItem.id, currentItem.name, null, true, itemPoster, null, true)

            itemTitle.text = currentItem.name

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    class PosterViewHolder(val bindings: ViewPosterItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<BaseTvShow>() {
            override fun areItemsTheSame(oldItem: BaseTvShow, newItem: BaseTvShow): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: BaseTvShow, newItem: BaseTvShow): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}