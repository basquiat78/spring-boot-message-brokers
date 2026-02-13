package io.basquiat.api.reservation

import io.basquiat.api.reservation.model.ReservationCreate
import io.basquiat.api.reservation.model.ReservationDto
import io.basquiat.api.reservation.usecase.FetchReservationUsecase
import io.basquiat.api.reservation.usecase.ReservationCancelUsecase
import io.basquiat.api.reservation.usecase.ReservationConfirmUsecase
import io.basquiat.api.reservation.usecase.ReservationProductUsecase
import io.basquiat.nats.model.PlaceReservationResponse
import io.basquiat.nats.model.ReservationActionResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "NATS Reservation request Controller")
class ReservationController(
    private val reservationProductUsecase: ReservationProductUsecase,
    private val fetchReservationUsecase: FetchReservationUsecase,
    private val reservationConfirmUsecase: ReservationConfirmUsecase,
    private val reservationCancelUsecase: ReservationCancelUsecase,
) {
    @PostMapping("")
    fun placeReservation(
        @RequestBody request: ReservationCreate,
    ): PlaceReservationResponse = reservationProductUsecase.execute(request)

    @GetMapping("/{id}")
    fun fetchReservation(
        @PathVariable("id") id: Long,
    ): ReservationDto = fetchReservationUsecase.execute(id)

    @PostMapping("/{id}/confirm")
    fun confirmReservation(
        @PathVariable("id") id: Long,
    ): ReservationActionResponse = reservationConfirmUsecase.execute(id)

    @PostMapping("/{id}/cancel")
    fun cancelReservation(
        @PathVariable("id") id: Long,
    ): ReservationActionResponse = reservationCancelUsecase.execute(id)
}