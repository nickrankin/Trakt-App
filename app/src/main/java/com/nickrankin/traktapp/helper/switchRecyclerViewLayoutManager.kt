package com.nickrankin.traktapp.helper

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nickrankin.traktapp.adapter.MediaEntryBasePagingAdapter
private const val DEFAULT_CAST_WIDTH = 85
private const val DEFAULT_POSTER_WIDTH = 170
fun switchRecyclerViewLayoutManager(context: Context, recyclerView: RecyclerView, viewType: Int) {
    var lm: GridLayoutManager? = null

    when(viewType) {
        MediaEntryBasePagingAdapter.VIEW_TYPE_POSTER -> {
            val displayMetrics = context.resources?.displayMetrics
            val screenWidthDp = displayMetrics?.widthPixels?.div(displayMetrics.density)

            if (screenWidthDp != null) {
                lm = GridLayoutManager(context, (screenWidthDp / DEFAULT_POSTER_WIDTH).toInt())
            } else {
                GridLayoutManager(context, 1)
            }
        }
        MediaEntryBasePagingAdapter.VIEW_TYPE_CARD -> {
            lm = GridLayoutManager(context, 1)
        }
        else -> {
            lm = GridLayoutManager(context, 1)
        }
    }
    recyclerView.layoutManager = lm
}

fun getResponsiveGridLayoutManager(context: Context, size: Int?):GridLayoutManager {
    var lm: GridLayoutManager? = null

    val displayMetrics = context.resources?.displayMetrics
    val screenWidthDp = displayMetrics?.widthPixels?.div(displayMetrics.density)

    if (screenWidthDp != null) {
        lm = GridLayoutManager(context, (screenWidthDp / (size ?: DEFAULT_POSTER_WIDTH)).toInt())
    } else {
        GridLayoutManager(context, 1)
    }

    return lm!!
}