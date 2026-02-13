package io.basquiat.api.order

import io.basquiat.api.order.model.OrderCreateDto
import io.basquiat.api.order.model.OrderDto
import io.basquiat.api.order.usecase.FetchOrderUsecase
import io.basquiat.api.order.usecase.PlaceOrderUsecase
import io.basquiat.nats.model.PlaceOrderResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
@Tag(name = "NATS Orders request Controller")
class OrderController(
    private val placeOrderUsecase: PlaceOrderUsecase,
    private val fetchOrderUsecase: FetchOrderUsecase,
) {
    @PostMapping("")
    fun placeOrder(
        @RequestBody request: OrderCreateDto,
    ): PlaceOrderResponse = placeOrderUsecase.execute(request)

    @GetMapping("/{id}")
    fun fetchOrder(
        @PathVariable("id") id: Long,
    ): OrderDto = fetchOrderUsecase.execute(id)
}