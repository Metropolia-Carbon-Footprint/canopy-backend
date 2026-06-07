package fi.metropolia.canopy.repository

import fi.metropolia.canopy.entity.Campus
import org.springframework.data.jpa.repository.JpaRepository

interface CampusRepository : JpaRepository<Campus, Int> {
    fun findAllByDeletedAtIsNullOrderByNameAsc(): List<Campus>

    fun findByCampusIdAndDeletedAtIsNull(campusId: Int): Campus?

    fun findByNameAndDeletedAtIsNull(name: String): Campus?
}