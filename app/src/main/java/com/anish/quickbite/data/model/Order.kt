package com.anish.quickbite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("menu_item_id") val menuItemId: String = "",
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("menu_items") val menuItem: MenuItem? = null
) {
    val price: Double get() = unitPrice
}

@Serializable
data class Order(
    val id: String = "",
    @SerialName("student_id") val studentId: String = "",
    @SerialName("canteen_id") val canteenId: String = "",
    val status: String = "ACTIVE",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("activated_at") val activatedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("recovery_code") val recoveryCode: String? = null,
    @SerialName("recovery_expires_at") val recoveryExpiresAt: String? = null,
    @SerialName("reopen_count") val reopenCount: Int = 0,
    @SerialName("order_items") val items: List<OrderItem> = emptyList(),
    @SerialName("canteens") val canteen: Canteen? = null
) {
    val userId: String get() = studentId
}
