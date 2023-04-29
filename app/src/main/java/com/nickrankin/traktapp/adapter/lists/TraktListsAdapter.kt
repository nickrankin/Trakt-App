package com.nickrankin.traktapp.adapter.lists

import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.databinding.TraktListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.repo.lists.ListsRepository
import com.uwetrottmann.trakt5.enums.ListPrivacy
import org.threeten.bp.format.DateTimeFormatter

class TraktListsAdapter(private val sharedPreferences: SharedPreferences, private val callback: (action: String, traktList: TraktList) -> Unit): ListAdapter<TraktList, TraktListsAdapter.ViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(TraktListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = getItem(position)

        if(currentItem.trakt_id == ListsRepository.WATCHLIST_ID) {
            holder.bindings.listitemCardview.cardElevation = 20f

            holder.bindings.apply {
                listitemDelete.visibility = View.GONE
                listitemEdit.visibility = View.GONE
                listitemPrivacy.visibility = View.GONE
            }
        } else {
            holder.bindings.listitemCardview.cardElevation = 4f

            holder.bindings.apply {
                listitemDelete.visibility = View.VISIBLE
                listitemEdit.visibility = View.VISIBLE
                listitemPrivacy.visibility = View.VISIBLE
            }
        }

        holder.bindings.apply {
            listitemName.text = currentItem.name
            listitemAuthor.text = "By: ${currentItem.user?.name}"

            listItemOverview.text = currentItem.description

            val privacyBage = listitemPrivacy
            when(currentItem.privacy) {
                ListPrivacy.PRIVATE -> {
                    privacyBage.setBackgroundColor(holder.itemView.context.getColor(R.color.red))
                    privacyBage.text = "Private"
                }
                ListPrivacy.FRIENDS -> {
                    privacyBage.setBackgroundColor(holder.itemView.context.getColor(R.color.black))
                    privacyBage.text = "Friends"
                }
                ListPrivacy.PUBLIC -> {
                    privacyBage.setBackgroundColor(holder.itemView.context.getColor(R.color.blue))
                    privacyBage.text = "Public"
                }
            }

            if(currentItem.created_at != null) {
                listitemCreated.text = "Created: " + currentItem.created_at.format(DateTimeFormatter.ofPattern(sharedPreferences.getString(AppConstants.DATE_FORMAT, "dd/MM/yyyy")))
            }

            if((currentItem.item_count ?: 0) > 999) {
                listItemCount.text = "999+"
            } else {
                listItemCount.text = currentItem.item_count.toString()
            }

            root.setOnClickListener { callback(OPEN_LIST, currentItem) }
            listitemEdit.setOnClickListener { callback(EDIT_LIST, currentItem) }
            listitemDelete.setOnClickListener { callback(DELETE_LIST, currentItem) }
        }
    }

    inner class ViewHolder(val bindings: TraktListItemBinding): RecyclerView.ViewHolder(bindings.root)


    companion object {
        const val OPEN_LIST = "open_list"
        const val EDIT_LIST = "edit_list"
        const val DELETE_LIST = "delete_list"

        private val COMPARATOR = object: DiffUtil.ItemCallback<TraktList>() {
            override fun areItemsTheSame(oldItem: TraktList, newItem: TraktList): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TraktList, newItem: TraktList): Boolean {
                return oldItem.trakt_id == newItem.trakt_id
            }
        }
    }
}