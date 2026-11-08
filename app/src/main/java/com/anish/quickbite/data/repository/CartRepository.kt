package com.anish.quickbite.data.repository

import com.anish.quickbite.QuickBiteApp
import com.anish.quickbite.data.local.dao.CartDao
import com.anish.quickbite.data.local.entity.CartItemEntity
import com.anish.quickbite.data.model.MenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CartRepository(
    private val cartDao: CartDao = QuickBiteApp.database.cartDao(),
    private val authRepository: AuthRepository = AuthRepositoryImpl()
) {
    private fun getUserId(): String {
        return authRepository.currentUserId() ?: "local_user"
    }

    fun getCartItemsFlow(): Flow<List<CartItemEntity>> {
        return cartDao.getCartItemsFlow(getUserId())
    }

    fun getCartCountFlow(): Flow<Int> {
        return cartDao.getTotalCartCountFlow(getUserId())
    }

    fun getCartItemsForCanteenFlow(canteenId: String): Flow<List<CartItemEntity>> {
        return cartDao.getCartItemsByCanteenFlow(getUserId(), canteenId)
    }

    suspend fun addToCart(menuItem: MenuItem, quantityToAdd: Int = 1) = withContext(Dispatchers.IO) {
        val userId = getUserId()
        val existing = cartDao.getCartItem(userId, menuItem.id)
        if (existing != null) {
            cartDao.updateQuantity(userId, menuItem.id, existing.quantity + quantityToAdd)
        } else {
            val entity = CartItemEntity(
                userId = userId,
                menuItemId = menuItem.id,
                canteenId = menuItem.canteenId,
                name = menuItem.name,
                price = menuItem.price,
                imageUrl = menuItem.imageUrl,
                category = menuItem.category,
                quantity = quantityToAdd
            )
            cartDao.insertOrUpdateCartItem(entity)
        }
    }

    suspend fun updateQuantity(menuItemId: String, newQuantity: Int) = withContext(Dispatchers.IO) {
        cartDao.setItemQuantity(getUserId(), menuItemId, newQuantity)
    }

    suspend fun removeFromCart(menuItemId: String) = withContext(Dispatchers.IO) {
        cartDao.deleteCartItem(getUserId(), menuItemId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        cartDao.clearCartForUser(getUserId())
    }

    suspend fun clearCartForCanteen(canteenId: String) = withContext(Dispatchers.IO) {
        cartDao.clearCartForCanteen(getUserId(), canteenId)
    }

    suspend fun getCartItems(): List<CartItemEntity> = withContext(Dispatchers.IO) {
        cartDao.getCartItemsDirect(getUserId())
    }
}
