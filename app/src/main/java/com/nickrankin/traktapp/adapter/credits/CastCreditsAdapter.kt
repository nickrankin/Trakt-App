package com.nickrankin.traktapp.adapter.credits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.CreditItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.Credit

class CastCreditsAdapter(private val glide: RequestManager): RecyclerView.Adapter<CastCreditsAdapter.CreditsViewHolder>() {
    private var credits: List<CastMember> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditsViewHolder {
        return CreditsViewHolder(CreditItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CreditsViewHolder, position: Int) {
        val currentItem = credits[position]

        holder.bindings.apply {
            if(currentItem.profile_path?.isNotEmpty() == true) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.profile_path)
                    .into(credititemImage)
            }

            credititemPersonName.text = currentItem.name
            credititemPersonRole.text = currentItem.character
        }
    }

    override fun getItemCount(): Int {
        return credits.size
    }

    fun updateCredits(credits: List<CastMember>) {
        this.credits = credits
        notifyDataSetChanged()
    }

    inner class CreditsViewHolder(val bindings: CreditItemBinding): RecyclerView.ViewHolder(bindings.root)
}