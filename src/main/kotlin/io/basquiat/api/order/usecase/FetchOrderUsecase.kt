package io.basquiat.api.order.usecase

import io.basquiat.api.order.model.OrderDto
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.natsResponse
import io.basquiat.nats.model.FetchOrder
import io.basquiat.nats.model.FetchOrderResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class FetchOrderUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<FetchOrder, FetchOrderResponse>(
        natsConnection,
        props,
        ApiSubject.ORDER_FETCH,
        FetchOrderResponse::class.java,
    ) {
    fun execute(id: Long): OrderDto {
        val response: FetchOrderResponse = sendRequest(FetchOrder(orderId = id))
        return response.order ?: natsResponse(response.error)
    }
}