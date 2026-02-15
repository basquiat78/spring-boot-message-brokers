package io.basquiat.global.broker.kafka

import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header

// @Component
// @ConditionalOnKafka
class KafkaEventSubscriber(
    private val handlers: List<MessageHandler<*>>,
) {
    private val log = logger<KafkaEventSubscriber>()

    private val handlerMap: Map<String, MessageHandler<*>> by lazy {
        handlers.associateBy { it.channel.channelName }
    }

    companion object {
        private val TOPIC_SUFFIX_REGEX = Regex("-retry-\\d+|-dlt$")
    }

    @RetryableTopic(
        attempts = "3",
        numPartitions = "3",
        backOff = BackOff(delay = 2000, multiplier = 2.0),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlt",
        dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
    )
    @KafkaListener(
        // SpEL을 사용하여 BrokerChannel Enum에 정의된 모든 channelName을 구독 리스트로 확보
        topics = ["#{T(io.basquiat.global.broker.common.code.BrokerChannel).values().![channelName]}"],
        groupId = $$"${spring.kafka.consumer.group-id:basquiat-group}",
    )
    @Suppress("UNCHECKED_CAST")
    fun onMessage(
        record: ConsumerRecord<String, Any>,
        ack: Acknowledgment,
    ) {
        val payload = record.value()
        val rawTopic = record.topic()

        val originalTopic = rawTopic.replace(TOPIC_SUFFIX_REGEX, "")

        handlerMap[originalTopic]?.let { handler ->
            try {
                (handler as MessageHandler<Any>).handle(payload)
                ack.acknowledge()
            } catch (e: Exception) {
                log.error("[Kafka] 핸들러 실패 [Topic: $rawTopic]: ${e.message}")
                // DLT로 보내도록
                throw e
            }
        } ?: run {
            log.warn("[Kafka] 매칭되는 핸들러 없음: $rawTopic (Original: $originalTopic)")
            ack.acknowledge()
        }
    }

    @DltHandler
    fun handleDlt(
        record: ConsumerRecord<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) errorMessage: String?,
        ack: Acknowledgment,
    ) {
        log.error(
            """
            [DLT 인입] 최종 처리 실패
            - 원본 토픽: $topic
            - 오프셋: $offset
            - 에러 메시지: $errorMessage
            - 페이로드: ${record.value()}
            """.trimIndent(),
        )
        // TODO: 이후 어떻게 처리할 것인지
        // 1. 알림봇에 에러난 것을 푸시한다.
        // 2. 디비에 저장하고 차후 보상 로직 또는 스크립트로 처리할지 결정하자.
        ack.acknowledge()
    }
}