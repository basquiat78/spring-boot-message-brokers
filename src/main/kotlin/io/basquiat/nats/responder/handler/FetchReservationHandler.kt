package io.basquiat.nats.responder.handler

import io.basquiat.api.reservation.model.ReservationDto
import io.basquiat.domain.reservation.service.ReservationService
import io.basquiat.nats.model.FetchReservation
import io.basquiat.nats.model.FetchReservationResponse
import org.springframework.stereotype.Service

@Service
class FetchReservationHandler(
    private val reservationService: ReservationService,
) {
    fun execute(request: FetchReservation): FetchReservationResponse {
        val reservationId = request.reservationId
        val reservation =
            reservationService
                .findByIdOrThrow(reservationId, "해적단 합류 예약 목록을 찾을 수 없습니다. 해적단 합류 예약 아이디: $reservationId")
        return FetchReservationResponse(reservation = ReservationDto.toDto(reservation))
    }
}