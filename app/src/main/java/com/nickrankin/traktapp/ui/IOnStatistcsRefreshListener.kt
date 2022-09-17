package com.nickrankin.traktapp.ui

import androidx.lifecycle.LiveData
import androidx.work.WorkInfo

interface IOnStatistcsRefreshListener {
    fun onRefresh(isRefreshing: Boolean)
}