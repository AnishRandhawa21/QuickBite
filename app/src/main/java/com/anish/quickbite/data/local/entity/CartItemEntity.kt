package com.anish.quickbite.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cart_items",
    primaryKeys = ["userId", "menuItemId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["canteenId"])
    ]
)
data class CartItemEntity(
    val userId: String,
    val menuItemId: String,
    val canteenId: String,
    val name: String,
    val price: Double,
    val imageUrl: String?,
    val category: String,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
)
