package io.basquiat.nats.responder.handler

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
                .findByIdForCache(productId)
        return FetchProductResponse(product = product)
    }
}