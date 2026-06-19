package fi.metropolia.canopy.controller

import com.fasterxml.jackson.databind.ObjectMapper
import fi.metropolia.canopy.dto.trip.PatchTripRequest
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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
        createTripForUser(userOneAuth.accessToken, createTripDto())

        val userTwoAuth = registerAndLogin("two@user.com")
        createTripForUser(userTwoAuth.accessToken, createTripDto())

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
        val (createdTripId, _) = createTripForUser(auth.accessToken, createTripDto())

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
        val (createdTripId, _) = createTripForUser(userOneAuth.accessToken, createTripDto())

        // When user two tries to access it, they should get a 404
        val userTwoAuth = registerAndLogin("two@user.com")
        mockMvc.get("/api/trips/$createdTripId") {
            header("Authorization", bearer(userTwoAuth.accessToken))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `should update a trip with PUT`() {
        // Given a user has created a trip
        val auth = registerAndLogin("test@user.com")
        val (tripId, _) = createTripForUser(auth.accessToken, createTripDto())

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
    fun `should partially update a trip with PATCH by appending segments`() {
        // Given a user has created a trip with one segment
        val auth = registerAndLogin("test@user.com")
        val initialSegment = TripSegmentDto(TransportMode.WALKING, BigDecimal(100), BigDecimal(10), 0)
        val initialTripDto = createTripDto(segments = listOf(initialSegment))
        val (tripId, _) = createTripForUser(auth.accessToken, initialTripDto)

        // When they send a PATCH request to add a new segment
        val newSegment = TripSegmentDto(TransportMode.BUS, BigDecimal(1000), BigDecimal(50), 1) // Use a new, unique segmentOrder
        val patchDto = PatchTripRequest(segments = listOf(newSegment))

        mockMvc.patch("/api/trips/$tripId") {
            header("Authorization", bearer(auth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(patchDto)
        }.andExpect {
            status { isOk() }
            jsonPath("$.tripId") { value(tripId) }
            jsonPath("$.segments.length()") { value(2) } // Verify there are now two segments
            jsonPath("$.totalCarbonGrams") { value(60) } // Verify total is recalculated (10 + 50)
            jsonPath("$.segments[0].transportMode") { value("WALKING") }
            jsonPath("$.segments[1].transportMode") { value("BUS") }
        }
    }

    @Test
    fun `should delete a trip`() {
        // Given a user has created a trip
        val auth = registerAndLogin("test@user.com")
        val (tripId, _) = createTripForUser(auth.accessToken, createTripDto())

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

    @Test
    fun `should fail to create a trip with duplicate segment orders`() {
        // Given an authenticated user
        val auth = registerAndLogin("test@user.com")

        // When they submit a trip with two segments having the same order
        val duplicateOrderSegments = listOf(
            TripSegmentDto(TransportMode.WALKING, BigDecimal(100), BigDecimal(10), 0),
            TripSegmentDto(TransportMode.BUS, BigDecimal(1000), BigDecimal(50), 0) // Duplicate order
        )
        val tripDto = createTripDto(segments = duplicateOrderSegments)

        // Then the request should fail due to the unique constraint
        mockMvc.post("/api/trips") {
            header("Authorization", bearer(auth.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(tripDto)
        }.andExpect {
            status { isConflict() }
        }
    }

    private fun createTripForUser(accessToken: String, tripDto: TripSubmissionDto): Pair<Int, LocalDateTime> {
        val result = mockMvc.post("/api/trips") {
            header("Authorization", bearer(accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(tripDto)
        }.andReturn()
        val jsonNode = objectMapper.readTree(result.response.contentAsString)
        val tripId = jsonNode.get("tripId").asInt()
        val startTimeString = jsonNode.get("startTime").asText()
        val startTime = LocalDateTime.parse(startTimeString) // Parse the actual time returned by the API
        return Pair(tripId, startTime)
    }

    private fun createTripDto(
        startTime: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
        endTime: LocalDateTime = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MICROS),
        destinationCampusName: String? = null,
        carbonGrams: BigDecimal = BigDecimal.TEN,
        segments: List<TripSegmentDto> = listOf(
            TripSegmentDto(
                transportMode = TransportMode.WALKING,
                distanceMeters = BigDecimal(100),
                carbonGrams = carbonGrams,
                segmentOrder = 0
            )
        )
    ): TripSubmissionDto {
        return TripSubmissionDto(
            startTime = startTime,
            endTime = endTime,
            destinationCampusName = destinationCampusName,
            segments = segments
        )
    }
}