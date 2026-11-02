package com.anish.quickbite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminOrdersState {
    object Loading : AdminOrdersState()
    data class OrdersLoaded(val orders: List<Order>) : AdminOrdersState()
    data class Error(val message: String) : AdminOrdersState()
}

class AdminOrdersViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminOrdersState>(AdminOrdersState.Loading)
    val uiState: StateFlow<AdminOrdersState> = _uiState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            if (_uiState.value !is AdminOrdersState.OrdersLoaded) {
                _uiState.value = AdminOrdersState.Loading
            }
            repository.getOrders().onSuccess { orders ->
                _uiState.value = AdminOrdersState.OrdersLoaded(orders)
            }.onFailure {
                _uiState.value = AdminOrdersState.Error(it.localizedMessage ?: "Failed to load orders")
            }
        }
    }

    fun updateOrderStatus(orderId: String, currentStatus: String) {
        val nextStatus = when (currentStatus) {
            "ACTIVE" -> "PICKUP_ACTIVE"
            "PICKUP_ACTIVE" -> "COMPLETED"
            "PLACED" -> "PREPARING"
            "PREPARING" -> "READY"
            "READY" -> "COMPLETED"
            else -> return // Invalid transition
        }

        viewModelScope.launch {
            repository.updateOrderStatus(orderId, nextStatus).onSuccess {
                loadOrders()
            }.onFailure {
                _uiState.value = AdminOrdersState.Error(it.localizedMessage ?: "Failed to update status")
            }
        }
    }

    fun verifyAndReopenOrder(
        recoveryCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.verifyAndReopenOrder(recoveryCode).onSuccess {
                loadOrders()
                onSuccess()
            }.onFailure {
                onError(it.localizedMessage ?: "Failed to verify recovery code")
            }
        }
    }
}
