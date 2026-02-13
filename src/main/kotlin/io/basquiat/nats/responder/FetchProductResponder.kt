package io.basquiat.nats.responder

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.nats.model.FetchProduct
import io.basquiat.nats.model.FetchProductResponse
import io.basquiat.nats.responder.common.AbstractResponder
import io.basquiat.nats.responder.handler.FetchProductHandler
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.Message
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component

@Component
class FetchProductResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: FetchProductHandler,
) : AbstractResponder<FetchProductResponse>(natsConnection, props, taskExecutor, ApiSubject.PRODUCT_FETCH) {
    override fun handleRequest(msg: Message): FetchProductResponse {
        val request = byteToObject(msg.data, FetchProduct::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            FetchProductResponse(error = e.message)
        }
    }
}