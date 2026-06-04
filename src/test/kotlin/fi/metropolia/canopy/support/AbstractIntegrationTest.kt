package fi.metropolia.canopy.support

import com.jayway.jsonpath.JsonPath
import fi.metropolia.canopy.dto.user.normalizedEmail
import fi.metropolia.canopy.entity.UserRole
import fi.metropolia.canopy.repository.UserRepository
import fi.metropolia.canopy.service.InMemoryRefreshTokenService
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
abstract class AbstractIntegrationTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var refreshTokenService: InMemoryRefreshTokenService

    @Autowired
    protected lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        jdbc.execute(
            """
            TRUNCATE TABLE
                trip_segments,
                trips,
                campuses,
                user_identities,
                local_credentials,
                users
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
        refreshTokenService.clear()
    }

    protected fun registerUser(email: String, password: String = "Password123!"): Int {
        val result = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "$email", "password": "$password" }"""),
        )
            .andExpect(status().isCreated)
            .andReturn()

        return JsonPath.read(result.response.contentAsString, "$.userId")
    }

    protected fun loginUser(email: String, password: String = "Password123!"): TestAuthTokens {
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "$email", "password": "$password" }"""),
        )
            .andExpect(status().isOk)
            .andReturn()

        val body = result.response.contentAsString
        return TestAuthTokens(
            accessToken = JsonPath.read(body, "$.accessToken"),
            refreshToken = JsonPath.read(body, "$.refreshToken"),
            expiresInSeconds = JsonPath.read<Int>(body, "$.expiresInSeconds").toLong(),
        )
    }

    protected fun registerAndLogin(
        email: String,
        password: String = "Password123!",
    ): TestAuthTokens {
        registerUser(email, password)
        return loginUser(email, password)
    }

    protected fun promoteToAdmin(email: String) {
        val user = requireNotNull(userRepository.findByEmail(email.normalizedEmail()))
        user.role = UserRole.ADMIN
        userRepository.save(user)
    }

    protected fun bearer(accessToken: String): String =
        "Bearer $accessToken"
}

data class TestAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
)
