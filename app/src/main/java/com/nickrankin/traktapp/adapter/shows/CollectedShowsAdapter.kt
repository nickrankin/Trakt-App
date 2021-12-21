package com.nickrankin.traktapp.adapter.shows

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.dao.show.model.CollectedShow
import com.nickrankin.traktapp.databinding.CollectedShowEntryListItemBinding
import com.nickrankin.traktapp.helper.AppConstants
import com.nickrankin.traktapp.helper.PosterImageLoader
import org.threeten.bp.format.DateTimeFormatter

private const val TAG = "CollectedShowsAdapter"
const val ICON_MARGIN = 2

class CollectedShowsAdapter(
    private val sharedPreferences: SharedPreferences,
    private val glide: RequestManager,
    private val imageLoader: PosterImageLoader,
    private val callback: (selectedShow: CollectedShow, action: Int) -> Unit
) : ListAdapter<CollectedShow, CollectedShowsAdapter.CollectedShowsViewHolder>(
    COMPARATOR
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectedShowsViewHolder {
        return CollectedShowsViewHolder(
            CollectedShowEntryListItemBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: CollectedShowsViewHolder, position: Int) {
        //holder.setIsRecyclable(false)

        val currentItem = getItem(position)

        holder.bindings.apply {
            collectedentryitemPoster.setImageResource(R.drawable.ic_trakt_svgrepo_com)

            collectedentryitemTitle.text = currentItem.show_title
            collectedentryitemCollectedDate.text = "Collected: " + currentItem.collected_at?.format(
                DateTimeFormatter.ofPattern(
                    sharedPreferences.getString(
                        "date_format",
                        AppConstants.DEFAULT_DATE_TIME_FORMAT
                    )
                )
            )
            collectedentryitemOverview.text = currentItem.show_overview

            imageLoader.loadImage(
                currentItem.show_tmdb_id,
                currentItem.language,
                true,
                callback = { posterPath ->
                    if (posterPath.isNotEmpty()) {
                        glide
                            .load(AppConstants.TMDB_POSTER_URL + posterPath)
                            .into(collectedentryitemPoster)
                    }
                })

            root.setOnClickListener {
                callback(currentItem, ACTION_NAVIGATE_SHOW)
            }

            collectedentryitemMenu.setOnClickListener { v ->
                showPopupMenu(v, currentItem)
            }

        }
    }

    private fun showPopupMenu(view: View, selectedItem: CollectedShow) {
        val context = view.context
        val popup = PopupMenu(context, view)

        popup.menuInflater.inflate(R.menu.collected_shows_popup_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.collectedshowspopup_nav_show -> {
                    callback(selectedItem, ACTION_NAVIGATE_SHOW)
                }
                R.id.collectedshowspopup_remove_show -> {
                    callback(selectedItem, ACTION_REMOVE_COLLECTION)
                }
            }
            Log.e(TAG, "showPopupMenu: Clicked ${item.title} for ${selectedItem.show_title}")
            true
        }

        // Workaround to add menu icons to dropdown list
        // https://www.material.io/components/menus/android#dropdown-menus
        @SuppressLint("RestrictedApi")
        if (popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)
            for (item in menuBuilder.visibleItems) {
                val iconMarginPx =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        ICON_MARGIN.toFloat(),
                        context.resources.displayMetrics
                    ).toInt()
                if (item.icon != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0)
                    } else {
                        item.icon =
                            object : InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx, 0) {
                                override fun getIntrinsicWidth(): Int {
                                    return intrinsicHeight + iconMarginPx + iconMarginPx
                                }
                            }
                    }
                }
            }
        }

        popup.show()
    }

    inner class CollectedShowsViewHolder(val bindings: CollectedShowEntryListItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val ACTION_NAVIGATE_SHOW = 0
        const val ACTION_REMOVE_COLLECTION = 1

        val COMPARATOR = object : DiffUtil.ItemCallback<CollectedShow>() {
            override fun areItemsTheSame(oldItem: CollectedShow, newItem: CollectedShow): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: CollectedShow,
                newItem: CollectedShow
            ): Boolean {
                return oldItem.show_trakt_id == newItem.show_trakt_id
            }
        }
    }
}