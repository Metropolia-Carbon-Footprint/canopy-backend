package fi.metropolia.canopy.service

import fi.metropolia.canopy.dto.campus.CampusResponse
import fi.metropolia.canopy.dto.campus.CreateCampusRequest
import fi.metropolia.canopy.dto.campus.UpdateCampusRequest
import fi.metropolia.canopy.dto.campus.toEntity
import fi.metropolia.canopy.dto.campus.toResponse
import fi.metropolia.canopy.dto.campus.updateFrom
import fi.metropolia.canopy.entity.Campus
import fi.metropolia.canopy.exception.InvalidRequestException
import fi.metropolia.canopy.exception.ResourceNotFoundException
import fi.metropolia.canopy.repository.CampusRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CampusService(
    private val campusRepository: CampusRepository
) {
    fun getAllCampuses(): List<CampusResponse> =
        campusRepository.findAllByDeletedAtIsNullOrderByNameAsc()
            .map(Campus::toResponse)

    fun getCampusById(campusId: Int): CampusResponse =
        findActiveCampus(campusId).toResponse()

    @Transactional
    fun createCampus(request: CreateCampusRequest): CampusResponse {
        validateCoordinatePair(request.latitude, request.longitude)

        return campusRepository.save(request.toEntity()).toResponse()
    }

    @Transactional
    fun updateCampus(campusId: Int, request: UpdateCampusRequest): CampusResponse {
        validateCoordinatePair(request.latitude, request.longitude)

        val campus = findActiveCampus(campusId)
        campus.updateFrom(request)

        return campusRepository.save(campus).toResponse()
    }

    @Transactional
    fun deleteCampus(campusId: Int) {
        val campus = findActiveCampus(campusId)
        campus.deletedAt = LocalDateTime.now()
        campusRepository.save(campus)
    }

    private fun findActiveCampus(campusId: Int): Campus =
        campusRepository.findByCampusIdAndDeletedAtIsNull(campusId)
            ?: throw ResourceNotFoundException("Campus", campusId)

    private fun validateCoordinatePair(latitude: BigDecimal?, longitude: BigDecimal?) {
        if ((latitude == null) != (longitude == null)) {
            throw InvalidRequestException(
                "Latitude and longitude must either both be provided or both be omitted"
            )
        }
    }
}
