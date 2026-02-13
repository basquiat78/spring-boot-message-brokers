package io.basquiat.api.order.model

import io.basquiat.nats.model.PlaceOrder

data class OrderCreateDto(
    val productId: Long,
    val quantity: Int,
) {
    fun toPlaceOrder(): PlaceOrder = PlaceOrder(productId = productId, quantity = quantity)
}