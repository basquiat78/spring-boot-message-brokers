package io.basquiat.api.reservation.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.reservation.code.ReservationStatus
import io.basquiat.domain.reservation.entity.Reservation
import io.basquiat.global.type.LongIdentifiable
import java.time.LocalDateTime

data class ReservationDto(
    override val id: Long,
    val product: ProductDto,
    val reservedAt: LocalDateTime,
    var status: ReservationStatus,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val createdAt: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val updatedAt: LocalDateTime,
) : LongIdentifiable {
    companion object {
        /**
         * entity to dto convert
         * @param entity
         * @return ReservationDto
         */
        fun toDto(entity: Reservation) =
            with(entity) {
                ReservationDto(
                    id = id!!,
                    product = ProductDto.toDto(product),
                    reservedAt = reservedAt,
                    status = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            }
    }
}