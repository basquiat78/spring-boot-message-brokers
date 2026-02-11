package io.basquiat.global.broker.redis

import io.basquiat.global.annotations.ConditionalOnRedis
import io.basquiat.global.broker.common.MessagePublisher
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.utils.logger
import io.basquiat.global.utils.notMatchMessageType
import org.redisson.api.RedissonClient
import org.redisson.api.stream.StreamAddArgs
import org.springframework.stereotype.Service

@ConditionalOnRedis
@Service("redisEventPublisher")
class RedisEventPublisher(
    private val redissonClient: RedissonClient,
) : MessagePublisher {
    private val log = logger<RedisEventPublisher>()

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // RedisChannel에 정의된 타입과 메시지 타입이 일치하는지 확인한다.
        if (!channel.type.isInstance(message)) notMatchMessageType("채널 ${channel.channelName}에 맞지 않는 메시지 타입입니다.")

        val stream = redissonClient.getStream<String, T>(channel.channelName)

        stream
            .addAsync(StreamAddArgs.entry("payload", message))
            .thenAccept { id ->
                log.info("Successfully Redis subscriber id: $id")
            }.exceptionally { e ->
                log.error("Failed to publish message to Redis Stream: ${e.message}")
                null
            }
    }

// 처음 시도한 방식
//    override fun <T : Any> publish(
//        channel: BrokerChannel,
//        message: T,
//    ) {
//        // RedisChannel에 정의된 타입과 메시지 타입이 일치하는지 확인한다.
//        if (!channel.type.isInstance(message)) notMatchMessageType("채널 ${channel.channelName}에 맞지 않는 메시지 타입입니다.")
//
//        val topic = redissonClient.getTopic(channel.channelName)
//        topic
//            .publishAsync(message)
//            .thenAccept { subscriber ->
//                log.info("subscriber: $subscriber")
//            }
//    }
}