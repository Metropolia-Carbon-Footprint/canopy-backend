package fi.metropolia.canopy.dto.trip

import java.math.BigDecimal
import java.time.LocalDateTime

// DTO for PATCH requests. All fields are nullable to allow for partial updates.
data class PatchTripRequest(
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
    val destinationCampusName: String? = null,
    val segments: List<TripSegmentDto>? = null
)