package fi.metropolia.canopy.config

import fi.metropolia.canopy.entity.Campus
import fi.metropolia.canopy.repository.CampusRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class DataSeeder(private val campusRepository: CampusRepository) {

    @PostConstruct
    fun seedData() {
        if (campusRepository.count() == 0L) {
            val campuses = listOf(
                Campus(
                    name = "Karamalmi",
                    city = "Espoo",
                    latitude = BigDecimal("60.2243"),
                    longitude = BigDecimal("24.7582")
                ),
                Campus(
                    name = "Myyrmäki",
                    city = "Vantaa",
                    latitude = BigDecimal("60.2590"),
                    longitude = BigDecimal("24.8480")
                ),
                Campus(
                    name = "Myllypuro",
                    city = "Helsinki",
                    latitude = BigDecimal("60.2239"),
                    longitude = BigDecimal("25.0781")
                ),
                Campus(
                    name = "Arabia",
                    city = "Helsinki",
                    latitude = BigDecimal("60.2113"),
                    longitude = BigDecimal("24.9820")
                )
            )
            campusRepository.saveAll(campuses)
        }
    }
}