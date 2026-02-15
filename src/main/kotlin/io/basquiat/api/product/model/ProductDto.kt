package io.basquiat.api.product.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.basquiat.domain.product.entity.Product
import io.basquiat.global.type.LongIdentifiable
import java.time.LocalDateTime

data class ProductDto(
    override val id: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val viewCount: Long? = null,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val createdAt: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val updatedAt: LocalDateTime,
) : LongIdentifiable {
    companion object {
        /**
         * entity to dto convert
         * @param entity
         * @return ProductDto
         */
        fun toDto(entity: Product) =
            with(entity) {
                ProductDto(
                    id = id!!,
                    name = name,
                    price = price,
                    quantity = quantity,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            }
    }

    fun withViewCount(viewCount: Long) = this.copy(viewCount = viewCount)
}