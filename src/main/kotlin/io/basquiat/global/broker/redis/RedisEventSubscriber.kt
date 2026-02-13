package io.basquiat.global.broker.redis

import io.basquiat.global.annotations.ConditionalOnRedis
import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.utils.convertMessage
import io.basquiat.global.utils.logger
import jakarta.annotation.PostConstruct
import org.redisson.api.RedissonClient
import org.redisson.api.stream.StreamCreateGroupArgs
import org.redisson.api.stream.StreamMessageId
import org.redisson.api.stream.StreamReadGroupArgs
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID.randomUUID

@Component
@ConditionalOnRedis
class RedisEventSubscriber(
    private val redissonClient: RedissonClient,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
    @Value($$"${redisson.group-name:consumer-group}")
    private val groupName: String,
) {
    private val log = logger<RedisEventSubscriber>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) {
            log.warn("등록된 핸들러가 없습니다. redis 리스너를 생성하지 않습니다.")
            return
        }
        handlers.forEach { handler ->
            setupStreamSubscription(handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setupStreamSubscription(handler: MessageHandler<T>) {
        val streamName = handler.channel.channelName
        val targetType = handler.channel.type
        val stream = redissonClient.getStream<String, T>(streamName)

        try {
            val streamCreateGroupArgs = StreamCreateGroupArgs.name(groupName).id(StreamMessageId.ALL).makeStream()
            stream.createGroup(streamCreateGroupArgs)
        } catch (e: Exception) {
            log.error("Consumer group [$groupName] already exists for stream [$streamName] / ${e.message}")
        }

        taskExecutor.execute {
            log.info("Starting Stream polling: [${handler::class.simpleName}] on Group:[$groupName]")
            while (!Thread.currentThread().isInterrupted) {
                try {
                    // message를 가져올 때 읽지 않은 메세지를 가져오도록 처리한다.
                    val messages =
                        stream.readGroup(
                            groupName,
                            "consumer-${randomUUID()}",
                            StreamReadGroupArgs.neverDelivered().count(1).timeout(Duration.ofSeconds(5)),
                        )

                    messages.forEach { (id, content) ->
                        try {
                            val message = content["payload"] as Any
                            val data = convertMessage(message, targetType) as T
                            handler.handle(data)
                            stream.ack(groupName, id)
                        } catch (e: Exception) {
                            log.error("Handler execution failed for message $id: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    if (e is InterruptedException || Thread.currentThread().isInterrupted) {
                        log.info("Redis Polling interrupted, stopping gracefully...")
                        Thread.currentThread().interrupt()
                        break
                    }

                    log.error("Stream polling error: ${e.message}. Retrying in 2s...")
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }
        log.info("Successfully registered Redis Stream Consumer: [${handler::class.simpleName}]")
    }

// 처음 시도했던 방식
//    @Suppress("UNCHECKED_CAST")
//    private fun <T> subscriptionSetup(handler: MessageHandler<T>) {
//        val channel = handler.channel
//        val topic = redissonClient.getTopic(channel.channelName)
//        val messageType = channel.type as Class<T>
//
//        topic.addListener(Any::class.java) { _, message ->
//            try {
//                val finalMessage = convertMessage(message, messageType)
//                handler.handle(finalMessage)
//            } catch (e: Exception) {
//                log.error("메시지 변환 또는 핸들러 실행 중 오류 발생: ${e.message}", e)
//            }
//        }
//        log.info("Redis 채널 자동 구독 완료: ${channel.channelName}")
//    }
}