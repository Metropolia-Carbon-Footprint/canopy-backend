package fi.metropolia.canopy.repository

import fi.metropolia.canopy.entity.TripSegment
import org.springframework.data.jpa.repository.JpaRepository

interface TripSegmentRepository : JpaRepository<TripSegment, Int>