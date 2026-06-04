package fi.metropolia.canopy.dto.user

import fi.metropolia.canopy.entity.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateUserRequest(
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must be at most 255 characters")
    val email: String,

    val role: UserRole,
)
