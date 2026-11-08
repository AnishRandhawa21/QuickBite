package com.anish.quickbite.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.data.model.OrderItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(
    tableName = "order_history",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["createdAt"])
    ]
)
data class OrderHistoryEntity(
    @PrimaryKey
    val orderId: String,
    val userId: String,
    val canteenId: String,
    val canteenName: String,
    val canteenLocation: String = "",
    val status: String,
    val totalAmount: Double,
    val createdAt: String?,
    val activatedAt: String?,
    val expiresAt: String?,
    val recoveryCode: String?,
    val recoveryExpiresAt: String?,
    val reopenCount: Int,
    val itemsJson: String,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(json: Json = defaultJson): Order {
        val itemsList: List<OrderItem> = try {
            json.decodeFromString(itemsJson)
        } catch (e: Exception) {
            emptyList()
        }
        return Order(
            id = orderId,
            studentId = userId,
            canteenId = canteenId,
            status = status,
            totalAmount = totalAmount,
            createdAt = createdAt,
            activatedAt = activatedAt,
            expiresAt = expiresAt,
            recoveryCode = recoveryCode,
            recoveryExpiresAt = recoveryExpiresAt,
            reopenCount = reopenCount,
            items = itemsList,
            canteen = Canteen(
                id = canteenId,
                name = canteenName,
                location = canteenLocation,
                isOpen = true
            )
        )
    }

    companion object {
        private val defaultJson = Json { ignoreUnknownKeys = true }

        fun fromDomain(
            order: Order,
            json: Json = defaultJson,
            cachedAt: Long = System.currentTimeMillis()
        ): OrderHistoryEntity {
            val itemsJsonString = try {
                json.encodeToString(order.items)
            } catch (e: Exception) {
                "[]"
            }
            return OrderHistoryEntity(
                orderId = order.id,
                userId = order.studentId,
                canteenId = order.canteenId,
                canteenName = order.canteen?.name ?: "Canteen",
                canteenLocation = order.canteen?.location ?: "",
                status = order.status,
                totalAmount = order.totalAmount,
                createdAt = order.createdAt,
                activatedAt = order.activatedAt,
                expiresAt = order.expiresAt,
                recoveryCode = order.recoveryCode,
                recoveryExpiresAt = order.recoveryExpiresAt,
                reopenCount = order.reopenCount,
                itemsJson = itemsJsonString,
                cachedAt = cachedAt
            )
        }
    }
}
