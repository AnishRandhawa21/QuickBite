package com.anish.quickbite

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.anish.quickbite.data.local.QuickBiteDatabase
import com.anish.quickbite.data.sync.MenuSyncWorker
import com.anish.quickbite.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class QuickBiteApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var startedActivityCount = 0

    companion object {
        private var _instance: QuickBiteApp? = null
        val instance: QuickBiteApp
            get() = checkNotNull(_instance) { "QuickBiteApp is not initialized yet." }

        val database: QuickBiteDatabase by lazy {
            QuickBiteDatabase.getInstance(instance)
        }

        val syncManager: SyncManager by lazy {
            SyncManager.getInstance()
        }
    }

    override fun onCreate() {
        super.onCreate()
        _instance = this

        // 1. Schedule periodic background sync via WorkManager (every 6h)
        MenuSyncWorker.schedulePeriodicSync(this)

        // 2. Start Realtime channel subscriptions for menu items & orders
        syncManager.startRealtimeSync(applicationScope)

        // 3. Foreground detection: trigger data synchronization when app enters foreground
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount == 0) {
                    syncManager.onAppForeground(applicationScope)
                }
                startedActivityCount++
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
