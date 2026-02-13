package io.basquiat.domain.orders.repository

import io.basquiat.domain.orders.entity.Order
import io.basquiat.global.repository.BaseRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderRepository : BaseRepository<Order, Long> {
    @Query(
        """
        SELECT orders From Order orders
        JOIN FETCH orders.product
        WHERE orders.id = :id
        """,
    )
    fun findWithProductById(
        @Param("id") id: Long,
    ): Order?
}