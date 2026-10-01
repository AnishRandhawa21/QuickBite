package com.anish.quickbite.data.repository

import com.anish.quickbite.SupabaseClient
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.CanteenAdmin
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.model.Order
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
private data class MenuItemCreateRequest(
    @SerialName("canteen_id") val canteenId: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_available") val isAvailable: Boolean
)

@Serializable
private data class MenuItemUpdateRequest(
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_available") val isAvailable: Boolean
)

@Serializable
private data class MenuItemAvailabilityUpdateRequest(
    @SerialName("is_available") val isAvailable: Boolean
)

@Serializable
private data class OrderStatusUpdateRequest(
    val status: String
)

@Serializable
private data class CanteenImageUrlUpdateRequest(
    @SerialName("image_url") val imageUrl: String
)

class AdminRepository {
    private val supabase = SupabaseClient.client
    private val auth = supabase.auth
    private val postgrest = supabase.postgrest

    suspend fun getAssignedCanteens(): Result<List<Canteen>> = withContext(Dispatchers.IO) {
        try {
            val adminId = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // We can query canteen_admins and join with canteens
            val canteenAdmins = postgrest.from("canteen_admins")
                .select(columns = Columns.raw("id, admin_id, canteen_id, canteens(*)")) {
                    filter {
                        eq("admin_id", adminId)
                    }
                }
                .decodeList<CanteenAdmin>()

            val canteens = canteenAdmins.mapNotNull { it.canteen }
            Result.success(canteens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMenuItems(canteenId: String): Result<List<MenuItem>> = withContext(Dispatchers.IO) {
        try {
            val items = postgrest.from("menu_items")
                .select {
                    filter {
                        eq("canteen_id", canteenId)
                    }
                }
                .decodeList<MenuItem>()
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMenuItem(menuItem: MenuItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val itemToInsert = MenuItemCreateRequest(
                canteenId = menuItem.canteenId,
                name = menuItem.name,
                description = menuItem.description,
                price = menuItem.price,
                category = menuItem.category,
                imageUrl = menuItem.imageUrl,
                isAvailable = menuItem.isAvailable
            )

            postgrest.from("menu_items").insert(itemToInsert)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMenuItem(menuItem: MenuItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val itemToUpdate = MenuItemUpdateRequest(
                name = menuItem.name,
                description = menuItem.description,
                price = menuItem.price,
                category = menuItem.category,
                imageUrl = menuItem.imageUrl,
                isAvailable = menuItem.isAvailable
            )
            postgrest.from("menu_items").update(itemToUpdate) {
                filter {
                    eq("id", menuItem.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            postgrest.from("menu_items").delete {
                filter {
                    eq("id", itemId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleMenuItemAvailability(itemId: String, isAvailable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = MenuItemAvailabilityUpdateRequest(isAvailable = isAvailable)
            postgrest.from("menu_items").update(request) {
                filter {
                    eq("id", itemId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFoodImage(canteenId: String, imageBytes: ByteArray, extension: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uniqueId = UUID.randomUUID().toString()
            val path = "$canteenId/$uniqueId.$extension"
            val bucket = supabase.storage.from("menu-images")
            
            bucket.upload(path, imageBytes) {
                upsert = false
            }
            
            val publicUrl = bucket.publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFoodImage(imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bucketName = "menu-images"
            val pathStart = imageUrl.indexOf("/$bucketName/")
            if (pathStart != -1) {
                val path = imageUrl.substring(pathStart + "/$bucketName/".length)
                supabase.storage.from(bucketName).delete(path)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val adminId = auth.currentUserOrNull()?.id
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            // Fetch assigned canteens
            val canteenAdmins = postgrest.from("canteen_admins")
                .select(columns = Columns.raw("canteen_id")) {
                    filter { eq("admin_id", adminId) }
                }
                .decodeList<CanteenAdmin>()
            
            val canteenIds = canteenAdmins.map { it.canteenId }
            if (canteenIds.isEmpty()) return@withContext Result.success(emptyList())

            val orders = postgrest.from("orders")
                .select(columns = Columns.raw("*, order_items(*, menu_items(*)), canteens(*)")) {
                    filter {
                        isIn("canteen_id", canteenIds)
                    }
                }
                .decodeList<Order>()

            // Sort so newest is first or by status
            val sortedOrders = orders.sortedByDescending { it.createdAt }
            Result.success(sortedOrders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = OrderStatusUpdateRequest(status = newStatus)
            postgrest.from("orders").update(request) {
                filter {
                    eq("id", orderId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCanteen(name: String, location: String, isOpen: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestParams = buildJsonObject {
                put("p_name", name)
                put("p_location", location)
                put("p_is_open", isOpen)
            }
            postgrest.rpc("create_canteen_and_assign", requestParams)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCanteenImageUrl(canteenId: String, imageUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = CanteenImageUrlUpdateRequest(imageUrl = imageUrl)
            postgrest.from("canteens").update(request) {
                filter {
                    eq("id", canteenId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadCanteenImage(
        canteenId: String,
        imageBytes: ByteArray,
        extension: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uniqueId = UUID.randomUUID().toString()
            val path = "$canteenId/$uniqueId.$extension"
            val bucket = supabase.storage.from("canteen-images")

            bucket.upload(path, imageBytes) {
                upsert = false
            }

            val publicUrl = bucket.publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
