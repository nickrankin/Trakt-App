package com.nickrankin.traktapp.helper

import com.uwetrottmann.trakt5.enums.SortBy
import com.uwetrottmann.trakt5.enums.SortHow

data class Sorting(var sortBy: String, var sortHow: String) {
    companion object {
        const val SORT_ORDER_ASC = "asc"
        const val SORT_ORDER_DESC = "desc"

        const val SORT_BY_TITLE = "title"
        const val SORT_BY_COLLECTED = "collected"
        const val SORT_BY_WATCHED = "watched"
        const val SORT_BY_YEAR = "year"
        const val SORT_BY_TRACKED_AT = "tracked_on"
        const val SORT_BY_NEXT_AIRING = "next_up"
    }
}