package com.anish.quickbite.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anish.quickbite.data.local.dao.CartDao
import com.anish.quickbite.data.local.dao.FavoriteDao
import com.anish.quickbite.data.local.dao.MenuDao
import com.anish.quickbite.data.local.dao.OrderHistoryDao
import com.anish.quickbite.data.local.dao.RecentlyViewedDao
import com.anish.quickbite.data.local.entity.CartItemEntity
import com.anish.quickbite.data.local.entity.FavoriteEntity
import com.anish.quickbite.data.local.entity.MenuItemEntity
import com.anish.quickbite.data.local.entity.OrderHistoryEntity
import com.anish.quickbite.data.local.entity.RecentlyViewedEntity

@Database(
    entities = [
        MenuItemEntity::class,
        CartItemEntity::class,
        FavoriteEntity::class,
        RecentlyViewedEntity::class,
        OrderHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class QuickBiteDatabase : RoomDatabase() {

    abstract fun menuDao(): MenuDao
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun orderHistoryDao(): OrderHistoryDao

    companion object {
        private const val DATABASE_NAME = "quickbite.db"

        @Volatile
        private var INSTANCE: QuickBiteDatabase? = null

        fun getInstance(context: Context): QuickBiteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): QuickBiteDatabase {
            return Room.databaseBuilder(
                appContext,
                QuickBiteDatabase::class.java,
                DATABASE_NAME
            )
                // Strict production requirement: Never destructive fallback.
                // Explicit migrations are registered here when upgrading versions.
                .build()
        }
    }
}
