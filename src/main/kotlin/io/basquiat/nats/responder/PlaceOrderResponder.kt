package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.PlaceOrder
import io.basquiat.nats.model.PlaceOrderResponse
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.PlaceOrderHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class PlaceOrderResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: PlaceOrderHandler,
) : AbstractResponder<PlaceOrderResponse>(natsConnection, props, taskExecutor, ApiSubject.ORDER_CREATE) {
    override fun handleRequest(msg: Message): PlaceOrderResponse {
        val request = byteToObject(msg.data, PlaceOrder::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            PlaceOrderResponse(error = e.message)
        }
    }
}