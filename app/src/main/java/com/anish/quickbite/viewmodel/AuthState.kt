package com.anish.quickbite.viewmodel

import com.anish.quickbite.data.model.UserProfile

sealed class AuthState {
    object Initializing : AuthState()
    object NotAuthenticated : AuthState()
    object SigningIn : AuthState()
    data class Authenticated(val userProfile: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
    object OAuthCancelled : AuthState()
    object NetworkError : AuthState()
    object SessionRestorationFailed : AuthState()
    object MissingProfile : AuthState()
}
