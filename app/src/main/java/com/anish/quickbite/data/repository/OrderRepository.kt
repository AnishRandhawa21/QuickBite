package com.anish.quickbite.data.repository

import android.util.Log
import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.SupabaseClient
import com.anish.quickbite.data.local.dao.OrderHistoryDao
import com.anish.quickbite.data.local.entity.CartItemEntity
import com.anish.quickbite.data.local.entity.OrderHistoryEntity
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.model.Order
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

interface OrderRepository {
    fun getOrdersFlow(): Flow<List<Order>>
    fun getActiveOrdersFlow(): Flow<List<Order>>
    fun getOrderByIdFlow(orderId: String): Flow<Order?>
    suspend fun syncOrders(): Result<List<Order>>
    suspend fun getOrders(): Result<List<Order>>
    suspend fun activatePass(orderId: String): Result<Unit>
    suspend fun placeOrder(canteenId: String, items: List<CartItemEntity>): Result<Order>
    suspend fun createDemoOrder(): Result<Unit>
    suspend fun clearLocalOrders()
}

class OrderRepositoryImpl(
    private val orderHistoryDao: OrderHistoryDao = QuickBiteApp.database.orderHistoryDao(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) : OrderRepository {

    private val supabase = SupabaseClient.client
    private val postgrest = supabase.postgrest
    private val auth = supabase.auth

    private fun getUserId(): String {
        return auth.currentUserOrNull()?.id
            ?: authRepository.currentUserId()
            ?: "local_user"
    }

    override fun getOrdersFlow(): Flow<List<Order>> {
        val userId = getUserId()
        return orderHistoryDao.getOrdersFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveOrdersFlow(): Flow<List<Order>> {
        val userId = getUserId()
        return orderHistoryDao.getActiveOrdersFlow(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOrderByIdFlow(orderId: String): Flow<Order?> {
        return orderHistoryDao.getOrderByIdFlow(orderId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun syncOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val userId = getUserId()
            val remoteOrders = postgrest.from("orders")
                .select(columns = Columns.raw("*, order_items(*, menu_items(*)), canteens(*)")) {
                    filter {
                        eq("student_id", userId)
                    }
                }
                .decodeList<Order>()

            // Update Room database as single source of truth
            val entities = remoteOrders.map { OrderHistoryEntity.fromDomain(it) }
            orderHistoryDao.syncOrders(userId, entities)

            Result.success(remoteOrders)
        } catch (e: Exception) {
            Log.e("OrderRepo", "Failed to sync orders from Supabase", e)
            val cached = orderHistoryDao.getOrdersDirect(getUserId()).map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        val cached = orderHistoryDao.getOrdersDirect(getUserId()).map { it.toDomain() }
        if (cached.isNotEmpty()) {
            return@withContext Result.success(cached)
        }
        syncOrders()
    }

    override suspend fun activatePass(orderId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = Instant.now()
            val expiresAt = now.plusSeconds(600) // 10 minutes from now

            val updatePayload = buildJsonObject {
                put("status", "READY")
                put("activated_at", now.toString())
                put("expires_at", expiresAt.toString())
            }

            postgrest.from("orders").update(updatePayload) {
                filter {
                    eq("id", orderId)
                }
            }

            // Immediately reflect in local Room cache
            orderHistoryDao.updateOrderStatus(
                orderId = orderId,
                status = "READY",
                activatedAt = now.toString(),
                expiresAt = expiresAt.toString()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepo", "Failed to activate pickup pass on server", e)
            val msg = e.localizedMessage ?: "Failed to activate pickup pass. Please check connection."
            Result.failure(Exception(msg))
        }
    }

    override suspend fun placeOrder(
        canteenId: String,
        items: List<CartItemEntity>
    ): Result<Order> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUserOrNull()?.id ?: authRepository.currentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            if (items.isEmpty()) {
                return@withContext Result.failure(Exception("Cart is empty"))
            }

            val totalAmount = items.sumOf { it.price * it.quantity }
            // Generate clean 5-character recovery code (e.g. 7K4P9 as in project plan)
            val alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
            val recoveryCode = (1..5).map { alphabet.random() }.joinToString("")

            val orderPayload = buildJsonObject {
                put("student_id", currentUserId)
                put("canteen_id", canteenId)
                put("total_amount", totalAmount)
                put("recovery_code", recoveryCode)
                put("status", "PLACED")
            }

            Log.d("OrderRepo", "Inserting real order to server: $orderPayload")
            postgrest.from("orders").insert(orderPayload)

            // Retrieve inserted order
            val latestOrders = postgrest.from("orders").select {
                filter {
                    eq("student_id", currentUserId)
                    eq("canteen_id", canteenId)
                }
            }.decodeList<Order>()

            val insertedOrder = latestOrders.maxByOrNull { it.createdAt ?: "" }
                ?: return@withContext Result.failure(Exception("Could not retrieve created order from server"))

            // Insert each item into order_items
            for (cartItem in items) {
                val itemPayload = buildJsonObject {
                    put("order_id", insertedOrder.id)
                    put("menu_item_id", cartItem.menuItemId)
                    put("quantity", cartItem.quantity)
                    put("unit_price", cartItem.price)
                }
                postgrest.from("order_items").insert(itemPayload)
            }

            // Clear user's local Room cart now that order is confirmed on server
            QuickBiteApp.database.cartDao().clearCartForUser(currentUserId)

            // Sync confirmed orders back into Room
            syncOrders()

            Result.success(insertedOrder)
        } catch (e: Exception) {
            Log.e("OrderRepo", "Failed to place order on server", e)
            val msg = e.localizedMessage ?: "Failed to place order. Please check connection."
            Result.failure(Exception(msg))
        }
    }

    override suspend fun createDemoOrder(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUserOrNull()?.id ?: authRepository.currentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val canteens = postgrest.from("canteens").select().decodeList<Canteen>()
            if (canteens.isEmpty()) return@withContext Result.failure(Exception("No canteens available"))
            val targetCanteen = canteens.firstOrNull { it.isOpen } ?: canteens.first()

            val menuItems = postgrest.from("menu_items").select {
                filter { eq("canteen_id", targetCanteen.id) }
            }.decodeList<MenuItem>()
            if (menuItems.isEmpty()) return@withContext Result.failure(Exception("No menu items available"))
            val targetItem = menuItems.first()

            val randomRecoveryCode = (1000..9999).random().toString()
            val orderPayload = buildJsonObject {
                put("student_id", currentUserId)
                put("canteen_id", targetCanteen.id)
                put("total_amount", targetItem.price)
                put("recovery_code", randomRecoveryCode)
                put("status", "PLACED")
            }

            Log.d("OrderRepo", "Inserting demo order to server: $orderPayload")
            postgrest.from("orders").insert(orderPayload)

            // Get inserted order
            val latestOrders = postgrest.from("orders").select {
                filter {
                    eq("student_id", currentUserId)
                    eq("canteen_id", targetCanteen.id)
                }
            }.decodeList<Order>()

            val insertedOrder = latestOrders.maxByOrNull { it.createdAt ?: "" }
            if (insertedOrder != null) {
                val itemPayload = buildJsonObject {
                    put("order_id", insertedOrder.id)
                    put("menu_item_id", targetItem.id)
                    put("quantity", 1)
                    put("unit_price", targetItem.price)
                }
                postgrest.from("order_items").insert(itemPayload)
            }

            // Sync newly confirmed order back into Room
            syncOrders()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepo", "Failed to create demo order on server", e)
            val msg = e.localizedMessage ?: "Failed to create demo order. Please check connection."
            Result.failure(Exception(msg))
        }
    }

    override suspend fun clearLocalOrders() = withContext(Dispatchers.IO) {
        orderHistoryDao.clearOrdersForUser(getUserId())
    }
}
