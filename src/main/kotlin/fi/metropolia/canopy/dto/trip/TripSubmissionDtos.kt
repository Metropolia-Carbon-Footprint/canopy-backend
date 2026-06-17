package fi.metropolia.canopy.dto.trip

import fi.metropolia.canopy.entity.TransportMode
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime

data class TripSegmentDto(
    @field:NotNull
    val transportMode: TransportMode,
    @field:NotNull
    val distanceMeters: BigDecimal,
    @field:NotNull
    val carbonGrams: BigDecimal,
    val segmentOrder: Int
)

data class TripSubmissionDto(
    @field:NotNull val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val destinationCampusName: String?,
    @field:NotEmpty val segments: List<TripSegmentDto>
)