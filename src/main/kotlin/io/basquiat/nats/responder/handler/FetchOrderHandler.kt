package io.basquiat.nats.responder.handler

import io.basquiat.api.order.model.OrderDto
import io.basquiat.domain.orders.repository.OrderRepository
import io.basquiat.global.utils.notFound
import io.basquiat.nats.model.FetchOrder
import io.basquiat.nats.model.FetchOrderResponse
import org.springframework.stereotype.Service

@Service
class FetchOrderHandler(
    private val orderRepository: OrderRepository,
) {
    fun execute(request: FetchOrder): FetchOrderResponse {
        val order =
            orderRepository
                .findWithProductById(request.orderId) ?: notFound("해적단 합류 목록을 찾을 수 없습니다. 해적단 합류 아이디: ${request.orderId}")
        return FetchOrderResponse(order = OrderDto.toDto(order))
    }
}