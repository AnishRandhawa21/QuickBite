package com.anish.quickbite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anish.quickbite.data.model.MenuItem

@Entity(
    tableName = "menu_items",
    indices = [
        Index(value = ["canteenId"]),
        Index(value = ["category"])
    ]
)
data class MenuItemEntity(
    @PrimaryKey
    val id: String,
    val canteenId: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MenuItem = MenuItem(
        id = id,
        canteenId = canteenId,
        name = name,
        description = description,
        price = price,
        category = category,
        imageUrl = imageUrl,
        isAvailable = isAvailable,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(item: MenuItem, cachedAt: Long = System.currentTimeMillis()): MenuItemEntity =
            MenuItemEntity(
                id = item.id,
                canteenId = item.canteenId,
                name = item.name,
                description = item.description,
                price = item.price,
                category = item.category,
                imageUrl = item.imageUrl,
                isAvailable = item.isAvailable,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt,
                cachedAt = cachedAt
            )
    }
}
