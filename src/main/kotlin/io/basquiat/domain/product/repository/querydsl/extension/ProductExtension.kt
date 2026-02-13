package io.basquiat.domain.product.repository.querydsl.extension

import com.querydsl.core.annotations.QueryDelegate
import com.querydsl.core.annotations.QueryEntity
import com.querydsl.core.types.Predicate
import io.basquiat.domain.product.entity.Product
import io.basquiat.domain.product.entity.QProduct

@QueryEntity
class ProductExtension

@QueryDelegate(Product::class)
fun ltLastId(
    product: QProduct,
    lastId: Long?,
): Predicate? =
    lastId
        ?.takeIf { it > 0 }
        ?.let { product.id.lt(it) }