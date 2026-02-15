package io.basquiat.global.broker.kafka

import io.basquiat.global.annotations.ConditionalOnKafka
import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import io.basquiat.global.utils.logger
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@ConditionalOnKafka
@Service("kafkaStreamPublisher")
class KafkaStreamPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val log = logger<KafkaStreamPublisher>()

    fun <T : Any> publish(
        kafkaStreamTopic: KafkaStreamTopic,
        key: String,
        message: T,
    ) {
        kafkaTemplate
            .send(kafkaStreamTopic.topic, key, message)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                log.info("[Kafka] 전송 성공: Topic=${metadata.topic()}, Offset=${metadata.offset()}")
            }.exceptionally { ex ->
                log.error("[Kafka] 전송 실패: ${ex.message}")
                null
            }
    }
}