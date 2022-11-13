package com.nickrankin.traktapp.adapter.credits

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.dao.credits.MovieCastPerson
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.dao.credits.model.CreditCharacterPerson
import com.nickrankin.traktapp.databinding.LayoutCharacterPosterItemBinding
import com.nickrankin.traktapp.helper.ImageItemType
import com.nickrankin.traktapp.helper.TmdbImageLoader
import com.uwetrottmann.trakt5.enums.Type

private const val TAG = "CharacterPosterAdapter"
class CharacterPosterAdapter constructor(private val glide: RequestManager, private val imageLoader: TmdbImageLoader, private val callback: (creditCharacterPerson: CreditCharacterPerson) -> Unit): ListAdapter<CreditCharacterPerson, CharacterPosterAdapter.CharacterVH>(
    COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterVH {
        return CharacterVH(
            LayoutCharacterPosterItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CharacterVH, position: Int) {
        val currentItem = getItem(position)
        when(currentItem.type) {
            Type.MOVIE -> {
                holder.bindings.apply {
                    characterposterlayoutTitle.text = "${currentItem.title} (${currentItem.year})"

                    imageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.MOVIE,
                        currentItem.tmdb_id,
                        currentItem.title,
                        null,
                        true, characterposterlayoutPoster,
                        null,
                        false
                    )

                    characterposterlayoutCharacter.text = currentItem.character
                }
            }
            Type.SHOW -> {
                holder.bindings.apply {
                    characterposterlayoutTitle.text = "${currentItem.title} (${currentItem.year})"

                    imageLoader.loadImages(
                        currentItem.trakt_id,
                        ImageItemType.SHOW,
                        currentItem.tmdb_id,
                        currentItem.title,
                        null,
                        true, characterposterlayoutPoster,
                        null,
                        false
                    )

                    characterposterlayoutCharacter.text = currentItem.character
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



    inner class CharacterVH(val bindings: LayoutCharacterPosterItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        private val COMPARATOR = object: DiffUtil.ItemCallback<CreditCharacterPerson>() {
            override fun areItemsTheSame(
                oldItem: CreditCharacterPerson,
                newItem: CreditCharacterPerson
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CreditCharacterPerson,
                newItem: CreditCharacterPerson
            ): Boolean {
                return oldItem.trakt_id_person_id == newItem.trakt_id_person_id
            }
        }
    }
}