package com.anish.quickbite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("menu_item_id") val menuItemId: String = "",
    val quantity: Int = 1,
    val price: Double = 0.0,
    @SerialName("menu_items") val menuItem: MenuItem? = null
)

@Serializable
data class Order(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("canteen_id") val canteenId: String = "",
    val status: String = "PLACED",
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("order_items") val items: List<OrderItem> = emptyList(),
    @SerialName("canteens") val canteen: Canteen? = null
)
