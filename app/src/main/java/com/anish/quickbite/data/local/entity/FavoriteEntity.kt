package com.anish.quickbite.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "favorites",
    primaryKeys = ["userId", "menuItemId"],
    indices = [
        Index(value = ["userId"])
    ]
)
data class FavoriteEntity(
    val userId: String,
    val menuItemId: String,
    val canteenId: String,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean = true,
    val addedAt: Long = System.currentTimeMillis()
)
