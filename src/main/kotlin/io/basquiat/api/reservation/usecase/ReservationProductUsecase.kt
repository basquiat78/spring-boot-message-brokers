package io.basquiat.api.reservation.usecase

import io.basquiat.api.reservation.model.ReservationCreate
import io.basquiat.global.properties.NatsProperties
import io.basquiat.nats.model.PlaceReservation
import io.basquiat.nats.model.PlaceReservationResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class ReservationProductUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<PlaceReservation, PlaceReservationResponse>(
        natsConnection,
        props,
        ApiSubject.RESERVATION_CREATE,
        PlaceReservationResponse::class.java,
    ) {
    fun execute(request: ReservationCreate): PlaceReservationResponse = sendRequest(request.toPlaceReservation())
}