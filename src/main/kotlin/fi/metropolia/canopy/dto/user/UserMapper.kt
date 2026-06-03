package fi.metropolia.canopy.dto.user

import fi.metropolia.canopy.entity.User
import fi.metropolia.canopy.entity.UserRole
import java.util.Locale

fun CreateUserRequest.toEntity(): User = User(
    email = email.normalizedEmail(),
    role = role,
)

fun User.updateFrom(request: UpdateUserRequest) {
    email = request.email.normalizedEmail()
    role = request.role
}

fun User.toResponse(): UserResponse = UserResponse(
    userId = userId,
    email = email,
    role = role,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun String.normalizedEmail(): String =
    trim().lowercase(Locale.ROOT)
