package io.basquiat.api.order.usecase

import io.basquiat.api.order.model.OrderCreateDto
import io.basquiat.global.properties.NatsProperties
import io.basquiat.nats.model.PlaceOrder
import io.basquiat.nats.model.PlaceOrderResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class PlaceOrderUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<PlaceOrder, PlaceOrderResponse>(
        natsConnection,
        props,
        ApiSubject.ORDER_CREATE,
        PlaceOrderResponse::class.java,
    ) {
    fun execute(request: OrderCreateDto): PlaceOrderResponse = sendRequest(request.toPlaceOrder())
}