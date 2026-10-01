package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminState {
    object Loading : AdminState()
    data class CanteensLoaded(val canteens: List<Canteen>) : AdminState()
    data class MenuLoaded(val canteenId: String, val menuItems: List<MenuItem>) : AdminState()
    data class Error(val message: String) : AdminState()
}

class AdminViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminState>(AdminState.Loading)
    val uiState: StateFlow<AdminState> = _uiState.asStateFlow()

    fun loadCanteens() {
        viewModelScope.launch {
            if (_uiState.value !is AdminState.CanteensLoaded) {
                _uiState.value = AdminState.Loading
            }
            repository.getAssignedCanteens().onSuccess { canteens ->
                _uiState.value = AdminState.CanteensLoaded(canteens)
            }.onFailure {
                _uiState.value = AdminState.Error(it.localizedMessage ?: "Failed to load canteens")
            }
        }
    }

    fun loadMenu(canteenId: String) {
        viewModelScope.launch {
            if (_uiState.value !is AdminState.MenuLoaded) {
                _uiState.value = AdminState.Loading
            }
            repository.getMenuItems(canteenId).onSuccess { items ->
                _uiState.value = AdminState.MenuLoaded(canteenId, items)
            }.onFailure {
                _uiState.value = AdminState.Error(it.localizedMessage ?: "Failed to load menu")
            }
        }
    }

    fun addMenuItem(
        menuItem: MenuItem,
        imageBytes: ByteArray?,
        extension: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            var finalImageUrl: String? = null
            if (imageBytes != null && extension != null) {
                repository.uploadFoodImage(menuItem.canteenId, imageBytes, extension).onSuccess { url ->
                    finalImageUrl = url
                }.onFailure {
                    onError(it.localizedMessage ?: "Failed to upload image")
                    return@launch
                }
            }

            val finalMenuItem = menuItem.copy(imageUrl = finalImageUrl)
            repository.addMenuItem(finalMenuItem).onSuccess {
                loadMenu(menuItem.canteenId)
                onSuccess()
            }.onFailure {
                finalImageUrl?.let { url -> repository.deleteFoodImage(url) }
                onError(it.localizedMessage ?: "Failed to add item")
            }
        }
    }

    fun updateMenuItem(
        menuItem: MenuItem,
        imageBytes: ByteArray?,
        extension: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            var finalImageUrl = menuItem.imageUrl
            var oldImageUrlToClean: String? = null

            if (imageBytes != null && extension != null) {
                repository.uploadFoodImage(menuItem.canteenId, imageBytes, extension).onSuccess { url ->
                    oldImageUrlToClean = finalImageUrl // save old to delete if new one succeeds
                    finalImageUrl = url
                }.onFailure {
                    onError(it.localizedMessage ?: "Failed to upload new image")
                    return@launch
                }
            } else if (imageBytes == null && extension == "remove") {
                // Special case to remove image without replacing
                oldImageUrlToClean = finalImageUrl
                finalImageUrl = null
            }

            val finalMenuItem = menuItem.copy(imageUrl = finalImageUrl)
            repository.updateMenuItem(finalMenuItem).onSuccess {
                loadMenu(menuItem.canteenId)
                onSuccess()
                // Clean up old image if we replaced or removed it
                oldImageUrlToClean?.let { url -> repository.deleteFoodImage(url) }
            }.onFailure {
                // If update failed but we uploaded a new image, delete the newly uploaded image
                if (imageBytes != null && finalImageUrl != null) {
                    repository.deleteFoodImage(finalImageUrl!!)
                }
                onError(it.localizedMessage ?: "Failed to update item")
            }
        }
    }

    fun deleteMenuItem(canteenId: String, item: MenuItem) {
        viewModelScope.launch {
            repository.deleteMenuItem(item.id).onSuccess {
                item.imageUrl?.let { url -> repository.deleteFoodImage(url) }
                loadMenu(canteenId)
            }.onFailure {
                _uiState.value = AdminState.Error(it.localizedMessage ?: "Failed to delete item")
            }
        }
    }

    fun toggleAvailability(canteenId: String, itemId: String, currentAvailability: Boolean) {
        viewModelScope.launch {
            repository.toggleMenuItemAvailability(itemId, !currentAvailability).onSuccess {
                loadMenu(canteenId)
            }.onFailure {
                _uiState.value = AdminState.Error(it.localizedMessage ?: "Failed to toggle availability")
            }
        }
    }

    fun createCanteen(
        name: String,
        location: String,
        isOpen: Boolean,
        imageBytes: ByteArray? = null,
        extension: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            // Removed _uiState.value = AdminState.Loading to prevent screen blink

            // 1. Create the canteen first
            repository.createCanteen(name, location, isOpen).onFailure {
                onError(it.localizedMessage ?: "Failed to create canteen")
                return@launch
            }

            // 2. Upload image and update if an image was selected
            if (imageBytes != null && extension != null) {
                // We fetch the canteens to find the newly created one (matching name & location without image)
                val canteens = repository.getAssignedCanteens().getOrNull()
                val newCanteen = canteens?.lastOrNull { 
                    it.name == name && it.location == location && it.imageUrl == null
                } ?: canteens?.lastOrNull { 
                    it.name == name && it.location == location 
                }

                if (newCanteen != null) {
                    val canteenId = newCanteen.id
                    // Upload to canteen-images/{canteenId}/{uniqueId}.ext
                    repository.uploadCanteenImage(canteenId, imageBytes, extension).onSuccess { url ->
                        // Update public.canteens.image_url
                        repository.updateCanteenImageUrl(canteenId, url)
                    }.onFailure {
                        it.printStackTrace()
                        onError("Canteen created, but image upload failed: ${it.localizedMessage}")
                        // We do not return here, we want to reload canteens so it appears in the list
                    }
                } else {
                    onError("Canteen created, but couldn't retrieve it for image upload.")
                }
            }

            // 3. Reload canteens and exit dialog
            loadCanteens()
            onSuccess()
        }
    }
}
