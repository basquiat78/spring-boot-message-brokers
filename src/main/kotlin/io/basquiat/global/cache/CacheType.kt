package io.basquiat.global.cache

import io.basquiat.api.order.model.OrderDto
import io.basquiat.api.product.model.ProductDto
import io.basquiat.api.reservation.model.ReservationDto
import java.time.Duration

enum class CacheType(
    val cacheName: String,
    val ttl: Duration,
    val clazz: Class<out Any> = Any::class.java,
) {
    DEFAULT("default", Duration.ofMinutes(10)),
    PRODUCT("product", Duration.ofHours(1), ProductDto::class.java),
    RESERVATION("reservation", Duration.ofHours(1), ReservationDto::class.java),
    ORDER("order", Duration.ofHours(1), OrderDto::class.java),
}