package io.basquiat.api.order.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.orders.code.OrderStatus
import io.basquiat.domain.orders.entity.Order
import io.basquiat.global.type.LongIdentifiable
import java.time.LocalDateTime

class OrderDto(
    override val id: Long,
    val product: ProductDto,
    val quantity: Int,
    var status: OrderStatus,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val createdAt: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val updatedAt: LocalDateTime,
) : LongIdentifiable {
    companion object {
        /**
         * entity to dto convert
         * @param entity
         * @return OrderDto
         */
        fun toDto(entity: Order) =
            with(entity) {
                OrderDto(
                    id = id!!,
                    product = ProductDto.toDto(product),
                    quantity = quantity,
                    status = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            }
    }
}