package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.FetchOrder
import io.basquiat.nats.model.FetchOrderResponse
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.FetchOrderHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class FetchOrderResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: FetchOrderHandler,
) : AbstractResponder<FetchOrderResponse>(natsConnection, props, taskExecutor, ApiSubject.ORDER_FETCH) {
    override fun handleRequest(msg: Message): FetchOrderResponse {
        val request = byteToObject(msg.data, FetchOrder::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            FetchOrderResponse(error = e.message)
        }
    }
}