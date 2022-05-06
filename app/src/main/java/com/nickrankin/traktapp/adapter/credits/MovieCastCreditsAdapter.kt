package com.nickrankin.traktapp.adapter.credits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.credits.MovieCastPerson
import com.nickrankin.traktapp.databinding.CreditItemBinding
import com.nickrankin.traktapp.helper.AppConstants


class MovieCastCreditsAdapter(private val glide: RequestManager): ListAdapter<MovieCastPerson, MovieCastCreditsAdapter.CreditsViewHolder>(
    COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditsViewHolder {
        return CreditsViewHolder(CreditItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CreditsViewHolder, position: Int) {
        val currentItem = getItem(position)

        holder.bindings.apply {
            if(currentItem.castPerson.photo_path != null) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.castPerson.photo_path )
                    .into(credititemImage)
            } else {
                credititemImage.setImageResource(R.drawable.ic_baseline_person_24)
            }

            credititemPersonName.text = currentItem.castPerson.name
            credititemPersonRole.text = currentItem.movieCastPersonData.character
        }
    }



    inner class CreditsViewHolder(val bindings: CreditItemBinding): BaseViewHolder(bindings.root)

    // Workaround to support variable height Cast Person elements in the horizontal RecyclerView.
    // https://stackoverflow.com/questions/64504633/horizontal-recyclerview-with-dynamic-item-s-height
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val lp: ViewGroup.LayoutParams = itemView.layoutParams
            if (lp is FlexboxLayoutManager.LayoutParams) {
                lp.flexShrink = 0.0f
                lp.alignSelf =
                    AlignItems.FLEX_START //this will align each itemView on Top or use AlignItems.FLEX_END to align it at Bottom
            }
        }
    }

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<MovieCastPerson>() {
            override fun areItemsTheSame(
                oldItem: MovieCastPerson,
                newItem: MovieCastPerson
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: MovieCastPerson,
                newItem: MovieCastPerson
            ): Boolean {
                return oldItem.movieCastPersonData.id == newItem.movieCastPersonData.id
            }
        }
    }
}