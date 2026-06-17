package fi.metropolia.canopy.dto.trip

import fi.metropolia.canopy.entity.TransportMode
import java.math.BigDecimal
import java.time.LocalDateTime

data class TripSegmentResponseDto(
    val tripSegmentId: Int,
    val transportMode: TransportMode,
    val distanceMeters: BigDecimal,
    val carbonGrams: BigDecimal,
    val segmentOrder: Int
)

data class TripResponseDto(
    val tripId: Int,
    val campusName: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val totalDistanceMeters: BigDecimal,
    val totalCarbonGrams: BigDecimal,
    val source: String?,
    val segments: List<TripSegmentResponseDto>
)