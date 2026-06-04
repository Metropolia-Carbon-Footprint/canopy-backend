package fi.metropolia.canopy.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must be at most 255 characters")
    val email: String,

    @field:NotBlank(message = "Password must not be blank")
    @field:Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    val password: String,
)

data class LoginRequest(
    @field:NotBlank(message = "Email must not be blank")
    @field:Email(message = "Email must be valid")
    @field:Size(max = 255, message = "Email must be at most 255 characters")
    val email: String,

    @field:NotBlank(message = "Password must not be blank")
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token must not be blank")
    val refreshToken: String,
)
