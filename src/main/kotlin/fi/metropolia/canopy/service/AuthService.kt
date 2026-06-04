package fi.metropolia.canopy.service

import fi.metropolia.canopy.dto.auth.AuthTokenResponse
import fi.metropolia.canopy.dto.auth.LoginRequest
import fi.metropolia.canopy.dto.auth.RegisterRequest
import fi.metropolia.canopy.dto.user.normalizedEmail
import fi.metropolia.canopy.dto.user.toResponse
import fi.metropolia.canopy.entity.User
import fi.metropolia.canopy.entity.UserRole
import fi.metropolia.canopy.exception.ResourceNotFoundException
import fi.metropolia.canopy.exception.UnauthorizedException
import fi.metropolia.canopy.security.AuthenticatedUser
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val jwtService: JwtService,
    private val refreshTokenService: InMemoryRefreshTokenService,
) {
    @Transactional
    fun register(request: RegisterRequest) =
        userService.createLocalUser(
            email = request.email,
            password = request.password,
            role = UserRole.USER,
        ).toResponse()

    fun login(request: LoginRequest): AuthTokenResponse {
        val authenticatedUser = authenticate(request.email, request.password)
        val user = userService.findActiveUser(authenticatedUser.userId)
        return issueTokenPair(user)
    }

    fun refresh(refreshToken: String): AuthTokenResponse {
        val userId = refreshTokenService.consume(refreshToken)
        val user = try {
            userService.findActiveUser(userId)
        } catch (_: ResourceNotFoundException) {
            throw UnauthorizedException("Invalid or expired refresh token")
        }
        return issueTokenPair(user)
    }

    fun logout(refreshToken: String) {
        refreshTokenService.revoke(refreshToken)
    }

    fun currentUser(userId: Int) =
        userService.findActiveUser(userId).toResponse()

    private fun authenticate(email: String, password: String): AuthenticatedUser {
        val authentication = try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(email.normalizedEmail(), password),
            )
        } catch (_: BadCredentialsException) {
            throw UnauthorizedException("Invalid email or password")
        } catch (_: AuthenticationException) {
            throw UnauthorizedException("Invalid email or password")
        }

        return authentication.principal as? AuthenticatedUser
            ?: throw UnauthorizedException("Invalid email or password")
    }

    private fun issueTokenPair(user: User): AuthTokenResponse {
        val accessToken = jwtService.createAccessToken(user)
        val refreshToken = refreshTokenService.issue(user.userId)
        return AuthTokenResponse(
            accessToken = accessToken.tokenValue,
            refreshToken = refreshToken,
            expiresInSeconds = accessToken.expiresInSeconds,
        )
    }
}
