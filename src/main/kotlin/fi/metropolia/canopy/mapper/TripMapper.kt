package fi.metropolia.canopy.mapper

import fi.metropolia.canopy.dto.trip.TripResponseDto
import fi.metropolia.canopy.dto.trip.TripSegmentResponseDto
import fi.metropolia.canopy.entity.Trip
import fi.metropolia.canopy.entity.TripSegment

fun Trip.toResponseDto(): TripResponseDto = TripResponseDto(
    tripId = this.tripId,
    campusName = this.campus?.name,
    startTime = this.startTime,
    endTime = this.endTime,
    totalDistanceMeters = this.totalDistanceMeters,
    totalCarbonGrams = this.totalCarbonGrams,
    source = this.source,
    segments = this.segments.map { it.toResponseDto() }.sortedBy { it.segmentOrder }
)

fun TripSegment.toResponseDto(): TripSegmentResponseDto = TripSegmentResponseDto(
    tripSegmentId = this.tripSegmentId,
    transportMode = this.transportMode,
    distanceMeters = this.distanceMeters,
    carbonGrams = this.carbonGrams,
    segmentOrder = this.segmentOrder
)