package io.basquiat.domain.reservation.service

import io.basquiat.domain.reservation.entity.Reservation
import io.basquiat.domain.reservation.repository.ReservationRepository
import io.basquiat.global.extensions.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReservationService(
    private val repository: ReservationRepository,
) {
    fun create(entity: Reservation): Reservation = repository.save(entity)

    @Transactional(readOnly = true)
    fun findByIdOrThrow(id: Long): Reservation = repository.findByIdOrThrow(id)

    @Transactional(readOnly = true)
    fun findByIdOrNull(id: Long): Reservation? = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    fun findReservationWithProduct(id: Long): Reservation? = repository.findWithProductById(id)
}