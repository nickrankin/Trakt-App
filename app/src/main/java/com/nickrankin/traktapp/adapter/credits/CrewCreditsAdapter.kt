package com.nickrankin.traktapp.adapter.credits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.databinding.CreditItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.Credit
import com.uwetrottmann.tmdb2.entities.CrewMember

class CrewCreditsAdapter(private val glide: RequestManager): RecyclerView.Adapter<CrewCreditsAdapter.CreditsViewHolder>() {
    private var credits: List<CrewMember> = listOf()

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
            credititemPersonRole.text = currentItem.department
        }
    }

    override fun getItemCount(): Int {
        return credits.size
    }

    fun updateCredits(credits: List<CrewMember>) {
        this.credits = credits
        notifyDataSetChanged()
    }

    inner class CreditsViewHolder(val bindings: CreditItemBinding): RecyclerView.ViewHolder(bindings.root)
}