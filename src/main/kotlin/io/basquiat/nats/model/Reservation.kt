package io.basquiat.nats.model

import io.basquiat.api.reservation.model.ReservationDto
import io.basquiat.global.utils.unableToJoin
import java.time.LocalDateTime

/**
 * 단일 상품 정보 요청
 */
data class PlaceReservation(
    val productId: Long,
    val reserveTime: LocalDateTime,
) {
    fun validateReserveTime() {
        if (reserveTime.isBefore(LocalDateTime.now())) unableToJoin("과거의 시간에는 해적단 합류를 예약할 수 없습니다.")
    }
}

data class PlaceReservationResponse(
    val reservationId: Long? = null,
    val error: String? = null,
)

data class FetchReservation(
    val reservationId: Long,
)

data class FetchReservationResponse(
    val reservation: ReservationDto? = null,
    val error: String? = null,
)

data class ReservationAction(
    val reservationId: Long,
)

data class ReservationActionResponse(
    val message: String? = null,
    val error: String? = null,
)

enum class ReservationActionType {
    CONFIRM,
    CANCEL,
}