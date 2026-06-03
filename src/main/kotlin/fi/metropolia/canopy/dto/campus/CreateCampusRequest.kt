package fi.metropolia.canopy.dto.campus

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateCampusRequest(
    @field:NotBlank(message = "Campus name must not be blank")
    @field:Size(max = 255, message = "Campus name must be at most 255 characters")
    val name: String,

    @field:Size(max = 100, message = "City must be at most 100 characters")
    val city: String? = null,

    @field:DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be at most 90")
    @field:Digits(integer = 3, fraction = 6, message = "Latitude must have at most 6 decimal places")
    val latitude: BigDecimal? = null,

    @field:DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be at most 180")
    @field:Digits(integer = 3, fraction = 6, message = "Longitude must have at most 6 decimal places")
    val longitude: BigDecimal? = null
)
