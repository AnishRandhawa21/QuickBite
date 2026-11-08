package com.anish.quickbite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anish.quickbite.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getFavoritesFlow(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND menuItemId = :menuItemId)")
    fun isFavoriteFlow(userId: String, menuItemId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND menuItemId = :menuItemId)")
    suspend fun isFavorite(userId: String, menuItemId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND menuItemId = :menuItemId")
    suspend fun deleteFavorite(userId: String, menuItemId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearFavorites(userId: String)

    @Transaction
    suspend fun toggleFavorite(favorite: FavoriteEntity) {
        if (isFavorite(favorite.userId, favorite.menuItemId)) {
            deleteFavorite(favorite.userId, favorite.menuItemId)
        } else {
            insertFavorite(favorite)
        }
    }
}
