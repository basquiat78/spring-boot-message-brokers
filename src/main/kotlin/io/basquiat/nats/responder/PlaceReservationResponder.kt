package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.PlaceReservation
import io.basquiat.nats.model.PlaceReservationResponse
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.PlaceReservationHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class PlaceReservationResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: PlaceReservationHandler,
) : AbstractResponder<PlaceReservationResponse>(natsConnection, props, taskExecutor, ApiSubject.RESERVATION_CREATE) {
    override fun handleRequest(msg: Message): PlaceReservationResponse {
        val request = byteToObject(msg.data, PlaceReservation::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            PlaceReservationResponse(error = e.message)
        }
    }
}