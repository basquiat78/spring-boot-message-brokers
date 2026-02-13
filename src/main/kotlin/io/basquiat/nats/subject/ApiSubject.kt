package io.basquiat.nats.subject

enum class ApiSubject(
    val subject: String,
    val durable: String,
) {
    HELLO("basquiat.bow.hello", "hello-api-group"),

    PRODUCT_FETCH("basquiat.product.fetch", "product-fetch-durable"),

    ORDER_CREATE("basquiat.order.create", "order-create-durable"),
    ORDER_FETCH("basquiat.order.fetch", "order-fetch-durable"),

    RESERVATION_CREATE("basquiat.reservation.create", "reservation-create-durable"),
    RESERVATION_FETCH("basquiat.reservation.fetch", "reservation-fetch-durable"),
    RESERVATION_CONFIRM("basquiat.reservation.confirm", "reservation-confirm-durable"),
    RESERVATION_CANCEL("basquiat.reservation.cancel", "reservation-cancel-durable"),
}