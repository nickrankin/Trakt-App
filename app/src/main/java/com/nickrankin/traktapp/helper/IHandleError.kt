package com.nickrankin.traktapp.helper

import android.view.View

interface IHandleError {
    fun showErrorSnackbarRetryButton(throwable: Throwable?, view: View, retryCallback: () -> Unit)

    fun showErrorMessageToast(throwable: Throwable?, customMessage: String)
}