package fi.metropolia.canopy.controller

import fi.metropolia.canopy.dto.trip.TripSegmentDto
import fi.metropolia.canopy.dto.trip.TripSubmissionDto
import fi.metropolia.canopy.entity.TransportMode
import fi.metropolia.canopy.repository.TripRepository
import fi.metropolia.canopy.support.AbstractIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.LocalDateTime

class TripIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var tripRepository: TripRepository

    @Test
    fun `should create a trip and return it`() {
        // Given an authenticated user
        val auth = registerAndLogin("test@user.com")

        // When they submit a valid trip
        val tripDto = createTripDto()

        mockMvc.post("/api/trips") {
            header("Authorization", bearer(auth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(tripDto)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.tripId") { exists() }
            jsonPath("$.totalCarbonGrams") { value(10) }
        }

        // Then the trip should be saved
        assertEquals(1, tripRepository.count())
    }

    @Test
    fun `should return a paginated list of trips for the current user`() {
        // Given two users, each with one trip
        val userOneAuth = registerAndLogin("one@user.com")
        mockMvc.post("/api/trips") {
            header("Authorization", bearer(userOneAuth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createTripDto())
        }

        val userTwoAuth = registerAndLogin("two@user.com")
        mockMvc.post("/api/trips") {
            header("Authorization", bearer(userTwoAuth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createTripDto())
        }

        // When getting trips as user one, they should only see their own trip
        mockMvc.get("/api/trips") {
            header("Authorization", bearer(userOneAuth.accessToken))
        }.andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.totalElements") { value(1) }
        }
    }

    @Test
    fun `should return a single trip by id`() {
        // Given a trip created by a user
        val auth = registerAndLogin("test@user.com")
        val createdTripId = createTripForUser(auth.accessToken)

        // When the user requests that trip by ID
        mockMvc.get("/api/trips/$createdTripId") {
            header("Authorization", bearer(auth.accessToken))
        }.andExpect {
            status { isOk() }
            jsonPath("$.tripId") { value(createdTripId) }
        }
    }

    @Test
    fun `should return 404 when getting a trip that does not exist or belongs to another user`() {
        // Given a trip created by user one
        val userOneAuth = registerAndLogin("one@user.com")
        val createdTripId = createTripForUser(userOneAuth.accessToken)

        // When user two tries to access it, they should get a 404
        val userTwoAuth = registerAndLogin("two@user.com")
        mockMvc.get("/api/trips/$createdTripId") {
            header("Authorization", bearer(userTwoAuth.accessToken))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should update a trip`() {
        // Given a user has created a trip
        val auth = registerAndLogin("test@user.com")
        val tripId = createTripForUser(auth.accessToken)

        // When they send a PUT request to update it
        val updatedDto = createTripDto(carbonGrams = BigDecimal(99))
        mockMvc.put("/api/trips/$tripId") {
            header("Authorization", bearer(auth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(updatedDto)
        }.andExpect {
            status { isOk() }
            jsonPath("$.tripId") { value(tripId) }
            jsonPath("$.totalCarbonGrams") { value(99) }
        }
    }

    @Test
    fun `should delete a trip`() {
        // Given a user has created a trip
        val auth = registerAndLogin("test@user.com")
        val tripId = createTripForUser(auth.accessToken)

        // When they send a DELETE request
        mockMvc.delete("/api/trips/$tripId") {
            header("Authorization", bearer(auth.accessToken))
        }.andExpect {
            status { isNoContent() }
        }

        // Then the trip should be soft-deleted
        val trip = tripRepository.findById(tripId).get()
        assertNotNull(trip.deletedAt)
    }

    private fun createTripForUser(accessToken: String): Int {
        val result = mockMvc.post("/api/trips") {
            header("Authorization", bearer(accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(createTripDto())
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("tripId").asInt()
    }

    private fun createTripDto(
        startTime: LocalDateTime = LocalDateTime.now(),
        endTime: LocalDateTime = LocalDateTime.now().plusHours(1),
        destinationCampusName: String? = null,
        carbonGrams: BigDecimal = BigDecimal.TEN
    ): TripSubmissionDto {
        return TripSubmissionDto(
            startTime = startTime,
            endTime = endTime,
            destinationCampusName = destinationCampusName,
            segments = listOf(
                TripSegmentDto(
                    transportMode = TransportMode.WALKING,
                    distanceMeters = BigDecimal(100),
                    carbonGrams = carbonGrams,
                    segmentOrder = 0
                )
            )
        )
    }
}