package com.anish.quickbite.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.SupabaseClient
import com.anish.quickbite.data.local.entity.MenuItemEntity
import com.anish.quickbite.data.local.entity.OrderHistoryEntity
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.model.Order
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import java.util.concurrent.TimeUnit

/**
 * Retryable background WorkManager task that refreshes canteen menus
 * and user order history when network is available.
 */
class MenuSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val postgrest = SupabaseClient.client.postgrest
    private val auth = SupabaseClient.client.auth
    private val database = QuickBiteApp.database

    override suspend fun doWork(): Result {
        Log.d("MenuSyncWorker", "Starting background synchronization work...")
        return try {
            // 1. Sync canteens & menus
            val canteens = postgrest.from("canteens").select().decodeList<Canteen>()
            for (canteen in canteens) {
                try {
                    val remoteItems = postgrest.from("menu_items")
                        .select { filter { eq("canteen_id", canteen.id) } }
                        .decodeList<MenuItem>()
                    val entities = remoteItems.map { MenuItemEntity.fromDomain(it) }
                    database.menuDao().syncCanteenMenu(canteen.id, entities)
                } catch (e: Exception) {
                    Log.w("MenuSyncWorker", "Failed to sync menu for canteen ${canteen.name}: ${e.message}")
                }
            }

            // 2. Sync order history if user is authenticated
            val currentUserId = auth.currentUserOrNull()?.id
            if (currentUserId != null) {
                try {
                    val remoteOrders = postgrest.from("orders")
                        .select(columns = Columns.raw("*, order_items(*, menu_items(*)), canteens(*)")) {
                            filter { eq("student_id", currentUserId) }
                        }
                        .decodeList<Order>()
                    val entities = remoteOrders.map { OrderHistoryEntity.fromDomain(it) }
                    database.orderHistoryDao().syncOrders(currentUserId, entities)
                } catch (e: Exception) {
                    Log.w("MenuSyncWorker", "Failed to sync orders for student $currentUserId: ${e.message}")
                }
            }

            Log.d("MenuSyncWorker", "Background synchronization completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e("MenuSyncWorker", "Error during background sync, requesting retry", e)
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "quickbite_menu_sync_work"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MenuSyncWorker>(
                repeatInterval = 6,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d("MenuSyncWorker", "Scheduled periodic menu sync work (6 hours).")
        }
    }
}
