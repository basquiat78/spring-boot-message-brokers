package io.basquiat.global.broker.common.router

import io.basquiat.global.broker.common.MessagePublisher
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.common.code.BrokerType
import io.basquiat.global.broker.common.publish
import io.basquiat.global.utils.notSupportBrokers
import org.springframework.stereotype.Component

@Component
class MessageRouter(
    private val publishers: Map<String, MessagePublisher>,
) {
    fun <T : Any> send(
        channel: BrokerChannel,
        type: BrokerType,
        message: T,
        delayMillis: Long? = null,
    ) {
        val publisher =
            when (type) {
                BrokerType.RABBITMQ -> publishers["rabbitEventPublisher"]
                BrokerType.REDIS -> publishers["redisEventPublisher"]
                BrokerType.KAFKA -> publishers["kafkaEventPublisher"]
                else -> null
            } ?: notSupportBrokers("지원하지 않는 브로커 타입입니다: $type")

        publisher.publish(channel, message, delayMillis)
    }
}