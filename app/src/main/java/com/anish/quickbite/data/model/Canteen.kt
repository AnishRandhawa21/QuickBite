package com.anish.quickbite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanteenAdmin(
    val id: String = "",
    @SerialName("admin_id") val adminId: String = "",
    @SerialName("canteen_id") val canteenId: String = "",
    @SerialName("canteens") val canteen: Canteen? = null
)

@Serializable
data class Canteen(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    @SerialName("is_open") val isOpen: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null
)
