package fi.metropolia.canopy.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import fi.metropolia.canopy.exception.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityConfig(
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                    ).permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/campuses/**").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
            }
            .oauth2ResourceServer { resourceServer ->
                resourceServer.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { request, response, _ ->
                        writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized", request)
                    }
                    .accessDeniedHandler { request, response, _ ->
                        writeError(response, HttpStatus.FORBIDDEN, "Forbidden", request)
                    }
            }
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    fun jwtEncoder(jwtSecretKey: SecretKey): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(jwtSecretKey))

    @Bean
    fun jwtDecoder(
        jwtSecretKey: SecretKey,
        securityProperties: SecurityProperties,
    ): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .also { decoder ->
                decoder.setJwtValidator(
                    JwtValidators.createDefaultWithIssuer(securityProperties.jwt.issuer),
                )
            }

    @Bean
    fun jwtSecretKey(securityProperties: SecurityProperties): SecretKey {
        val configuredSecret = securityProperties.jwt.secret?.takeIf { it.isNotBlank() }
        val keyBytes = if (configuredSecret == null) {
            ByteArray(32).also { SecureRandom().nextBytes(it) }
        } else {
            configuredSecret.toByteArray(Charsets.UTF_8).let {
                if (it.size >= 32) it else MessageDigest.getInstance("SHA-256").digest(it)
            }
        }

        return SecretKeySpec(keyBytes, "HmacSHA256")
    }

    private fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
            jwt.getClaimAsString("role")
                ?.let { listOf(SimpleGrantedAuthority("ROLE_$it")) }
                ?: emptyList()
        }
        return converter
    }

    private fun writeError(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            ApiErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI,
            ),
        )
    }
}
