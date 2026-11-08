package com.anish.quickbite.data.repository

import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.local.dao.FavoriteDao
import com.anish.quickbite.data.local.entity.FavoriteEntity
import com.anish.quickbite.data.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FavoriteRepository(
    private val favoriteDao: FavoriteDao = QuickBiteApp.database.favoriteDao(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) {
    private fun getUserId(): String {
        return authRepository.currentUserId() ?: "local_user"
    }

    fun getFavoritesFlow(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getFavoritesFlow(getUserId())
    }

    fun isFavoriteFlow(menuItemId: String): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(getUserId(), menuItemId)
    }

    suspend fun toggleFavorite(menuItem: MenuItem) = withContext(Dispatchers.IO) {
        val entity = FavoriteEntity(
            userId = getUserId(),
            menuItemId = menuItem.id,
            canteenId = menuItem.canteenId,
            name = menuItem.name,
            price = menuItem.price,
            category = menuItem.category,
            imageUrl = menuItem.imageUrl,
            isAvailable = menuItem.isAvailable
        )
        favoriteDao.toggleFavorite(entity)
    }

    suspend fun removeFavorite(menuItemId: String) = withContext(Dispatchers.IO) {
        favoriteDao.deleteFavorite(getUserId(), menuItemId)
    }
}
