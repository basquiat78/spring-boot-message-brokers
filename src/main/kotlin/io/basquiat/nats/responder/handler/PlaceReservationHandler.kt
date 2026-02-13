package io.basquiat.nats.responder.handler

import io.basquiat.domain.product.repository.ProductRepository
import io.basquiat.domain.reservation.code.ReservationStatus
import io.basquiat.domain.reservation.entity.Reservation
import io.basquiat.domain.reservation.repository.ReservationRepository
import io.basquiat.global.extensions.findByIdOrThrow
import io.basquiat.nats.model.PlaceReservation
import io.basquiat.nats.model.PlaceReservationResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlaceReservationHandler(
    private val productRepository: ProductRepository,
    private val reservationRepository: ReservationRepository,
) {
    @Transactional
    fun execute(request: PlaceReservation): PlaceReservationResponse {
        request.validateReserveTime()

        val (productId, reserveTime) = request

        val product =
            productRepository
                .findByIdOrThrow(productId, "해당 보물을 찾을 수 없습니다. 보물 아이디: $productId")

        product.reserveProduct(1)

        val reservation =
            Reservation(
                product = product,
                reservedAt = reserveTime,
                status = ReservationStatus.PENDING,
            )
        val completeReservation = reservationRepository.save(reservation)
        return PlaceReservationResponse(reservationId = reservation.id)
    }
}