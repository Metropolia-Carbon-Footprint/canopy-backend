package fi.metropolia.canopy.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity @Table(name = "trips")
class Trip(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var tripId: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    var campus: Campus? = null,

    @Column(nullable = false)
    var startTime: LocalDateTime,

    var endTime: LocalDateTime? = null,

    @Column(name = "total_distance_m")
    var totalDistanceMeters: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_carbon_g")
    var totalCarbonGrams: BigDecimal = BigDecimal.ZERO,

    var source: String? = null,

    @OneToMany(mappedBy = "trip", cascade = [CascadeType.ALL], orphanRemoval = true)
    var segments: MutableList<TripSegment> = mutableListOf(),

    @CreationTimestamp
    var createdAt: LocalDateTime? = null,
    @UpdateTimestamp
    var updatedAt: LocalDateTime? = null,
    var deletedAt: LocalDateTime? = null
)