package com.anish.quickbite.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "recently_viewed",
    primaryKeys = ["userId", "menuItemId"],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["viewedAt"])
    ]
)
data class RecentlyViewedEntity(
    val userId: String,
    val menuItemId: String,
    val canteenId: String,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String?,
    val viewedAt: Long = System.currentTimeMillis()
)
