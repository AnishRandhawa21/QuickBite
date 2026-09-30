package com.anish.quickbite.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserRole {
    STUDENT,
    ADMIN
}

@Serializable
data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val role: UserRole = UserRole.STUDENT,
    @SerialName("created_at") val createdAt: String? = null
)
