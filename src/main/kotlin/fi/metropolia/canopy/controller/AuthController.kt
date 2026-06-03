package fi.metropolia.canopy.controller

import fi.metropolia.canopy.dto.auth.AuthTokenResponse
import fi.metropolia.canopy.dto.auth.LoginRequest
import fi.metropolia.canopy.dto.auth.RefreshTokenRequest
import fi.metropolia.canopy.dto.auth.RegisterRequest
import fi.metropolia.canopy.dto.user.UserResponse
import fi.metropolia.canopy.security.currentUserId
import fi.metropolia.canopy.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): UserResponse =
        authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthTokenResponse =
        authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthTokenResponse =
        authService.refresh(request.refreshToken)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.logout(request.refreshToken)
    }

    @GetMapping("/me")
    fun me(authentication: Authentication): UserResponse =
        authService.currentUser(authentication.currentUserId())
}
