package fi.metropolia.canopy.dto.user

import fi.metropolia.canopy.entity.UserRole
import java.time.LocalDateTime

data class UserResponse(
    val userId: Int,
    val email: String,
    val role: UserRole,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
)
