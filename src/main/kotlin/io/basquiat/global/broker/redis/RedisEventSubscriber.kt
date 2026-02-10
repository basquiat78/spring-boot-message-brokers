package io.basquiat.global.broker.redis

import io.basquiat.global.annotations.ConditionalOnRedis
import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.utils.convertMessage
import io.basquiat.global.utils.logger
import jakarta.annotation.PostConstruct
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Component
@ConditionalOnRedis
class RedisEventSubscriber(
    private val redissonClient: RedissonClient,
    private val handlers: List<MessageHandler<*>>,
) {
    private val log = logger<RedisEventSubscriber>()

    /**
     * 해당 코드를 수정하지 않고 RedisMessageHandler를 구현한 핸들러를 추가하면 자동으로 구독할 수 있도록 한다.
     */
    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) {
            log.warn("등록된 핸들러가 없습니다. redis 리스너를 생성하지 않습니다.")
            return
        }
        handlers.forEach { handler ->
            subscriptionSetup(handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> subscriptionSetup(handler: MessageHandler<T>) {
        val channel = handler.channel
        val topic = redissonClient.getTopic(channel.channelName)
        val messageType = channel.type as Class<T>

        topic.addListener(Any::class.java) { _, message ->
            try {
                val finalMessage = convertMessage(message, messageType)
                handler.handle(finalMessage)
            } catch (e: Exception) {
                log.error("메시지 변환 또는 핸들러 실행 중 오류 발생: ${e.message}", e)
            }
        }
        log.info("Successfully Redis subscribed: [${handler::class.simpleName}] -> Queue:[${channel.channelName}]")
    }
}