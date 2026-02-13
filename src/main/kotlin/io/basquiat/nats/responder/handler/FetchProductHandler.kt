package io.basquiat.nats.responder.handler

import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.product.repository.ProductRepository
import io.basquiat.global.extensions.findByIdOrThrow
import io.basquiat.nats.model.FetchProduct
import io.basquiat.nats.model.FetchProductResponse
import org.springframework.stereotype.Service

@Service
class FetchProductHandler(
    private val productRepository: ProductRepository,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId
        val product =
            productRepository
                .findByIdOrThrow(productId, "해당 보물을 찾을 수 없습니다. 보물 아이디: $productId")
        return FetchProductResponse(product = ProductDto.toDto(product))
    }
}