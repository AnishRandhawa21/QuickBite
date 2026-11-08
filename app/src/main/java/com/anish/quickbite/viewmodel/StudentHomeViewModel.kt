package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.local.entity.CartItemEntity
import com.anish.quickbite.data.local.entity.RecentlyViewedEntity
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.repository.CartRepository
import com.anish.quickbite.data.repository.FavoriteRepository
import com.anish.quickbite.data.repository.RecentlyViewedRepository
import com.anish.quickbite.data.repository.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentHomeUiState(
    val isLoadingCanteens: Boolean = true,
    val canteens: List<Canteen> = emptyList(),
    val selectedCanteen: Canteen? = null,
    val isLoadingMenu: Boolean = false,
    val isRefreshingMenu: Boolean = false,
    val menuItems: List<MenuItem> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val isOffline: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val cartItems: List<CartItemEntity> = emptyList(),
    val cartItemCount: Int = 0,
    val favoriteItemIds: Set<String> = emptySet(),
    val recentlyViewed: List<RecentlyViewedEntity> = emptyList()
) {
    val cartTotal: Double
        get() = cartItems.sumOf { it.price * it.quantity }

    val categories: List<String>
        get() {
            val list = menuItems.map { it.category.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            val base = listOf("All")
            return if (favoriteItemIds.isNotEmpty()) {
                base + listOf("Favorites") + list
            } else {
                base + list
            }
        }

    val filteredItems: List<MenuItem>
        get() {
            return menuItems.filter { item ->
                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Favorites" -> favoriteItemIds.contains(item.id)
                    else -> item.category.equals(selectedCategory, ignoreCase = true)
                }
                val matchesQuery = searchQuery.isBlank() ||
                        item.name.contains(searchQuery, ignoreCase = true) ||
                        item.description.contains(searchQuery, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }
}

class StudentHomeViewModel(
    private val repository: StudentRepository = StudentRepository(),
    private val cartRepository: CartRepository = CartRepository(),
    private val favoriteRepository: FavoriteRepository = FavoriteRepository(),
    private val recentlyViewedRepository: RecentlyViewedRepository = RecentlyViewedRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentHomeUiState())
    val uiState: StateFlow<StudentHomeUiState> = _uiState.asStateFlow()

    private var menuObservationJob: Job? = null
    private var menuSyncJob: Job? = null

    init {
        loadCanteens()
        observeCart()
        observeFavorites()
        observeRecentlyViewed()
    }

    private fun observeCart() {
        viewModelScope.launch {
            cartRepository.getCartItemsFlow().collect { items ->
                val count = items.sumOf { it.quantity }
                _uiState.update { it.copy(cartItems = items, cartItemCount = count) }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteRepository.getFavoritesFlow().collect { favorites ->
                val ids = favorites.map { it.menuItemId }.toSet()
                _uiState.update { it.copy(favoriteItemIds = ids) }
            }
        }
    }

    private fun observeRecentlyViewed() {
        viewModelScope.launch {
            recentlyViewedRepository.getRecentlyViewedFlow(limit = 10).collect { items ->
                _uiState.update { it.copy(recentlyViewed = items) }
            }
        }
    }

    fun loadCanteens() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCanteens = true, errorMessage = null) }
            repository.getCanteens().onSuccess { canteens ->
                val defaultSelected = canteens.firstOrNull { it.isOpen } ?: canteens.firstOrNull()
                _uiState.update {
                    it.copy(
                        isLoadingCanteens = false,
                        canteens = canteens,
                        selectedCanteen = defaultSelected
                    )
                }
                defaultSelected?.let { loadMenu(it.id) }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isLoadingCanteens = false,
                        errorMessage = err.localizedMessage ?: "Failed to load canteens"
                    )
                }
            }
        }
    }

    fun selectCanteen(canteen: Canteen) {
        if (_uiState.value.selectedCanteen?.id == canteen.id) return
        _uiState.update {
            it.copy(
                selectedCanteen = canteen,
                selectedCategory = "All",
                searchQuery = ""
            )
        }
        loadMenu(canteen.id)
    }

    fun loadMenu(canteenId: String) {
        menuObservationJob?.cancel()
        menuSyncJob?.cancel()

        // 1. Observe local Room Database through Flow
        menuObservationJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMenu = it.menuItems.isEmpty(), errorMessage = null) }
            repository.getMenuItemsFlow(canteenId).collect { cachedItems ->
                _uiState.update { current ->
                    current.copy(
                        menuItems = cachedItems,
                        isLoadingMenu = false
                    )
                }
            }
        }

        // 2. Concurrently synchronize with Supabase
        menuSyncJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingMenu = true) }
            val syncResult = repository.syncMenu(canteenId)
            syncResult.onSuccess {
                _uiState.update {
                    it.copy(
                        isRefreshingMenu = false,
                        isOffline = false,
                        errorMessage = null
                    )
                }
            }.onFailure { _ ->
                _uiState.update { current ->
                    current.copy(
                        isRefreshingMenu = false,
                        isOffline = true,
                        errorMessage = if (current.menuItems.isEmpty()) {
                            "Offline: Unable to load menu and no cached items found."
                        } else null
                    )
                }
            }
        }
    }

    fun refreshMenu() {
        val currentCanteen = _uiState.value.selectedCanteen ?: return
        loadMenu(currentCanteen.id)
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    // Cart Operations (Persistent in Room)
    fun addToCart(item: MenuItem, quantity: Int = 1) {
        viewModelScope.launch {
            cartRepository.addToCart(item, quantity)
        }
    }

    fun updateCartQuantity(menuItemId: String, newQuantity: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(menuItemId, newQuantity)
        }
    }

    fun removeFromCart(menuItemId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(menuItemId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            cartRepository.clearCart()
        }
    }

    // Favorite Operations (Persistent in Room)
    fun toggleFavorite(item: MenuItem) {
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(item)
        }
    }

    // Recently Viewed Operations (Persistent in Room)
    fun recordRecentlyViewed(item: MenuItem) {
        viewModelScope.launch {
            recentlyViewedRepository.recordView(item)
        }
    }

    // Real Order Placement (Authoritative on Supabase, Clears Room Cart & Syncs)
    fun placeOrder(
        onSuccess: (com.anish.quickbite.data.model.Order) -> Unit,
        onError: (String) -> Unit
    ) {
        val selectedCanteen = _uiState.value.selectedCanteen
            ?: _uiState.value.canteens.firstOrNull()
            ?: return onError("No canteen selected")

        val currentCart = _uiState.value.cartItems
        if (currentCart.isEmpty()) {
            return onError("Your cart is empty")
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errorMessage = null) }
            repository.placeOrder(selectedCanteen.id, currentCart)
                .onSuccess { order ->
                    _uiState.update { it.copy(isPlacingOrder = false) }
                    onSuccess(order)
                }
                .onFailure { error ->
                    val message = error.localizedMessage ?: "Failed to place order"
                    _uiState.update { it.copy(isPlacingOrder = false, errorMessage = message) }
                    onError(message)
                }
        }
    }
}
