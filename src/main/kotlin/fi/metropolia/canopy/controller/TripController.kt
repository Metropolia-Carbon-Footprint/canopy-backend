package fi.metropolia.canopy.controller

import fi.metropolia.canopy.dto.trip.TripResponseDto
import fi.metropolia.canopy.dto.trip.TripSubmissionDto
import fi.metropolia.canopy.mapper.toResponseDto
import fi.metropolia.canopy.service.TripService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/trips")
class TripController(
    private val tripService: TripService
) {

    @GetMapping
    fun getTrips(@ParameterObject @PageableDefault(size = 20, sort = ["startTime"]) pageable: Pageable): ResponseEntity<Page<TripResponseDto>> {
        val trips = tripService.getTripsForCurrentUser(pageable)
        return ResponseEntity.ok(trips)
    }

    @GetMapping("/{tripId}")
    fun getTripById(@PathVariable tripId: Int): ResponseEntity<TripResponseDto> {
        val trip = tripService.getTripById(tripId)
        return ResponseEntity.ok(trip)
    }

    @Operation(
        summary = "Submit a new trip",
        responses = [
            ApiResponse(
                responseCode = "201",
                description = "Trip created successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = TripResponseDto::class))]
            )
        ]
    )
    @PostMapping
    fun submitTrip(@Valid @RequestBody tripDto: TripSubmissionDto): ResponseEntity<TripResponseDto> {
        val savedTrip = tripService.saveTrip(tripDto)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTrip.toResponseDto())
    }

    @PutMapping("/{tripId}")
    fun updateTrip(@PathVariable tripId: Int, @Valid @RequestBody tripDto: TripSubmissionDto): ResponseEntity<TripResponseDto> {
        val updatedTrip = tripService.updateTrip(tripId, tripDto)
        return ResponseEntity.ok(updatedTrip.toResponseDto())
    }

    @Operation(
        summary = "Delete a trip",
        responses = [
            ApiResponse(responseCode = "204", description = "Trip deleted successfully"),
            ApiResponse(responseCode = "404", description = "Trip not found")
        ]
    )
    @DeleteMapping("/{tripId}")
    fun deleteTrip(@PathVariable tripId: Int): ResponseEntity<Void> {
        tripService.deleteTrip(tripId)
        return ResponseEntity.noContent().build()
    }
}