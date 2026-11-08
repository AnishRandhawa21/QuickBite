package com.anish.quickbite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anish.quickbite.data.local.entity.MenuItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    @Query("SELECT * FROM menu_items WHERE canteenId = :canteenId ORDER BY isAvailable DESC, category ASC, name ASC")
    fun getMenuItemsByCanteenFlow(canteenId: String): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items ORDER BY name ASC")
    fun getAllMenuItemsFlow(): Flow<List<MenuItemEntity>>

    @Query("SELECT * FROM menu_items WHERE id = :itemId LIMIT 1")
    fun getMenuItemByIdFlow(itemId: String): Flow<MenuItemEntity?>

    @Query("SELECT * FROM menu_items WHERE id = :itemId LIMIT 1")
    suspend fun getMenuItemById(itemId: String): MenuItemEntity?

    @Query("SELECT * FROM menu_items WHERE canteenId = :canteenId")
    suspend fun getMenuItemsByCanteenDirect(canteenId: String): List<MenuItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity)

    @Query("DELETE FROM menu_items WHERE id = :id")
    suspend fun deleteMenuItemById(id: String)

    @Query("DELETE FROM menu_items WHERE canteenId = :canteenId")
    suspend fun deleteMenuItemsForCanteen(canteenId: String)

    @Transaction
    suspend fun syncCanteenMenu(canteenId: String, items: List<MenuItemEntity>) {
        deleteMenuItemsForCanteen(canteenId)
        insertMenuItems(items)
    }
}
