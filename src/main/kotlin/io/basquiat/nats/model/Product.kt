package io.basquiat.nats.model

import io.basquiat.api.product.model.ProductDto

/**
 * 단일 상품 정보 요청
 */
data class FetchProduct(
    val productId: Long,
)

data class FetchProductResponse(
    val product: ProductDto? = null,
    val error: String? = null,
)