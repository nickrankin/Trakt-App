package com.nickrankin.traktapp.adapter.credits

import android.util.Log
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
import com.nickrankin.traktapp.dao.credits.ShowCastPerson
import com.nickrankin.traktapp.databinding.CreditItemBinding
import com.nickrankin.traktapp.helper.AppConstants

private const val TAG = "ShowCastCreditsAdapter"
class ShowCastCreditsAdapter(private val glide: RequestManager, private val callback: (selectedCastPerson: ShowCastPerson) -> Unit): ListAdapter<ShowCastPerson, ShowCastCreditsAdapter.CreditsViewHolder>(
    COMPARATOR) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditsViewHolder {
        return CreditsViewHolder(CreditItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: CreditsViewHolder, position: Int) {

        // Equal height credits
        holder.itemView.post {
            val wMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(holder.itemView.width, View.MeasureSpec.EXACTLY)
            val hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

            holder.itemView.measure(wMeasureSpec, hMeasureSpec)
            if (holder.itemView.measuredHeight > holder.itemView.height) {
                holder.itemView.layoutParams =
                    (holder.itemView.layoutParams as ViewGroup.LayoutParams)
                        .apply {
                            height = holder.itemView.measuredHeight
                        }
            }
        }

        val currentItem = getItem(position)

        holder.bindings.apply {
            if(currentItem?.picture_path != null && currentItem.picture_path.isNotBlank()) {
                glide
                    .load(AppConstants.TMDB_POSTER_URL + currentItem.picture_path )
                    .into(credititemImage)
            } else {
                credititemImage.setImageResource(R.drawable.ic_baseline_person_24)
            }

            credititemPersonName.text = currentItem?.name
            credititemPersonRole.text = currentItem.character

            root.setOnClickListener {
                callback(currentItem)
            }
        }
    }

    inner class CreditsViewHolder(val bindings: CreditItemBinding): RecyclerView.ViewHolder(bindings.root)


    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<ShowCastPerson>() {
            override fun areItemsTheSame(
                oldItem: ShowCastPerson,
                newItem: ShowCastPerson
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: ShowCastPerson,
                newItem: ShowCastPerson
            ): Boolean {
                return oldItem.person_trakt_id == newItem.person_trakt_id
            }
        }
    }
}