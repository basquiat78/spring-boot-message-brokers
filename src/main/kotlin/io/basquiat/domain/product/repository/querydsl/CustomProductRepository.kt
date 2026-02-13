package io.basquiat.domain.product.repository.querydsl

import io.basquiat.domain.product.entity.Product

interface CustomProductRepository {
    fun findAllByCursor(
        lastId: Long?,
        limit: Long,
    ): List<Product>
}