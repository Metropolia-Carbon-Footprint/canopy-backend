package fi.metropolia.canopy.service

import fi.metropolia.canopy.config.SecurityProperties
import fi.metropolia.canopy.exception.UnauthorizedException
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

data class RefreshTokenRecord(
    val userId: Int,
    val expiresAt: Instant,
)

@Service
class InMemoryRefreshTokenService(
    private val securityProperties: SecurityProperties,
) {
    private val secureRandom = SecureRandom()
    private val clock: Clock = Clock.systemUTC()
    private val records = ConcurrentHashMap<String, RefreshTokenRecord>()

    fun issue(userId: Int): String {
        val rawToken = generateToken()
        records[hash(rawToken)] = RefreshTokenRecord(
            userId = userId,
            expiresAt = Instant.now(clock).plus(securityProperties.refreshToken.ttl),
        )
        return rawToken
    }

    fun consume(rawToken: String): Int {
        val hashedToken = hash(rawToken)
        val record = records.remove(hashedToken)
            ?: throw UnauthorizedException("Invalid or expired refresh token")

        if (record.expiresAt.isBefore(Instant.now(clock))) {
            throw UnauthorizedException("Invalid or expired refresh token")
        }

        return record.userId
    }

    fun revoke(rawToken: String) {
        records.remove(hash(rawToken))
    }

    fun clear() {
        records.clear()
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
