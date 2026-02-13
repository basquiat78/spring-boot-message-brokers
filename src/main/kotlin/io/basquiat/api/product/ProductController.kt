package io.basquiat.api.product

import io.basquiat.api.product.usecase.FetchProductUsecase
import io.basquiat.api.product.usecase.ProductAllUsecase
import io.basquiat.global.model.vo.PaginationVo
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
@Tag(name = "NATS Product Controller")
class ProductController(
    private val productAllUsecase: ProductAllUsecase,
    private val getProductUsecase: FetchProductUsecase,
) {
    @GetMapping("")
    fun fetchProductAll(request: PaginationVo) = productAllUsecase.execute(request)

    @GetMapping("/{id}")
    fun fetchProduct(
        @PathVariable("id") id: Long,
    ) = getProductUsecase.execute(id)
}