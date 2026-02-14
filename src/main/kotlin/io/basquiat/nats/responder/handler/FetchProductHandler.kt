package io.basquiat.nats.responder.handler

import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.product.service.ProductService
import io.basquiat.nats.model.FetchProduct
import io.basquiat.nats.model.FetchProductResponse
import org.springframework.stereotype.Service

@Service
class FetchProductHandler(
    private val productService: ProductService,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId
        val product =
            productService
                .findByIdOrThrow(productId, "해당 보물을 찾을 수 없습니다. 보물 아이디: $productId")
        return FetchProductResponse(product = ProductDto.toDto(product))
    }
}