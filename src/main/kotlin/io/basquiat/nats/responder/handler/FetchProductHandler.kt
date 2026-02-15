package io.basquiat.nats.responder.handler

import io.basquiat.domain.product.service.ProductService
import io.basquiat.global.broker.kafka.KafkaStreamPublisher
import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import io.basquiat.global.broker.kafka.service.StreamStateStoreQueryService
import io.basquiat.nats.model.FetchProduct
import io.basquiat.nats.model.FetchProductResponse
import org.springframework.stereotype.Service

@Service
class FetchProductHandler(
    private val productService: ProductService,
    private val kafkaStreamPublisher: KafkaStreamPublisher,
    private val streamStateStoreQueryService: StreamStateStoreQueryService,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId

        // 스트림즈로 publish하면 앞서 정의한 Processor가 데이터를 처리한다.
        kafkaStreamPublisher.publish(
            kafkaStreamTopic = KafkaStreamTopic.PRODUCT_FETCH,
            key = productId.toString(),
            message = "PRODUCT_VIEW_COUNT_EVENT",
        )

        val product =
            productService
                .findByIdForCache(productId)

        // 조회 이후 state store를 통해서 조회수를 가져온다.
        val viewCount = streamStateStoreQueryService.getTodayProductViewCount(productId.toString())
        return FetchProductResponse(product = product.withViewCount(viewCount))
    }
}