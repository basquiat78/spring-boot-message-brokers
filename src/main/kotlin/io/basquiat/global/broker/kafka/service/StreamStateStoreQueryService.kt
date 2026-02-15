package io.basquiat.global.broker.kafka.service

import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import io.basquiat.global.utils.logger
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.errors.InvalidStateStoreException
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.springframework.kafka.config.StreamsBuilderFactoryBean
import org.springframework.stereotype.Service
import java.time.*

@Service
class StreamStateStoreQueryService(
    private val factoryBean: StreamsBuilderFactoryBean,
) {
    private val log = logger<StreamStateStoreQueryService>()

    private val defaultCount: Long = 0L

    /**
     *  상품의 실시간 조회수 가져오기
     */
    fun getProductViewCount(productId: String): Long {
        val kafkaStreams = factoryBean.kafkaStreams ?: return defaultCount

        // 어떤 이유로 State Store가 RUNNING이 아닐 때는 서비스가 중단되는 것을 막기 위해 기본값을 던지도록 한다.
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            log.warn("Kafka Streams State : ${kafkaStreams.state()}")
            return defaultCount
        }

        return try {
            val store =
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        QueryableStoreTypes.keyValueStore<String, Long>(),
                    ),
                )
            store.get(productId) ?: defaultCount
        } catch (e: InvalidStateStoreException) {
            // 리밸런싱 중이거나 상태가 변했을 때 잡아서 로그만 남기고 기본 값을 던지도록 한다.
            log.error("Kafka Streams Status is REBALANCING or Change: ${e.message}")
            defaultCount
        }
    }

    fun getTodayProductViewCount(productId: String): Long {
        val kafkaStreams = factoryBean.kafkaStreams ?: return defaultCount
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) return defaultCount

        return try {
            // 1. WindowStore 타입으로 가져오기
            val store =
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        QueryableStoreTypes.windowStore<String, Long>(),
                    ),
                )

            val now = Instant.now()
            val startOfDay =
                LocalDateTime
                    .of(LocalDate.now(), LocalTime.MIN)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

            val iterator = store.backwardFetch(productId, startOfDay, now)

            if (iterator.hasNext()) {
                iterator.next().value
            } else {
                defaultCount
            }
        } catch (e: Exception) {
            defaultCount
        }
    }
}