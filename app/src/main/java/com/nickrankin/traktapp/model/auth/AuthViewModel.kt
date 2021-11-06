package com.nickrankin.traktapp.model.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nickrankin.traktapp.helper.Resource
import com.nickrankin.traktapp.repo.auth.AuthRepository
import com.uwetrottmann.trakt5.entities.AccessToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val authRepository: AuthRepository): ViewModel() {

    private val refreshEventChannel: Channel<Boolean> = Channel()
    private val refreshEvent = refreshEventChannel.receiveAsFlow()

    suspend fun exchangeCodeForAccessToken(code: String, isStaging: Boolean) = authRepository.exchangeCodeForAccessToken(code, isStaging)

    suspend fun getUserSlug() = authRepository.getUserSlug()

    @ExperimentalCoroutinesApi
    val userProfile = refreshEvent.flatMapLatest { shouldRefresh ->
        authRepository.getUserSettings(shouldRefresh)
    }

    fun onStart() {
        viewModelScope.launch {
            refreshEventChannel.send(false)
        }
    }
    fun onRefresh() {
        viewModelScope.launch {
            refreshEventChannel.send(true)
        }
    }
}