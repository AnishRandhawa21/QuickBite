package com.anish.quickbite.data.repository

import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.local.dao.RecentlyViewedDao
import com.anish.quickbite.data.local.entity.RecentlyViewedEntity
import com.anish.quickbite.data.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RecentlyViewedRepository(
    private val recentlyViewedDao: RecentlyViewedDao = QuickBiteApp.database.recentlyViewedDao(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) {
    private fun getUserId(): String {
        return authRepository.currentUserId() ?: "local_user"
    }

    fun getRecentlyViewedFlow(limit: Int = 10): Flow<List<RecentlyViewedEntity>> {
        return recentlyViewedDao.getRecentlyViewedFlow(getUserId(), limit)
    }

    suspend fun recordView(menuItem: MenuItem) = withContext(Dispatchers.IO) {
        val entity = RecentlyViewedEntity(
            userId = getUserId(),
            menuItemId = menuItem.id,
            canteenId = menuItem.canteenId,
            name = menuItem.name,
            price = menuItem.price,
            category = menuItem.category,
            imageUrl = menuItem.imageUrl
        )
        recentlyViewedDao.recordRecentlyViewed(entity)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        recentlyViewedDao.clearHistory(getUserId())
    }
}
