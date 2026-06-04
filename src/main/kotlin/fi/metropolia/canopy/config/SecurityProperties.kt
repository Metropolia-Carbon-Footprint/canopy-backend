package fi.metropolia.canopy.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    val jwt: Jwt = Jwt(),
    val refreshToken: RefreshToken = RefreshToken(),
) {
    data class Jwt(
        val issuer: String = "canopy-backend",
        val accessTtl: Duration = Duration.ofMinutes(15),
        val secret: String? = null,
    )

    data class RefreshToken(
        val ttl: Duration = Duration.ofDays(30),
    )
}
