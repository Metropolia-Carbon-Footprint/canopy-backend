package fi.metropolia.canopy.controller

import fi.metropolia.canopy.dto.campus.CampusResponse
import fi.metropolia.canopy.dto.campus.CreateCampusRequest
import fi.metropolia.canopy.dto.campus.UpdateCampusRequest
import fi.metropolia.canopy.service.CampusService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/campuses")
class CampusController(
    private val campusService: CampusService
) {
    @GetMapping
    fun getAllCampuses(): List<CampusResponse> =
        campusService.getAllCampuses()

    @GetMapping("/{campusId}")
    fun getCampusById(@PathVariable campusId: Int): CampusResponse =
        campusService.getCampusById(campusId)

    @PostMapping
    fun createCampus(
        @Valid @RequestBody request: CreateCampusRequest
    ): ResponseEntity<CampusResponse> {
        val campus = campusService.createCampus(request)

        return ResponseEntity
            .created(URI.create("/api/campuses/${campus.campusId}"))
            .body(campus)
    }

    @PutMapping("/{campusId}")
    fun updateCampus(
        @PathVariable campusId: Int,
        @Valid @RequestBody request: UpdateCampusRequest
    ): CampusResponse = campusService.updateCampus(campusId, request)

    @DeleteMapping("/{campusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCampus(@PathVariable campusId: Int) {
        campusService.deleteCampus(campusId)
    }
}
