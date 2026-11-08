package com.anish.quickbite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anish.quickbite.data.local.entity.OrderHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderHistoryDao {

    @Query("SELECT * FROM order_history WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOrdersFlow(userId: String): Flow<List<OrderHistoryEntity>>

    @Query("SELECT * FROM order_history WHERE userId = :userId AND status IN ('PLACED', 'PREPARING', 'READY', 'PICKUP_ACTIVE', 'ACTIVE') ORDER BY createdAt DESC")
    fun getActiveOrdersFlow(userId: String): Flow<List<OrderHistoryEntity>>

    @Query("SELECT * FROM order_history WHERE orderId = :orderId LIMIT 1")
    fun getOrderByIdFlow(orderId: String): Flow<OrderHistoryEntity?>

    @Query("SELECT * FROM order_history WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): OrderHistoryEntity?

    @Query("SELECT * FROM order_history WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getOrdersDirect(userId: String): List<OrderHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderHistoryEntity)

    @Query("""
        UPDATE order_history 
        SET status = :status, 
            activatedAt = COALESCE(:activatedAt, activatedAt), 
            expiresAt = COALESCE(:expiresAt, expiresAt) 
        WHERE orderId = :orderId
    """)
    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        activatedAt: String?,
        expiresAt: String?
    )

    @Query("DELETE FROM order_history WHERE userId = :userId")
    suspend fun clearOrdersForUser(userId: String)

    @Transaction
    suspend fun syncOrders(userId: String, orders: List<OrderHistoryEntity>) {
        clearOrdersForUser(userId)
        insertOrders(orders)
    }
}
