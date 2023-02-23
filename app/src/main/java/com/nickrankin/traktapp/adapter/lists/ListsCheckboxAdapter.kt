package com.nickrankin.traktapp.adapter.lists

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.dao.lists.model.TraktList
import com.nickrankin.traktapp.dao.lists.model.TraktListEntry
import com.nickrankin.traktapp.databinding.CheckboxListItemBinding
import com.nickrankin.traktapp.databinding.DialogListsBinding
import com.uwetrottmann.trakt5.enums.Type

private const val TAG = "ListsCheckboxAdapter"
class ListsCheckboxAdapter(private val currentItemTraktId: Int, private val type: Type, private val onCheckboxChecked: (traktList: TraktList, isChecked: Boolean) -> Unit): ListAdapter<Pair<TraktList, List<TraktListEntry>>, ListsCheckboxAdapter.CheckboxViewHolder>(COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckboxViewHolder {
        return CheckboxViewHolder(
            CheckboxListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CheckboxViewHolder, position: Int) {
        val currentListEntry = getItem(position)

        holder.bindings.apply {
            checkboxlistitemCheckbox.text = currentListEntry.first.name

            checkboxlistitemCheckbox.setOnClickListener {
                val checkbox: CheckBox = it as CheckBox

                // The state of checkbox now..
                val selectedValue = checkbox.isChecked

                // There is a possibility list addition or removal will fail e.g: Network failure, so we wait until new entries are bound to listadapter to display state.
                checkbox.isChecked = !checkbox.isChecked

                onCheckboxChecked(currentListEntry.first, selectedValue)
            }

            // Check if list entry for current item exists in list, if it does we check the checkbox..
            checkboxlistitemCheckbox.isChecked =
                when(type) {
                    Type.MOVIE -> {
                        currentListEntry.second.find { (it.movie?.trakt_id ?: -1) == currentItemTraktId } != null
                    }
                    Type.SHOW -> {
                        currentListEntry.second.find { (it.show?.trakt_id ?: -1) == currentItemTraktId } != null
                    }
                    Type.EPISODE -> {
                        currentListEntry.second.find { (it.episode?.trakt_id ?: -1) == currentItemTraktId } != null
                    }
                    Type.PERSON -> {
                        currentListEntry.second.find { (it.person?.trakt_id ?: -1) == currentItemTraktId } != null
                    }
                    else -> {
                        Log.e(TAG, "onBindViewHolder: Invalid type in this content: Type: $type", )
                        false
                    }
                }
                currentListEntry.second.find { (it.movie?.trakt_id ?: -1) == currentItemTraktId } != null
        }
    }

    inner class CheckboxViewHolder(val bindings: CheckboxListItemBinding): RecyclerView.ViewHolder(bindings.root)

    companion object {
        val COMPARATOR = object: DiffUtil.ItemCallback<Pair<TraktList, List<TraktListEntry>>>() {
            override fun areItemsTheSame(
                oldItem: Pair<TraktList, List<TraktListEntry>>,
                newItem: Pair<TraktList, List<TraktListEntry>>
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: Pair<TraktList, List<TraktListEntry>>,
                newItem: Pair<TraktList, List<TraktListEntry>>
            ): Boolean {
                // We check also if the length of TraktListEntry List is changed. In this case we assume user has checked or unchecked something!, so update the listentry adapter with the changed list entries
                return oldItem.first.trakt_id == newItem.first.trakt_id && oldItem.second.size == newItem.second.size
            }
        }
    }

}