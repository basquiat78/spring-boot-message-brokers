package io.basquiat.api.reservation.usecase

import io.basquiat.api.reservation.model.ReservationDto
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.natsResponse
import io.basquiat.nats.model.FetchReservation
import io.basquiat.nats.model.FetchReservationResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class FetchReservationUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<FetchReservation, FetchReservationResponse>(
        natsConnection,
        props,
        ApiSubject.RESERVATION_FETCH,
        FetchReservationResponse::class.java,
    ) {
    fun execute(id: Long): ReservationDto {
        val response: FetchReservationResponse = sendRequest(FetchReservation(reservationId = id))
        return response.reservation ?: natsResponse(response.error)
    }
}