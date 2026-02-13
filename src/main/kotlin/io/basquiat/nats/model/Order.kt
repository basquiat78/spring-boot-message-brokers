package io.basquiat.nats.model

import io.basquiat.api.order.model.OrderDto

/**
 * 주문 요청  객체
 */
data class PlaceOrder(
    val productId: Long,
    val quantity: Int,
)

/**
 * 주문 요청에 대한 응답 객체
 */
data class PlaceOrderResponse(
    val orderId: Long? = null,
    val error: String? = null,
)

/**
 * 주문 요청  객체
 */
data class FetchOrder(
    val orderId: Long,
)

/**
 * 주문 요청에 대한 응답 객체
 */
data class FetchOrderResponse(
    val order: OrderDto? = null,
    val error: String? = null,
)