package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.data.repository.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class StudentPassUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val orders: List<Order> = emptyList(),
    val currentOrder: Order? = null,
    val remainingSeconds: Long = 0,
    val formattedTime: String = "10:00",
    val progress: Float = 1f,
    val isExpired: Boolean = false,
    val errorMessage: String? = null,
    val isActionInProgress: Boolean = false
)

class StudentPassViewModel(
    private val repository: StudentRepository = StudentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentPassUiState(isLoading = true))
    val uiState: StateFlow<StudentPassUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var ordersObservationJob: Job? = null
    private var syncJob: Job? = null

    init {
        observeOrders()
        syncOrders()
    }

    /**
     * Continuously observes cached orders from Room Database via Flow.
     * Guarantees that local orders, active passes, and recovery codes are
     * immediately available offline without waiting for network requests.
     */
    private fun observeOrders() {
        ordersObservationJob?.cancel()
        ordersObservationJob = viewModelScope.launch {
            repository.getStudentOrdersFlow().collect { cachedOrders ->
                val currentSelectedId = _uiState.value.currentOrder?.id

                // Prefer currently selected order if still present, otherwise find active/recent
                val activeOrder = cachedOrders.firstOrNull { it.id == currentSelectedId }
                    ?: cachedOrders.firstOrNull { it.status in listOf("READY", "PICKUP_ACTIVE", "PLACED", "PREPARING", "ACTIVE") }
                    ?: cachedOrders.firstOrNull()

                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        orders = cachedOrders,
                        currentOrder = activeOrder
                    )
                }

                if ((activeOrder?.status == "READY" || activeOrder?.status == "PICKUP_ACTIVE") && activeOrder.expiresAt != null) {
                    startCountdown(activeOrder.expiresAt)
                } else if (activeOrder?.status in listOf("PLACED", "PREPARING", "ACTIVE")) {
                    countdownJob?.cancel()
                    _uiState.update {
                        it.copy(
                            remainingSeconds = 600,
                            formattedTime = "10:00",
                            progress = 1f,
                            isExpired = false
                        )
                    }
                } else {
                    countdownJob?.cancel()
                }
            }
        }
    }

    /**
     * Refreshes orders from Supabase into Room.
     * Network failures do not clear or corrupt valid cached data.
     */
    fun syncOrders() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val result = repository.syncStudentOrders()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        isOffline = false,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { current ->
                    current.copy(
                        isRefreshing = false,
                        isOffline = true,
                        errorMessage = if (current.orders.isEmpty()) {
                            error.localizedMessage ?: "Offline: No cached orders found."
                        } else null
                    )
                }
            }
        }
    }

    fun loadOrders() {
        syncOrders()
    }

    fun selectOrder(order: Order) {
        _uiState.update { it.copy(currentOrder = order) }
        if (order.status == "PICKUP_ACTIVE" && order.expiresAt != null) {
            startCountdown(order.expiresAt)
        } else if (order.status == "ACTIVE") {
            countdownJob?.cancel()
            _uiState.update {
                it.copy(
                    remainingSeconds = 600,
                    formattedTime = "10:00",
                    progress = 1f,
                    isExpired = false
                )
            }
        } else {
            countdownJob?.cancel()
        }
    }

    fun activatePass(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.activatePass(orderId)
            result.onSuccess {
                _uiState.update { it.copy(isActionInProgress = false) }
                syncOrders()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = error.localizedMessage ?: "Failed to activate pass"
                    )
                }
            }
        }
    }

    fun createDemoOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, errorMessage = null) }
            val result = repository.createDemoOrder()
            result.onSuccess {
                _uiState.update { it.copy(isActionInProgress = false) }
                syncOrders()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        errorMessage = error.localizedMessage ?: "Failed to create demo order"
                    )
                }
            }
        }
    }

    /**
     * One-Tap Reorder: Copies items from a previously completed order into the Room cart.
     */
    fun reorder(order: Order, onReordered: () -> Unit) {
        viewModelScope.launch {
            val currentUserId = com.anish.quickbite.data.SupabaseClient.client.pluginManager.getPluginOrNull(io.github.jan.supabase.auth.Auth)?.currentUserOrNull()?.id
                ?: return@launch
            for (item in order.items) {
                val menuItem = item.menuItem ?: continue
                val cartItem = com.anish.quickbite.data.local.entity.CartItemEntity(
                    userId = currentUserId,
                    menuItemId = menuItem.id,
                    canteenId = order.canteenId,
                    name = menuItem.name,
                    price = item.price,
                    imageUrl = menuItem.imageUrl,
                    category = menuItem.category,
                    quantity = item.quantity
                )
                com.anish.quickbite.QuickBiteApp.database.cartDao().insertOrUpdateCartItem(cartItem)
            }
            onReordered()
        }
    }

    private fun startCountdown(expiresAtIso: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            try {
                val expireTime = Instant.parse(expiresAtIso).epochSecond
                while (true) {
                    val now = Instant.now().epochSecond
                    val remaining = expireTime - now

                    if (remaining <= 0) {
                        _uiState.update {
                            it.copy(
                                remainingSeconds = 0,
                                formattedTime = "00:00",
                                progress = 0f,
                                isExpired = true
                            )
                        }
                        break
                    } else {
                        val minutes = remaining / 60
                        val seconds = remaining % 60
                        val formatted = String.format("%02d:%02d", minutes, seconds)
                        val progress = (remaining.toFloat() / 600f).coerceIn(0f, 1f)

                        _uiState.update {
                            it.copy(
                                remainingSeconds = remaining,
                                formattedTime = formatted,
                                progress = progress,
                                isExpired = false
                            )
                        }
                    }
                    delay(1000)
                }
            } catch (_: Exception) {
                // If parsing fails, default to 10 min
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        ordersObservationJob?.cancel()
        syncJob?.cancel()
    }
}
