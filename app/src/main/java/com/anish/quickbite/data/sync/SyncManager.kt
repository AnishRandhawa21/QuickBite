package com.anish.quickbite.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.SupabaseClient
import com.anish.quickbite.data.local.entity.MenuItemEntity
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.repository.OrderRepository
import com.anish.quickbite.data.repository.OrderRepositoryImpl
import com.anish.quickbite.data.repository.StudentRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeOldRecordOrNull
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val timestamp: Long = System.currentTimeMillis()) : SyncStatus
    data class Error(val message: String) : SyncStatus
}

/**
 * Central orchestrator for QuickBite's offline-first synchronization.
 * Handles:
 * 1. Immediate and foreground refresh cycles
 * 2. Background retryable worker scheduling via WorkManager
 * 3. Realtime Supabase PostgreSQL changes for menu items & orders
 * 4. Observable sync status state
 */
class SyncManager(
    private val studentRepository: StudentRepository = StudentRepository(),
    private val orderRepository: OrderRepository = OrderRepositoryImpl()
) {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val database = QuickBiteApp.database

    /**
     * Synchronizes menus (for a specific canteen or all available canteens)
     * and current user orders from Supabase into the local Room database.
     */
    suspend fun syncAll(canteenId: String? = null): Result<Unit> {
        _syncStatus.value = SyncStatus.Syncing
        return try {
            if (canteenId != null) {
                studentRepository.syncMenu(canteenId)
            } else {
                studentRepository.getCanteens().onSuccess { canteens ->
                    for (canteen in canteens) {
                        studentRepository.syncMenu(canteen.id)
                    }
                }
            }

            val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                orderRepository.syncOrders()
            }

            _syncStatus.value = SyncStatus.Success()
            Log.d("SyncManager", "Full synchronization completed successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            val message = e.message ?: "Failed to synchronize remote data"
            Log.w("SyncManager", "Sync encountered an error: $message")
            _syncStatus.value = SyncStatus.Error(message)
            Result.failure(e)
        }
    }

    /**
     * Triggered when the app transitions back to the foreground.
     */
    fun onAppForeground(scope: CoroutineScope) {
        Log.d("SyncManager", "App foregrounded: initiating synchronization...")
        scope.launch {
            syncAll()
        }
    }

    /**
     * Starts listening to Supabase Realtime postgres changes.
     * Updates Room database automatically when cloud modifications occur.
     */
    fun startRealtimeSync(scope: CoroutineScope) {
        // 1. Menu items channel
        scope.launch {
            try {
                val menuChannel = SupabaseClient.client.realtime.channel("public:menu_items")
                val menuFlow = menuChannel.postgresChangeFlow<PostgresAction>("public") {
                    table = "menu_items"
                }
                menuChannel.subscribe()
                Log.d("SyncManager", "Subscribed to Supabase Realtime channel for menu_items")

                menuFlow.collect { action ->
                    try {
                        when (action) {
                            is PostgresAction.Insert -> {
                                val item = action.decodeRecordOrNull<MenuItem>()
                                if (item != null) {
                                    database.menuDao().insertMenuItem(MenuItemEntity.fromDomain(item))
                                    Log.d("SyncManager", "Realtime: inserted menu item '${item.name}' into Room")
                                }
                            }
                            is PostgresAction.Update -> {
                                val item = action.decodeRecordOrNull<MenuItem>()
                                if (item != null) {
                                    database.menuDao().insertMenuItem(MenuItemEntity.fromDomain(item))
                                    Log.d("SyncManager", "Realtime: updated menu item '${item.name}' in Room")
                                }
                            }
                            is PostgresAction.Delete -> {
                                val itemId = action.oldRecord["id"]?.jsonPrimitive?.contentOrNull
                                    ?: action.decodeOldRecordOrNull<MenuItem>()?.id
                                if (itemId != null) {
                                    database.menuDao().deleteMenuItemById(itemId)
                                    Log.d("SyncManager", "Realtime: deleted menu item $itemId from Room")
                                }
                            }
                            else -> Unit
                        }
                    } catch (e: Exception) {
                        Log.w("SyncManager", "Error handling menu realtime event: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Realtime menu subscription failed: ${e.message}")
            }
        }

        // 2. Orders channel
        scope.launch {
            try {
                val orderChannel = SupabaseClient.client.realtime.channel("public:orders")
                val orderFlow = orderChannel.postgresChangeFlow<PostgresAction>("public") {
                    table = "orders"
                }
                orderChannel.subscribe()
                Log.d("SyncManager", "Subscribed to Supabase Realtime channel for orders")

                orderFlow.collect { action ->
                    try {
                        val currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
                        if (currentUserId != null) {
                            when (action) {
                                is PostgresAction.Insert,
                                is PostgresAction.Update,
                                is PostgresAction.Delete -> {
                                    orderRepository.syncOrders()
                                    Log.d("SyncManager", "Realtime: synced orders into Room after order change")
                                }
                                else -> Unit
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("SyncManager", "Error handling order realtime event: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w("SyncManager", "Realtime orders subscription failed: ${e.message}")
            }
        }
    }

    /**
     * Schedules a one-time immediate sync through WorkManager.
     */
    fun enqueueImmediateSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<MenuSyncWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        Log.d("SyncManager", "Enqueued immediate one-time background sync task.")
    }

    companion object {
        @Volatile
        private var instance: SyncManager? = null

        fun getInstance(): SyncManager {
            return instance ?: synchronized(this) {
                instance ?: SyncManager().also { instance = it }
            }
        }
    }
}
