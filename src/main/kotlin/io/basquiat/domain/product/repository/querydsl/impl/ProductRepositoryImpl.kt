package io.basquiat.domain.product.repository.querydsl.impl

import com.querydsl.jpa.impl.JPAQueryFactory
import io.basquiat.domain.product.entity.Product
import io.basquiat.domain.product.entity.QProduct.product
import io.basquiat.domain.product.repository.querydsl.CustomProductRepository
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : CustomProductRepository {
    override fun findAllByCursor(
        lastId: Long?,
        limit: Long,
    ): List<Product> =
        queryFactory
            .selectFrom(product)
            .where(
                product.ltLastId(lastId),
            ).orderBy(product.id.desc())
            .limit(limit)
            .fetch()
}