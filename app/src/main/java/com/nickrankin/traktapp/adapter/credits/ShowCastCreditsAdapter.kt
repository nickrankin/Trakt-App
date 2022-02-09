package com.nickrankin.traktapp.adapter.credits

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayoutManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.databinding.CreditItemBinding
import com.nickrankin.traktapp.helper.AppConstants


class ShowCastCreditsAdapter(private val glide: RequestManager): RecyclerView.Adapter<ShowCastCreditsAdapter.CreditsViewHolder>() {
    private var credits: List<ShowCastPerson> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditsViewHolder {
        return CreditsViewHolder(CreditItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CreditsViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        val currentItem = credits[position]

        holder.bindings.apply {
            if(currentItem.castPerson.photo_path != null) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.castPerson.photo_path )
                    .into(credititemImage)
            } else {
                credititemImage.setImageResource(R.drawable.ic_baseline_person_24)
            }

            credititemPersonName.text = currentItem.castPerson.name
            credititemPersonRole.text = currentItem.showCastPersonData.character
        }
    }

    override fun getItemCount(): Int {
        return credits.size
    }

    fun updateCredits(credits: List<ShowCastPerson>) {
        this.credits = credits
        notifyDataSetChanged()
    }

    inner class CreditsViewHolder(val bindings: CreditItemBinding): BaseViewHolder(bindings.root)

    // Workaround to support variable height Cast Person elements in the horizontal RecyclerView.
    // https://stackoverflow.com/questions/64504633/horizontal-recyclerview-with-dynamic-item-s-height
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val lp: ViewGroup.LayoutParams = itemView.getLayoutParams()
            if (lp is FlexboxLayoutManager.LayoutParams) {
                val flexboxLp = lp
                flexboxLp.flexShrink = 0.0f
                flexboxLp.alignSelf =
                    AlignItems.FLEX_START //this will align each itemView on Top or use AlignItems.FLEX_END to align it at Bottom
            }
        }
    }
}