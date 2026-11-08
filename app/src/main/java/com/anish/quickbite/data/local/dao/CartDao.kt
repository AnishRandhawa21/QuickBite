package com.anish.quickbite.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.anish.quickbite.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY addedAt DESC")
    fun getCartItemsFlow(userId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND canteenId = :canteenId ORDER BY addedAt DESC")
    fun getCartItemsByCanteenFlow(userId: String, canteenId: String): Flow<List<CartItemEntity>>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE userId = :userId")
    fun getTotalCartCountFlow(userId: String): Flow<Int>

    @Query("SELECT * FROM cart_items WHERE userId = :userId")
    suspend fun getCartItemsDirect(userId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND menuItemId = :menuItemId LIMIT 1")
    suspend fun getCartItem(userId: String, menuItemId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCartItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE userId = :userId AND menuItemId = :menuItemId")
    suspend fun updateQuantity(userId: String, menuItemId: String, quantity: Int)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND menuItemId = :menuItemId")
    suspend fun deleteCartItem(userId: String, menuItemId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCartForUser(userId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId AND canteenId = :canteenId")
    suspend fun clearCartForCanteen(userId: String, canteenId: String)

    @Transaction
    suspend fun setItemQuantity(userId: String, menuItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            deleteCartItem(userId, menuItemId)
        } else {
            updateQuantity(userId, menuItemId, newQuantity)
        }
    }
}
