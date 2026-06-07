package fi.metropolia.canopy.repository

import fi.metropolia.canopy.entity.Trip
import fi.metropolia.canopy.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface TripRepository : JpaRepository<Trip, Int> {
    fun findByTripIdAndUserAndDeletedAtIsNull(tripId: Int, user: User): Trip?

    fun findAllByUserAndDeletedAtIsNull(user: User, pageable: Pageable): Page<Trip>
}