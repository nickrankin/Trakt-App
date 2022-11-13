package com.nickrankin.traktapp.helper

import com.nickrankin.traktapp.dao.show.model.TrackedShowWithEpisodes
import com.nickrankin.traktapp.model.shows.ShowsTrackingViewModel
import kotlinx.coroutines.channels.Channel

interface ISortable<T> {

    /***
     *
     * @param sortBy The value to sort the list by.
     *
     *
     * */
    fun applySorting(sortBy: String)

    /**
     * @param sortingChannel The Coroutine Channel used to emit new sorting values
     * @param currentSorting The current value in the Sorting channel (sortingflow.value)
     * @param sortBy The new sortby value
     *
     * This method will check if sorting should be applied in Ascending or Descending mode. The provided sorting channel is updated with the latest Sorting value
     *
     * **/
    suspend fun updateSorting(currentSorting: Sorting, sortingChannel: Channel<ISortable.Sorting>, sortBy: String) {
        if (sortBy == currentSorting.sortBy && currentSorting.sortHow == SORT_ORDER_DESC) {
            // Sort Ascending
            sortingChannel.send(Sorting(currentSorting.sortBy, SORT_ORDER_ASC))
        } else if (sortBy == currentSorting.sortBy) {
            // Sort Descending
            sortingChannel.send(getSorting(currentSorting.sortBy, SORT_ORDER_DESC))
        } else {
            sortingChannel.send(getSorting(sortBy, SORT_ORDER_DESC))
        }
    }

    /**
     *
     * @param list The list to sort
     * @return The sorted list
     *
     * Return the sorted list based on the relevant criteria.
     *
     *
     * **/
    fun sortList(list: List<T>,
        sorting: Sorting
    ): List<T>

    fun getSorting(sortBy: String, sortHow: String): Sorting {
        return Sorting(sortBy, sortHow)
    }

    data class Sorting(val sortBy: String, val sortHow: String)

    companion object {
        const val SORT_BY_TITLE = "title"
        const val SORT_BY_YEAR = "year"

        const val SORT_ORDER_ASC = "asc"
        const val SORT_ORDER_DESC = "desc"
    }

}