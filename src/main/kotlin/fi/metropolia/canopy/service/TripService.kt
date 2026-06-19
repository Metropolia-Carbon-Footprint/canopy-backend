package fi.metropolia.canopy.service

import fi.metropolia.canopy.dto.trip.PatchTripRequest
import fi.metropolia.canopy.dto.trip.TripSubmissionDto
import fi.metropolia.canopy.dto.trip.TripResponseDto
import fi.metropolia.canopy.entity.*
import fi.metropolia.canopy.exception.ConflictException
import fi.metropolia.canopy.exception.ResourceNotFoundException
import fi.metropolia.canopy.mapper.toResponseDto
import fi.metropolia.canopy.repository.CampusRepository
import fi.metropolia.canopy.repository.TripRepository
import fi.metropolia.canopy.repository.UserRepository
import fi.metropolia.canopy.security.currentUserId
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class TripService(
    private val tripRepository: TripRepository,
    private val campusRepository: CampusRepository,
    private val userRepository: UserRepository
) {

    fun getTripsForCurrentUser(pageable: Pageable): Page<TripResponseDto> {
        val user = getCurrentUser()

        return tripRepository.findAllByUserAndDeletedAtIsNull(user, pageable)
            .map { it.toResponseDto() }
    }

    fun getTripById(tripId: Int): TripResponseDto {
        val user = getCurrentUser()

        return tripRepository.findByTripIdAndUserAndDeletedAtIsNull(tripId, user)
            ?.toResponseDto()
            ?: throw ResourceNotFoundException("Trip", tripId)
    }

    @Transactional
    fun saveTrip(dto: TripSubmissionDto, user: User? = null): Trip {
        val finalUser = user ?: getCurrentUser()

        val campus: Campus? = dto.destinationCampusName?.let {
            campusRepository.findByNameAndDeletedAtIsNull(it)
        }

        val trip = Trip(
            user = finalUser,
            campus = campus,
            startTime = dto.startTime,
            endTime = dto.endTime,
            source = "MOBILE_APP",
            totalDistanceMeters = dto.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.distanceMeters },
            totalCarbonGrams = dto.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.carbonGrams }
        )

        trip.segments = dto.segments.map { segmentDto ->
            TripSegment(
                trip = trip,
                transportMode = segmentDto.transportMode,
                distanceMeters = segmentDto.distanceMeters,
                carbonGrams = segmentDto.carbonGrams,
                segmentOrder = segmentDto.segmentOrder
            )
        }.toMutableList()

        try {
            return tripRepository.save(trip)
        } catch (ex: DataIntegrityViolationException) {
            if (ex.cause is ConstraintViolationException) {
                throw ConflictException("A trip segment with the same order already exists for this trip.")
            }
            throw ex
        }
    }

    @Transactional
    fun updateTrip(tripId: Int, dto: TripSubmissionDto): Trip {
        val user = getCurrentUser()
        val trip = tripRepository.findByTripIdAndUserAndDeletedAtIsNull(tripId, user)
            ?: throw ResourceNotFoundException("Trip", tripId)

        val campus: Campus? = dto.destinationCampusName?.let {
            campusRepository.findByNameAndDeletedAtIsNull(it)
        }

        trip.campus = campus
        trip.startTime = dto.startTime
        trip.endTime = dto.endTime
        trip.totalDistanceMeters = dto.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.distanceMeters }
        trip.totalCarbonGrams = dto.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.carbonGrams }
        
        trip.segments.clear()
        tripRepository.flush() // Force the DELETE statements to be executed now

        trip.segments.addAll(dto.segments.map { segmentDto ->
            TripSegment(
                trip = trip,
                transportMode = segmentDto.transportMode,
                distanceMeters = segmentDto.distanceMeters,
                carbonGrams = segmentDto.carbonGrams,
                segmentOrder = segmentDto.segmentOrder
            )
        })

        try {
            return tripRepository.save(trip)
        } catch (ex: DataIntegrityViolationException) {
            if (ex.cause is ConstraintViolationException) {
                throw ConflictException("A trip segment with the same order already exists for this trip.")
            }
            throw ex
        }
    }

    @Transactional
    fun patchTrip(tripId: Int, dto: PatchTripRequest): Trip {
        val user = getCurrentUser()
        val trip = tripRepository.findByTripIdAndUserAndDeletedAtIsNull(tripId, user)
            ?: throw ResourceNotFoundException("Trip", tripId)

        dto.destinationCampusName?.let {
            trip.campus = campusRepository.findByNameAndDeletedAtIsNull(it)
        }
        dto.startTime?.let { trip.startTime = it }
        dto.endTime?.let { trip.endTime = it }

        dto.segments?.let { newSegments ->
            // Append new segments to the existing list
            trip.segments.addAll(newSegments.map { segmentDto ->
                TripSegment(
                    trip = trip,
                    transportMode = segmentDto.transportMode,
                    distanceMeters = segmentDto.distanceMeters,
                    carbonGrams = segmentDto.carbonGrams,
                    segmentOrder = segmentDto.segmentOrder
                )
            })
            // Recalculate totals based on the full, updated list of segments
            trip.totalDistanceMeters = trip.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.distanceMeters }
            trip.totalCarbonGrams = trip.segments.fold(BigDecimal.ZERO) { acc, segment -> acc + segment.carbonGrams }
        }

        try {
            return tripRepository.save(trip)
        } catch (ex: DataIntegrityViolationException) {
            if (ex.cause is ConstraintViolationException) {
                throw ConflictException("A trip segment with the same order already exists for this trip.")
            }
            throw ex
        }
    }

    @Transactional
    fun deleteTrip(tripId: Int) {
        val user = getCurrentUser()
        val trip = tripRepository.findByTripIdAndUserAndDeletedAtIsNull(tripId, user)
            ?: throw ResourceNotFoundException("Trip", tripId)

        trip.deletedAt = LocalDateTime.now()
        tripRepository.save(trip)
    }

    private fun getCurrentUser(): User {
        val userId = SecurityContextHolder.getContext().authentication?.currentUserId()
            ?: throw IllegalStateException("User ID not found in security context. Is the user authenticated?")
        return userRepository.findById(userId)
            .orElseThrow { UsernameNotFoundException("User not found with id: $userId") }
    }
}