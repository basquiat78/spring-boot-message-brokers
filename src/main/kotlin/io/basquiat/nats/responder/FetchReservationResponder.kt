package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.FetchReservation
import io.basquiat.nats.model.FetchReservationResponse
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.FetchReservationHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class ReservationCancelProductResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: FetchReservationHandler,
) : AbstractResponder<FetchReservationResponse>(natsConnection, props, taskExecutor, ApiSubject.RESERVATION_FETCH) {
    override fun handleRequest(msg: Message): FetchReservationResponse {
        val request = byteToObject(msg.data, FetchReservation::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            FetchReservationResponse(error = e.message)
        }
    }
}