package com.anish.quickbite.viewmodel

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.repository.AuthRepository
import com.anish.quickbite.data.repository.AuthRepositoryImpl
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            authRepository.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val userId = authRepository.currentUserId()
                        if (userId != null) {
                            loadProfile(userId)
                        } else {
                            _authState.value = AuthState.SessionRestorationFailed
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (_authState.value !is AuthState.SigningIn) {
                            _authState.value = AuthState.NotAuthenticated
                        }
                    }
                    is SessionStatus.Initializing -> {
                        _authState.value = AuthState.Initializing
                    }
                    else -> {
                        _authState.value = AuthState.NotAuthenticated
                    }
                }
            }
        }
    }

    private suspend fun loadProfile(userId: String) {
        val result = authRepository.getUserProfile(userId)
        result.onSuccess { profile ->
            if (profile != null) {
                _authState.value = AuthState.Authenticated(profile)
            } else {
                // Profile missing in PostgreSQL -> Create initial STUDENT profile
                createInitialProfile(userId)
            }
        }.onFailure { throwable ->
            if (throwable is IOException) {
                _authState.value = AuthState.NetworkError
            } else {
                _authState.value = AuthState.Error(throwable.localizedMessage ?: "Failed to load user profile")
            }
        }
    }

    private suspend fun createInitialProfile(userId: String) {
        val createResult = authRepository.createInitialProfileIfMissing(userId)
        createResult.onSuccess { newProfile ->
            _authState.value = AuthState.Authenticated(newProfile)
        }.onFailure { throwable ->
            _authState.value = AuthState.MissingProfile
        }
    }

    fun signInWithGoogleNative(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.SigningIn
            val result = authRepository.signInWithGoogleNative(context)
            result.onFailure { throwable ->
                val message = throwable.localizedMessage ?: ""
                when {
                    throwable is GetCredentialCancellationException || message.contains("cancel", ignoreCase = true) -> {
                        _authState.value = AuthState.OAuthCancelled
                    }
                    throwable is IOException -> {
                        _authState.value = AuthState.NetworkError
                    }
                    else -> {
                        _authState.value = AuthState.Error(
                            if (message.isBlank()) "Authentication failed. Please check your Google and Supabase configuration." else message
                        )
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.NotAuthenticated
        }
    }

    fun resetErrorState() {
        if (_authState.value is AuthState.Error ||
            _authState.value is AuthState.OAuthCancelled ||
            _authState.value is AuthState.NetworkError ||
            _authState.value is AuthState.MissingProfile ||
            _authState.value is AuthState.SessionRestorationFailed
        ) {
            _authState.value = AuthState.NotAuthenticated
        }
    }
}
