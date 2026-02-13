package io.basquiat.api.reservation.model

import io.basquiat.nats.model.PlaceReservation
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReservationCreate(
    val productId: Long,
    val reserveTime: String,
) {
    fun toPlaceReservation(): PlaceReservation {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val date = LocalDate.parse(this.reserveTime, formatter)
        val localDateTime = date.atStartOfDay()
        return PlaceReservation(productId = productId, reserveTime = localDateTime)
    }
}