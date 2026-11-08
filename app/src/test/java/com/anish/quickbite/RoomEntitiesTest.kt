package com.anish.quickbite

import com.anish.quickbite.data.local.entity.CartItemEntity
import com.anish.quickbite.data.local.entity.FavoriteEntity
import com.anish.quickbite.data.local.entity.MenuItemEntity
import com.anish.quickbite.data.local.entity.OrderHistoryEntity
import com.anish.quickbite.data.local.entity.RecentlyViewedEntity
import com.anish.quickbite.data.model.Canteen
import com.anish.quickbite.data.model.MenuItem
import com.anish.quickbite.data.model.Order
import com.anish.quickbite.data.model.OrderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomEntitiesTest {

    @Test
    fun menuItemEntity_conversion_isAccurate() {
        val original = MenuItem(
            id = "item_123",
            canteenId = "canteen_abc",
            name = "Masala Dosa",
            description = "Crispy golden dosa with potato masala",
            price = 55.0,
            category = "Breakfast",
            imageUrl = "https://example.com/dosa.jpg",
            isAvailable = true
        )

        val entity = MenuItemEntity.fromDomain(original)
        assertEquals("item_123", entity.id)
        assertEquals("canteen_abc", entity.canteenId)
        assertEquals("Masala Dosa", entity.name)
        assertEquals(55.0, entity.price, 0.001)
        assertTrue(entity.isAvailable)

        val restored = entity.toDomain()
        assertEquals(original, restored)
    }

    @Test
    fun orderHistoryEntity_conversion_withRelations_preservesData() {
        val testCanteen = Canteen(
            id = "canteen_main",
            name = "Main Campus Canteen",
            location = "Building A",
            isOpen = true
        )

        val testItem = MenuItem(
            id = "item_456",
            canteenId = "canteen_main",
            name = "Cold Coffee",
            description = "Chilled brewed coffee",
            price = 40.0,
            category = "Beverages",
            isAvailable = true
        )

        val orderItem = OrderItem(
            id = "oi_789",
            orderId = "order_999",
            menuItemId = "item_456",
            quantity = 2,
            unitPrice = 40.0,
            menuItem = testItem
        )

        val domainOrder = Order(
            id = "order_999",
            studentId = "student_user_1",
            canteenId = "canteen_main",
            status = "PICKUP_ACTIVE",
            totalAmount = 80.0,
            activatedAt = "2026-11-08T12:00:00Z",
            expiresAt = "2026-11-08T12:10:00Z",
            recoveryCode = "RECOV123",
            reopenCount = 0,
            createdAt = "2026-11-08T11:55:00Z",
            items = listOf(orderItem),
            canteen = testCanteen
        )

        val entity = OrderHistoryEntity.fromDomain(domainOrder)
        assertEquals("order_999", entity.orderId)
        assertEquals("student_user_1", entity.userId)
        assertEquals("PICKUP_ACTIVE", entity.status)
        assertEquals(80.0, entity.totalAmount, 0.001)
        assertEquals("RECOV123", entity.recoveryCode)
        assertEquals("Main Campus Canteen", entity.canteenName)

        val restored = entity.toDomain()
        assertEquals("order_999", restored.id)
        assertEquals("student_user_1", restored.studentId)
        assertEquals("PICKUP_ACTIVE", restored.status)
        assertEquals(80.0, restored.totalAmount, 0.001)
        assertEquals("RECOV123", restored.recoveryCode)
        assertEquals(1, restored.items.size)
        assertEquals("Cold Coffee", restored.items.first().menuItem?.name)
        assertEquals(2, restored.items.first().quantity)
        assertEquals("Main Campus Canteen", restored.canteen?.name)
    }

    @Test
    fun cartItemEntity_totalPrice_and_equality() {
        val cartItem = CartItemEntity(
            userId = "student_1",
            menuItemId = "item_777",
            canteenId = "canteen_1",
            name = "Paneer Roll",
            price = 70.0,
            category = "Snacks",
            quantity = 3,
            imageUrl = null
        )

        val total = cartItem.price * cartItem.quantity
        assertEquals(210.0, total, 0.001)
        assertEquals("student_1", cartItem.userId)
        assertEquals(3, cartItem.quantity)
    }

    @Test
    fun favoriteAndRecentlyViewed_fieldsIntegrity() {
        val now = System.currentTimeMillis()
        val fav = FavoriteEntity(
            userId = "user_abc",
            menuItemId = "item_1",
            canteenId = "canteen_1",
            name = "Veg Burger",
            price = 60.0,
            category = "Snacks",
            imageUrl = null,
            isAvailable = true,
            addedAt = now
        )
        assertEquals("user_abc", fav.userId)
        assertEquals("item_1", fav.menuItemId)

        val recent = RecentlyViewedEntity(
            userId = "user_abc",
            menuItemId = "item_1",
            canteenId = "canteen_1",
            name = "Veg Burger",
            price = 60.0,
            category = "Snacks",
            imageUrl = null,
            viewedAt = now
        )
        assertEquals("Veg Burger", recent.name)
        assertEquals(now, recent.viewedAt)
    }
}
