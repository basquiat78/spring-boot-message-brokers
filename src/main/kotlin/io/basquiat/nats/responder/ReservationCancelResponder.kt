package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.ReservationAction
import io.basquiat.nats.model.ReservationActionResponse
import io.basquiat.nats.model.ReservationActionType
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.ReservationActionHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class ReservationCancelResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: ReservationActionHandler,
) : AbstractResponder<ReservationActionResponse>(natsConnection, props, taskExecutor, ApiSubject.RESERVATION_CANCEL) {
    override fun handleRequest(msg: Message): ReservationActionResponse {
        val request = byteToObject(msg.data, ReservationAction::class.java)
        return try {
            usecase.execute(request, ReservationActionType.CANCEL)
        } catch (e: Exception) {
            ReservationActionResponse(error = e.message)
        }
    }
}