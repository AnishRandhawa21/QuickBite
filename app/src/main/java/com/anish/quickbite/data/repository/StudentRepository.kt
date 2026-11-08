package com.anish.quickbite.data.repository

import android.util.Log
import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.SupabaseClient
import com.anish.quickbite.data.local.dao.MenuDao
import com.anish.quickbite.data.local.entity.MenuItemEntity
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.data.model.UserProfile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class StudentRepository(
    private val menuDao: MenuDao = QuickBiteApp.database.menuDao(),
    private val orderRepository: OrderRepository = OrderRepositoryImpl()
) {
    private val supabase = SupabaseClient.client
    private val postgrest = supabase.postgrest
    private val auth = supabase.auth

    /**
     * Observes menu items from the local Room database via Flow.
     * Guarantees offline availability and immediate UI emission.
     */
    fun getMenuItemsFlow(canteenId: String): Flow<List<MenuItem>> {
        return menuDao.getMenuItemsByCanteenFlow(canteenId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Synchronizes menu items from Supabase into the local Room database.
     * Network failures do NOT erase or corrupt valid cached data.
     */
    suspend fun syncMenu(canteenId: String): Result<List<MenuItem>> = withContext(Dispatchers.IO) {
        try {
            val remoteItems = postgrest.from("menu_items")
                .select {
                    filter {
                        eq("canteen_id", canteenId)
                    }
                }
                .decodeList<MenuItem>()

            val sorted = remoteItems.sortedWith(
                compareByDescending<MenuItem> { it.isAvailable }
                    .thenBy { it.category }
                    .thenBy { it.name }
            )

            // Atomically update local Room cache
            val entities = sorted.map { MenuItemEntity.fromDomain(it) }
            menuDao.syncCanteenMenu(canteenId, entities)

            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("StudentRepo", "Failed to sync menu from Supabase, keeping cached data", e)
            Result.failure(e)
        }
    }

    suspend fun getCanteens(): Result<List<Canteen>> = withContext(Dispatchers.IO) {
        try {
            val canteens = postgrest.from("canteens")
                .select()
                .decodeList<Canteen>()
            val sorted = canteens.sortedWith(
                compareByDescending<Canteen> { it.isOpen }.thenBy { it.name }
            )
            Result.success(sorted)
        } catch (e: Exception) {
            Log.e("StudentRepo", "Failed to get canteens", e)
            Result.failure(e)
        }
    }

    suspend fun getMenuItems(canteenId: String): Result<List<MenuItem>> = withContext(Dispatchers.IO) {
        // Attempt synchronization first
        val syncResult = syncMenu(canteenId)
        if (syncResult.isSuccess) {
            syncResult
        } else {
            // Offline fallback: load from Room cache
            val cachedEntities = menuDao.getMenuItemsByCanteenDirect(canteenId)
            if (cachedEntities.isNotEmpty()) {
                Result.success(cachedEntities.map { it.toDomain() })
            } else {
                syncResult
            }
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile?> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            val profile = postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", currentUserId)
                    }
                }
                .decodeSingleOrNull<UserProfile>()
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("StudentRepo", "Failed to get profile", e)
            Result.failure(e)
        }
    }

    /**
     * Observes orders continuously from Room Database via Flow.
     */
    fun getStudentOrdersFlow(): Flow<List<Order>> {
        return orderRepository.getOrdersFlow()
    }

    /**
     * Observes active pickup orders continuously from Room Database via Flow.
     */
    fun getActiveOrdersFlow(): Flow<List<Order>> {
        return orderRepository.getActiveOrdersFlow()
    }

    /**
     * Synchronizes student orders from Supabase into local Room database.
     */
    suspend fun syncStudentOrders(): Result<List<Order>> {
        return orderRepository.syncOrders()
    }

    /**
     * Retrieves student orders, attempting sync first with offline fallback to Room.
     */
    suspend fun getStudentOrders(): Result<List<Order>> {
        return orderRepository.getOrders()
    }

    /**
     * Activates a 10-minute pickup pass. Authoritative on Supabase, updates Room on success.
     */
    suspend fun activatePass(orderId: String): Result<Unit> {
        return orderRepository.activatePass(orderId)
    }

    /**
     * Places a real order from the user's cart onto Supabase and caches into Room.
     */
    suspend fun placeOrder(canteenId: String, items: List<com.anish.quickbite.data.local.entity.CartItemEntity>): Result<Order> {
        return orderRepository.placeOrder(canteenId, items)
    }

    /**
     * Creates a test order on the server and caches into Room.
     */
    suspend fun createDemoOrder(): Result<Unit> {
        return orderRepository.createDemoOrder()
    }
}
