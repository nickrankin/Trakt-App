package com.nickrankin.traktapp.adapter.credits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.credits.model.CreditPerson
import com.nickrankin.traktapp.dao.credits.model.TmCastPerson
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.enums.Type

private const val TAG = "CharacterPosterAdapter"
class CharacterPosterAdapter constructor(private val glide: RequestManager, private val imageLoader: TmdbImageLoader, private val callback: (creditCharacterPerson: CreditPerson) -> Unit): ListAdapter<CreditPerson, CharacterPosterAdapter.CharacterVH>(
    COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterVH {
        return CharacterVH(
            ViewPosterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CharacterVH, position: Int) {
        val currentItem = getItem(position)
        when(currentItem.type) {
            Type.MOVIE -> {
                holder.bindings.apply {
                    itemTitle.text = "${currentItem.title} (${currentItem.year})"

                    imageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.MOVIE,
                        currentItem.tmdb_id,
                        currentItem.title,
                        null,
                        true, itemPoster,
                        null,
                        false
                    )

                    itemTimestamp.visibility = View.VISIBLE

                    if(currentItem is TmCastPerson) {
                        itemTimestamp.text = currentItem.character
                    }

                }
            }
            Type.SHOW -> {
                holder.bindings.apply {
                    itemTitle.text = "${currentItem.title} (${currentItem.year})"

                    imageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.SHOW,
                        currentItem.tmdb_id,
                        currentItem.title,
                        null,
                        true, itemPoster,
                        null,
                        false
                    )

                    itemTimestamp.visibility = View.VISIBLE

                    if(currentItem is TmCastPerson) {
                        itemTimestamp.text = currentItem.character                }
                }
            }
            else -> {
                throw RuntimeException("Cannot display type of $currentItem")
            }
        }

        holder.bindings.root.setOnClickListener {
            callback(currentItem)
        }
    }



    inner class CharacterVH(val bindings: ViewPosterItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<CreditPerson>() {
            override fun areItemsTheSame(
                oldItem: CreditPerson,
                newItem: CreditPerson
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CreditPerson,
                newItem: CreditPerson
            ): Boolean {
                return oldItem.trakt_id == newItem.trakt_id && oldItem.person_trakt_id == newItem.person_trakt_id
            }
        }
    }
}