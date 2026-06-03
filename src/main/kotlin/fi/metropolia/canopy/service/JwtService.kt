package fi.metropolia.canopy.service

import fi.metropolia.canopy.config.SecurityProperties
import fi.metropolia.canopy.entity.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.util.UUID

data class AccessToken(
    val tokenValue: String,
    val expiresInSeconds: Long,
)

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val securityProperties: SecurityProperties,
) {
    private val clock: Clock = Clock.systemUTC()

    fun createAccessToken(user: User): AccessToken {
        val issuedAt = Instant.now(clock)
        val expiresAt = issuedAt.plus(securityProperties.jwt.accessTtl)
        val claims = JwtClaimsSet.builder()
            .issuer(securityProperties.jwt.issuer)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.userId.toString())
            .id(UUID.randomUUID().toString())
            .claim("role", user.role.name)
            .build()

        return AccessToken(
            tokenValue = jwtEncoder.encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    claims,
                ),
            ).tokenValue,
            expiresInSeconds = securityProperties.jwt.accessTtl.seconds,
        )
    }
}
