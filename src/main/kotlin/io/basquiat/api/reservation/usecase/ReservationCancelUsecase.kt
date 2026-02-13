package io.basquiat.api.reservation.usecase

import io.basquiat.global.properties.NatsProperties
import io.basquiat.nats.model.ReservationAction
import io.basquiat.nats.model.ReservationActionResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class ReservationCancelUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<ReservationAction, ReservationActionResponse>(
        natsConnection,
        props,
        ApiSubject.RESERVATION_CANCEL,
        ReservationActionResponse::class.java,
    ) {
    fun execute(id: Long): ReservationActionResponse = sendRequest(ReservationAction(reservationId = id))
}