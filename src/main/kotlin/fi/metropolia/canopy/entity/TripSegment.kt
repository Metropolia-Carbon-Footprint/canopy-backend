package fi.metropolia.canopy.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "trip_segments",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_trip_segment_order",
            columnNames = ["trip_id", "segmentOrder"]
        )
    ]
)
class TripSegment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var tripSegmentId: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id")
    var trip: Trip,

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false)
    var transportMode: TransportMode,

    @Column(name = "distance_m", nullable = false)
    var distanceMeters: BigDecimal,

    @Column(name = "carbon_g", nullable = false)
    var carbonGrams: BigDecimal,

    @Column(nullable = false)
    var segmentOrder: Int,

    @CreationTimestamp
    var createdAt: LocalDateTime? = null,
    @UpdateTimestamp
    var updatedAt: LocalDateTime? = null
)