package fi.metropolia.canopy.campus

import fi.metropolia.canopy.support.AbstractIntegrationTest
import fi.metropolia.canopy.support.TestAuthTokens
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class CampusCrudIntegrationTest : AbstractIntegrationTest() {
    private lateinit var adminTokens: TestAuthTokens

    @BeforeEach
    fun createAdmin() {
        val email = "admin@example.com"
        registerUser(email)
        promoteToAdmin(email)
        adminTokens = loginUser(email)
    }

    @Test
    fun `create retrieve update delete and hide campus`() {
        val campusId = createCampus(
            """
            {
              "name": "Myyrmaki Campus",
              "city": "Vantaa",
              "latitude": 60.2589,
              "longitude": 24.8442
            }
            """.trimIndent(),
        )

        mockMvc.perform(get("/api/campuses/{campusId}", campusId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.campusId").value(campusId))
            .andExpect(jsonPath("\$.name").value("Myyrmaki Campus"))
            .andExpect(jsonPath("\$.city").value("Vantaa"))

        mockMvc.perform(
            put("/api/campuses/{campusId}", campusId)
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Myyrmaki Updated",
                      "city": "Vantaa",
                      "latitude": 60.2589,
                      "longitude": 24.8442
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.campusId").value(campusId))
            .andExpect(jsonPath("\$.name").value("Myyrmaki Updated"))

        mockMvc.perform(
            delete("/api/campuses/{campusId}", campusId)
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/campuses/{campusId}", campusId))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/campuses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.length()").value(0))
    }

    @Test
    fun `list returns active campuses ordered by name`() {
        createCampus("""{ "name": "Zeta Campus" }""")
        val deletedCampusId = createCampus("""{ "name": "Beta Campus" }""")
        createCampus("""{ "name": "Alpha Campus" }""")

        mockMvc.perform(
            delete("/api/campuses/{campusId}", deletedCampusId)
                .header("Authorization", bearer(adminTokens.accessToken)),
        )
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/campuses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.length()").value(2))
            .andExpect(jsonPath("\$[0].name").value("Alpha Campus"))
            .andExpect(jsonPath("\$[1].name").value("Zeta Campus"))
    }

    @Test
    fun `invalid campus requests return bad request`() {
        mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "   " }"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.status").value(400))
            .andExpect(jsonPath("\$.fieldErrors.name").exists())

        mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "name": "Karamalmi", "latitude": 60.22 }"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.status").value(400))
            .andExpect(
                jsonPath("\$.message")
                    .value("Latitude and longitude must either both be provided or both be omitted"),
            )

        mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Invalid Longitude",
                      "latitude": 60.22,
                      "longitude": 181
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("\$.fieldErrors.longitude").exists())
    }

    @Test
    fun `missing campus returns not found`() {
        mockMvc.perform(get("/api/campuses/{campusId}", 999))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("\$.status").value(404))
            .andExpect(jsonPath("\$.message").value("Campus with id 999 was not found"))
            .andExpect(jsonPath("\$.path").value("/api/campuses/999"))
    }

    private fun createCampus(requestBody: String): Int {
        val result = mockMvc.perform(
            post("/api/campuses")
                .header("Authorization", bearer(adminTokens.accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody),
        )
            .andExpect(status().isCreated)
            .andExpect(header().exists("Location"))
            .andReturn()

        return requireNotNull(result.response.getHeader("Location"))
            .substringAfterLast('/')
            .toInt()
    }
}