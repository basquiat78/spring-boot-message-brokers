package io.basquiat.domain.reservation.entity

import io.basquiat.domain.product.entity.Product
import io.basquiat.domain.reservation.code.ReservationStatus
import io.basquiat.global.model.entity.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDateTime

@Entity
@DynamicUpdate
@Table(name = "reservation")
class Reservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    val product: Product,
    @Column(name = "reserved_at", nullable = false, updatable = true)
    val reservedAt: LocalDateTime,
    @Enumerated(EnumType.STRING)
    var status: ReservationStatus = ReservationStatus.PENDING,
) : BaseTimeEntity()