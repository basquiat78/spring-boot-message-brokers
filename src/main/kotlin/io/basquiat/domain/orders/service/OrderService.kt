package io.basquiat.domain.orders.service

import io.basquiat.domain.orders.entity.Order
import io.basquiat.domain.orders.repository.OrderRepository
import io.basquiat.global.extensions.findByIdOrThrow
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderService(
    private val repository: OrderRepository,
) {
    fun create(entity: Order): Order = repository.save(entity)

    @Transactional(readOnly = true)
    fun findByIdOrThrow(id: Long): Order = repository.findByIdOrThrow(id)

    @Transactional(readOnly = true)
    fun findByIdOrNull(id: Long): Order? = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    fun findOrdersWithProduct(id: Long): Order? = repository.findWithProductById(id)
}