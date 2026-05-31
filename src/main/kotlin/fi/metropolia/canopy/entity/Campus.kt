package fi.metropolia.canopy.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity @Table(name = "campuses")
class Campus(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var campusId: Int = 0,
    var name: String,
    var city: String?,
    var latitude: BigDecimal?,
    var longitude: BigDecimal?,

    @CreationTimestamp
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    var updatedAt: LocalDateTime? = null,

    var deletedAt: LocalDateTime? = null
)