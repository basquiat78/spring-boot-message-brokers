package io.basquiat.global.broker.kafka

import io.basquiat.global.broker.common.MessagePublisher
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.utils.logger
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service("kafkaEventPublisher")
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MessagePublisher {
    private val log = logger<KafkaEventPublisher>()

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        kafkaTemplate
            .send(channel.channelName, message)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                log.info("[Kafka] 전송 성공: Topic=${metadata.topic()}, Offset=${metadata.offset()}")
            }.exceptionally { ex ->
                log.error("[Kafka] 전송 실패: ${ex.message}")
                null
            }
    }
}