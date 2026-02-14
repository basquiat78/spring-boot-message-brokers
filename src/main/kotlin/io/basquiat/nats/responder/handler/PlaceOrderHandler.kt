package io.basquiat.nats.responder.handler

import io.basquiat.domain.orders.code.OrderStatus
import io.basquiat.domain.orders.entity.Order
import io.basquiat.domain.orders.service.OrderService
import io.basquiat.domain.product.service.ProductService
import io.basquiat.global.utils.unableToJoin
import io.basquiat.nats.model.PlaceOrder
import io.basquiat.nats.model.PlaceOrderResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlaceOrderHandler(
    private val productService: ProductService,
    private val orderService: OrderService,
) {
    @Transactional
    fun execute(request: PlaceOrder): PlaceOrderResponse {
        val (productId, quantity) = request

        val product =
            productService
                .findByIdOrThrow(productId, "해당 보물을 찾을 수 없습니다. 보물 아이디: $productId")
        if (product.quantity < quantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        product.quantity -= quantity
        val entity =
            Order(
                product = product,
                quantity = quantity,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderService.create(entity)
        return PlaceOrderResponse(orderId = completeOrder.id)
    }
}