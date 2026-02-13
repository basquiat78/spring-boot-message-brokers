package io.basquiat.domain.reservation.repository

import io.basquiat.domain.reservation.entity.Reservation
import io.basquiat.global.repository.BaseRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservationRepository : BaseRepository<Reservation, Long> {
    @Query(
        """
        SELECT reservation From Reservation reservation
        JOIN FETCH reservation.product
        WHERE reservation.id = :id
        """,
    )
    fun findWithProductById(
        @Param("id") id: Long,
    ): Reservation?
}