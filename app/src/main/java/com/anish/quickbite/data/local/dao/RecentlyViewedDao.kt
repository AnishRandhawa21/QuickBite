package com.anish.quickbite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anish.quickbite.data.local.entity.RecentlyViewedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentlyViewedDao {

    @Query("SELECT * FROM recently_viewed WHERE userId = :userId ORDER BY viewedAt DESC LIMIT :limit")
    fun getRecentlyViewedFlow(userId: String, limit: Int = 10): Flow<List<RecentlyViewedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateView(item: RecentlyViewedEntity)

    @Query("DELETE FROM recently_viewed WHERE userId = :userId AND menuItemId = :menuItemId")
    suspend fun deleteItem(userId: String, menuItemId: String)

    @Query("DELETE FROM recently_viewed WHERE userId = :userId")
    suspend fun clearHistory(userId: String)

    @Query("""
        DELETE FROM recently_viewed 
        WHERE userId = :userId 
        AND menuItemId NOT IN (
            SELECT menuItemId FROM recently_viewed 
            WHERE userId = :userId 
            ORDER BY viewedAt DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun pruneOldHistory(userId: String, keepCount: Int = 20)

    @Transaction
    suspend fun recordRecentlyViewed(item: RecentlyViewedEntity, maxKeep: Int = 20) {
        insertOrUpdateView(item)
        pruneOldHistory(item.userId, maxKeep)
    }
}
