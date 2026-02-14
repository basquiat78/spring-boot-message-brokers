package io.basquiat.nats.responder.handler

import io.basquiat.domain.orders.code.OrderStatus
import io.basquiat.domain.orders.entity.Order
import io.basquiat.domain.orders.repository.OrderRepository
import io.basquiat.domain.product.repository.ProductRepository
import io.basquiat.global.annotations.DistributedLock
import io.basquiat.global.utils.unableToJoin
import io.basquiat.nats.model.PlaceOrder
import io.basquiat.nats.model.PlaceOrderResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlaceOrderHandler(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    @DistributedLock(key = "#request.productId", waitTime = 10L, leaseTime = 3L, useWatchdog = true)
    fun execute(request: PlaceOrder): PlaceOrderResponse {
        val (productId, quantity) = request

        val product =
            productRepository
                .findById(productId)
                .orElseThrow { NoSuchElementException("해당 보물을 찾을 수 없습니다.") }
        if (product.quantity < quantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        product.quantity -= quantity
        val entity =
            Order(
                product = product,
                quantity = quantity,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderRepository.save(entity)
        return PlaceOrderResponse(orderId = completeOrder.id)
    }
}