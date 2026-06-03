package fi.metropolia.canopy.dto.campus

import fi.metropolia.canopy.entity.Campus

fun CreateCampusRequest.toEntity(): Campus = Campus(
    name = name.trim(),
    city = city.normalizedOrNull(),
    latitude = latitude,
    longitude = longitude
)

fun Campus.updateFrom(request: UpdateCampusRequest) {
    name = request.name.trim()
    city = request.city.normalizedOrNull()
    latitude = request.latitude
    longitude = request.longitude
}

fun Campus.toResponse(): CampusResponse = CampusResponse(
    campusId = campusId,
    name = name,
    city = city,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun String?.normalizedOrNull(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
