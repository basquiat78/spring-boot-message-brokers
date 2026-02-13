package io.basquiat.api.product.usecase

import io.basquiat.api.product.model.ProductDto
import io.basquiat.domain.product.service.ProductService
import io.basquiat.global.model.response.Pagination
import io.basquiat.global.model.vo.PaginationVo
import io.basquiat.global.utils.make
import org.springframework.stereotype.Service

@Service
class ProductAllUsecase(
    private val productService: ProductService,
) {
    fun execute(request: PaginationVo): Pair<List<ProductDto>, Pagination?> {
        val products = productService.findAll(request.lastId, request.limit()).map(ProductDto::toDto)
        if (products.isEmpty()) {
            return emptyList<ProductDto>() to null
        }

        val size = request.size()
        return make(products, size)
    }
}