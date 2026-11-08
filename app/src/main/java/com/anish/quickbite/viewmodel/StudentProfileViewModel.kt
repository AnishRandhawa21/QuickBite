package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.model.UserProfile
import com.anish.quickbite.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StudentProfileUiState {
    object Loading : StudentProfileUiState()
    data class Success(val profile: UserProfile) : StudentProfileUiState()
    data class Error(val message: String) : StudentProfileUiState()
}

class StudentProfileViewModel(
    private val repository: StudentRepository = StudentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StudentProfileUiState>(StudentProfileUiState.Loading)
    val uiState: StateFlow<StudentProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = StudentProfileUiState.Loading
            repository.getCurrentUserProfile().onSuccess { profile ->
                if (profile != null) {
                    _uiState.value = StudentProfileUiState.Success(profile)
                } else {
                    _uiState.value = StudentProfileUiState.Error("Profile not found")
                }
            }.onFailure { err ->
                _uiState.value = StudentProfileUiState.Error(err.localizedMessage ?: "Failed to load profile")
            }
        }
    }
}
