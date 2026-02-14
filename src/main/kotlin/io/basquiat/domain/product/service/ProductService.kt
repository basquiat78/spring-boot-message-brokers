package io.basquiat.domain.product.service

import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.product.entity.Product
import io.basquiat.domain.product.repository.ProductRepository
import io.basquiat.global.extensions.findByIdOrThrow
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val repository: ProductRepository,
) {
    fun create(entity: Product): Product = repository.save(entity)

    @Transactional(readOnly = true)
    fun findByIdOrThrow(
        id: Long,
        message: String? = null,
    ): Product = repository.findByIdOrThrow(id, message)

    @Cacheable(value = ["product"], key = "#productId")
    fun findByIdForCache(productId: Long): ProductDto {
        val product = repository.findByIdOrThrow(productId, "보물을 찾을 수 없습니다. 보물 아이디: $productId")
        return ProductDto.toDto(product)
    }

    @Transactional(readOnly = true)
    fun findByIdOrNull(id: Long): Product? = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    fun findAll(
        lastId: Long?,
        limit: Long,
    ): List<Product> = repository.findAllByCursor(lastId, limit)
}