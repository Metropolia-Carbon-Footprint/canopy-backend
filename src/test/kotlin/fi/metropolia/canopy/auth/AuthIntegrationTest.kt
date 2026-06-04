package fi.metropolia.canopy.auth

import com.jayway.jsonpath.JsonPath
import fi.metropolia.canopy.entity.UserRole
import fi.metropolia.canopy.repository.LocalCredentialRepository
import fi.metropolia.canopy.support.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class AuthIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var localCredentialRepository: LocalCredentialRepository

    @Test
    fun `register stores encoded local credential and rejects duplicate email case-insensitively`() {
        val userId = registerUser("Student@Example.com")

        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)

        val user = requireNotNull(userRepository.findById(userId).orElse(null))
        val credential = requireNotNull(localCredentialRepository.findByUserUserId(userId))
        assertTrue(user.email == "student@example.com")
        assertTrue(user.role == UserRole.USER)
        assertNotEquals("Password123!", credential.passwordHash)
        assertTrue(credential.passwordHash.startsWith("{bcrypt}"))

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "STUDENT@example.com", "password": "Password123!" }"""),
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `login me refresh rotation and logout work`() {
        val email = "student@example.com"
        registerUser(email)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "$email", "password": "wrong-password" }"""),
        )
            .andExpect(status().isUnauthorized)

        val tokens = loginUser(email)
        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())
        assertTrue(tokens.expiresInSeconds == 900L)

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", bearer(tokens.accessToken)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.email").value(email))
            .andExpect(jsonPath("\$.role").value("USER"))

        val refreshResult = mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "refreshToken": "${tokens.refreshToken}" }"""),
        )
            .andExpect(status().isOk)
            .andReturn()

        val refreshedBody = refreshResult.response.contentAsString
        val refreshedAccessToken: String = JsonPath.read(refreshedBody, "$.accessToken")
        val refreshedRefreshToken: String = JsonPath.read(refreshedBody, "$.refreshToken")
        assertNotEquals(tokens.accessToken, refreshedAccessToken)
        assertNotEquals(tokens.refreshToken, refreshedRefreshToken)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "refreshToken": "${tokens.refreshToken}" }"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "refreshToken": "$refreshedRefreshToken" }"""),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "refreshToken": "$refreshedRefreshToken" }"""),
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `soft-deleted user cannot log in refresh or use me`() {
        val email = "deleted@example.com"
        val tokens = registerAndLogin(email)
        val user = requireNotNull(userRepository.findByEmail(email))
        user.deletedAt = LocalDateTime.now()
        userRepository.save(user)

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "$email", "password": "Password123!" }"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "refreshToken": "${tokens.refreshToken}" }"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            get("/api/auth/me")
                .header("Authorization", bearer(tokens.accessToken)),
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `campus reads stay public and campus writes require admin`() {
        val userTokens = registerAndLogin("user@example.com")
        val adminEmail = "admin@example.com"
        registerUser(adminEmail)
        promoteToAdmin(adminEmail)
        val adminTokens = loginUser(adminEmail)

        mockMvc.perform(get("/api/campuses"))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/api/campuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "No Auth Campus" }"""),
        )
            .andExpect(status().isUnauthorized)

        mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(userTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "User Campus" }"""),
        )
            .andExpect(status().isForbidden)

        val created = mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "Admin Campus" }"""),
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andReturn()

        val campusId = requireNotNull(created.response.getHeader("Location"))
            .substringAfterLast('/')
            .toInt()
        mockMvc.perform(get("/api/campuses/{campusId}", campusId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.name").value("Admin Campus"))
    }

    @Test
    fun `admin user crud happy path works and non-admin is rejected`() {
        val userTokens = registerAndLogin("user@example.com")
        val adminEmail = "admin@example.com"
        registerUser(adminEmail)
        promoteToAdmin(adminEmail)
        val adminTokens = loginUser(adminEmail)

        mockMvc.perform(
            get("/api/users")
                .header("Authorization", bearer(userTokens.accessToken)),
        )
            .andExpect(status().isForbidden)

        val createResult = mockMvc.perform(
            post("/api/users")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "managed@example.com",
                      "password": "Password123!",
                      "role": "ADMIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("\$.email").value("managed@example.com"))
            .andExpect(jsonPath("\$.role").value("ADMIN"))
            .andReturn()

        val managedUserId: Int = JsonPath.read(createResult.response.contentAsString, "$.userId")
        assertNotNull(localCredentialRepository.findByUserUserId(managedUserId))

        mockMvc.perform(
            get("/api/users/{userId}", managedUserId)
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.userId").value(managedUserId))

        mockMvc.perform(
            put("/api/users/{userId}", managedUserId)
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "email": "updated@example.com", "role": "USER" }"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.email").value("updated@example.com"))
            .andExpect(jsonPath("\$.role").value("USER"))

        mockMvc.perform(
            get("/api/users")
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$[?(@.email == 'updated@example.com')]").exists())

        mockMvc.perform(
            delete("/api/users/{userId}", managedUserId)
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/users/{userId}", managedUserId)
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isNotFound)
    }
}