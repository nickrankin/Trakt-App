package com.nickrankin.traktapp.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.InsetDrawable
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.R
import com.nickrankin.traktapp.adapter.shows.ICON_MARGIN
import com.nickrankin.traktapp.dao.movies.model.WatchedMovieAndStats
import com.nickrankin.traktapp.databinding.ViewCardItemBinding
import com.nickrankin.traktapp.databinding.ViewPosterItemBinding

private const val TAG = "MediaEntryBasePagingAda"
abstract class MediaEntryBasePagingAdapter<T: Any> constructor(protected val controls: AdaptorActionControls<T>?, diffCallback: DiffUtil.ItemCallback<T>): PagingDataAdapter<T, RecyclerView.ViewHolder>(diffCallback) {

    private var currentViewType = VIEW_TYPE_POSTER

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            VIEW_TYPE_POSTER -> {
                return PosterViewHolder(
                    ViewPosterItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_CARD -> {
                return CardViewHolder(
                    ViewCardItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            else -> {
                return PosterViewHolder(
                    ViewPosterItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(holder.absoluteAdapterPosition)

        if (item != null) {
            when (holder) {
                is PosterViewHolder -> {
                    holder.bindings.apply {

                        if (controls != null) {

                            holder.itemView.setOnClickListener {
                                controls.entrySelectedCallback(item)
                            }

                            if (controls.menuResource != null) {

                                root.setOnLongClickListener { clickedView ->
                                    showMenu(item, root, itemPoster, null)

                                    true
                                }

                            }
                        }


                    }
                }
                is CardViewHolder -> {
                    holder.bindings.apply {

                        if (controls != null) {
                            holder.itemView.setOnClickListener {
                                controls.entrySelectedCallback(item)
                            }

                            root.setOnLongClickListener {

                                showMenu(item, root, itemPoster, itemBackdropImageview)

                                true
                            }

                            if (controls.buttonText != null) {
                                buttonControl.visibility = View.VISIBLE
                                buttonControl.icon = controls.buttonIconResource
                                buttonControl.text = controls.buttonText

                                buttonControl.setOnClickListener {
                                    controls.buttonClickedCallback(item)

                                }

                            }
                        }
                    }
                }
                else -> {
                    Log.e(TAG, "onBindViewHolder: Invalid ViewHolder ${holder.javaClass.name}")
                }
            }
        }

    }

    private fun showMenu(selectedItem: T, v: View, posterImageView: ImageView, backdropImageView: ImageView?) {
        if(controls?.menuResource == null) {
            return
        }

        val popup = PopupMenu(v.context!!, v)

        // Inflate both BaseMenu resource items and the provided menu resource
        popup.menuInflater.inflate(controls.menuResource, popup.menu)
        popup.menuInflater.inflate(R.menu.base_popup_menu, popup.menu)

        if(popup.menu is MenuBuilder) {
            val menuBuilder = popup.menu as MenuBuilder
            menuBuilder.setOptionalIconsVisible(true)

            for(item in menuBuilder.visibleItems) {
                val iconMarginPx =
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, ICON_MARGIN.toFloat(), v.context!!.resources.displayMetrics)
                        .toInt()
                if (item.icon != null) {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                        item.icon = InsetDrawable(item.icon, iconMarginPx, 0, iconMarginPx,0)
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

        popup.setOnMenuItemClickListener { menuItem ->

            if(menuItem.itemId == R.id.popupmenu_refresh_image) {
                reloadImages(selectedItem, posterImageView, backdropImageView)
            } else {
                // Respond to menu item click.
                controls.menuItemSelectedCallback(selectedItem, menuItem.itemId)
            }
            true
        }

        popup.setOnDismissListener { popupMenu ->
            popupMenu.dismiss()
            // Respond to popup being dismissed.
        }
        // Show the popup menu.
        popup.show()
    }

    abstract fun reloadImages(selectedItem: T, posterImageView: ImageView, backdropImageView: ImageView?)

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(currentViewType)
        return currentViewType
    }

    fun switchView(viewType: Int) {
        currentViewType = when(viewType) {
            VIEW_TYPE_CARD -> {
                VIEW_TYPE_CARD
            }
            VIEW_TYPE_POSTER -> {
                VIEW_TYPE_POSTER
            }
            else -> {
                throw RuntimeException("Invalid viewtype specified, ($viewType)")
            }
        }
    }



    class PosterViewHolder(val bindings: ViewPosterItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    class CardViewHolder(val bindings: ViewCardItemBinding) :
        RecyclerView.ViewHolder(bindings.root)

    companion object {
        const val VIEW_TYPE_POSTER = 0
        const val VIEW_TYPE_CARD = 1
        const val ACTION_NAVIGATE_SHOW = 0
    }
}