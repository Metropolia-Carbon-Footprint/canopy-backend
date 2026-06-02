package fi.metropolia.canopy.dto.campus

import java.math.BigDecimal
import java.time.LocalDateTime

data class CampusResponse(
    val campusId: Int,
    val name: String,
    val city: String?,
    val latitude: BigDecimal?,
    val longitude: BigDecimal?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)
