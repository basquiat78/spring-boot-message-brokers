package io.basquiat.api.product.usecase

import io.basquiat.api.product.model.ProductDto
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.unableToJoin
import io.basquiat.nats.model.FetchProduct
import io.basquiat.nats.model.FetchProductResponse
import io.basquiat.nats.requester.AbstractRequester
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import org.springframework.stereotype.Service

@Service
class FetchProductUsecase(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<FetchProduct, FetchProductResponse>(
        natsConnection,
        props,
        ApiSubject.PRODUCT_FETCH,
        FetchProductResponse::class.java,
    ) {
    fun execute(id: Long): ProductDto {
        val response: FetchProductResponse = sendRequest(FetchProduct(productId = id))
        return response.product ?: unableToJoin(response.error)
    }
}