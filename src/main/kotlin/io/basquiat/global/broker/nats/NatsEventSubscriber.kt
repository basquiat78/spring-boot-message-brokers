package io.basquiat.global.broker.nats

import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.global.utils.convertMessage
import io.basquiat.global.utils.logger
import io.nats.client.Connection
import io.nats.client.Message
import io.nats.client.PushSubscribeOptions
import io.nats.client.api.ConsumerConfiguration
import jakarta.annotation.PostConstruct
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import io.nats.client.MessageHandler as NatsMessageHandler

@Component
class NatsEventSubscriber(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<NatsEventSubscriber>()

//    @PostConstruct
//    fun init() {
//        if (handlers.isEmpty()) return
//
//        val js = natsConnection.jetStream()
//        val jsm = natsConnection.jetStreamManagement()
//
//        handlers.forEach { handler ->
//            setupPullSubscription(js, jsm, handler)
//        }
//    }
//
//    private fun setupPullSubscription(
//        js: JetStream,
//        jsm: JetStreamManagement,
//        handler: MessageHandler<*>,
//    ) {
//        val channel = handler.channel
//        val subject = channel.channelName
//        val targetType = channel.type
//        val streamName = props.streamName
//
//        val instanceId = UUID.randomUUID().toString().substring(0, 8)
//        val uniqueDurable = "${subject.replace(".", "_")}_$instanceId"
//
//        try {
//            // 1. 컨슈머 설정 (Redis의 StreamCreateGroupArgs 역할)
//            val consumerConfig =
//                ConsumerConfiguration
//                    .builder()
//                    .durable(uniqueDurable)
//                    .maxDeliver(props.maxDelivery)
//                    .ackWait(Duration.ofSeconds(30))
//                    .build()
//
//            // [인터페이스 확인 완료] 있으면 업데이트, 없으면 생성
//            jsm.addOrUpdateConsumer(streamName, consumerConfig)
//            log.info("✅ Consumer setup completed: $uniqueDurable")
//        } catch (e: Exception) {
//            log.error("❌ Consumer [$uniqueDurable] setup failed: ${e.message}")
//            return // 설정 실패 시 해당 핸들러는 폴링을 시작하지 않음
//        }
//
//        // 2. Polling 시작 (Redis와 동일한 TaskExecutor 방식)
//        taskExecutor.execute {
//            log.info("🚀 Starting NATS Pull polling: [${handler::class.simpleName}] on Subject: [$subject]")
//
//            // 이미 생성된 컨슈머에 바인딩하여 구독
//            val options = PullSubscribeOptions.bind(streamName, uniqueDurable)
//            val sub = js.subscribe(subject, options)
//
//            while (!Thread.currentThread().isInterrupted) {
//                try {
//                    val messages = sub.fetch(1, Duration.ofSeconds(1))
//                    for (msg in messages) {
//                        processMessage(msg, handler, targetType, subject)
//                    }
//                } catch (e: Exception) {
//                    if (e is InterruptedException || Thread.currentThread().isInterrupted) {
//                        log.info("NATS Polling interrupted for $subject")
//                        return@execute
//                    }
//                    log.error("NATS Pull error [$subject]: ${e.message}. Retrying in 2s...")
//                    Thread.sleep(2000) // Backoff
//                }
//            }
//        }
//    }
//
//    @Suppress("UNCHECKED_CAST")
//    private fun processMessage(
//        msg: Message,
//        handler: MessageHandler<*>,
//        targetType: Class<*>,
//        subject: String,
//    ) {
//        try {
//            val rawData = byteToObject(msg.data, Any::class.java)
//            val finalMessage = convertMessage(rawData, targetType)
//
//            (handler as MessageHandler<Any>).handle(finalMessage)
//            msg.ack()
//        } catch (e: Exception) {
//            val deliveredCount = msg.metaData().deliveredCount()
//            log.error("NATS 처리 실패 [$deliveredCount/${props.maxDelivery}] [Subject: $subject]: ${e.message}")
//
//            if (deliveredCount >= props.maxDelivery) {
//                log.error("🔥 Max delivery reached. Terminating message.")
//                msg.term() // 더 이상 재전송 하지 않음
//            } else {
//                msg.nak() // 다시 보내달라고 요청
//            }
//        }
//    }

    /**
     * push 방식
     */
    @PostConstruct
    @Suppress("UNCHECKED_CAST")
    fun init() {
        if (handlers.isEmpty()) return
        val js = natsConnection.jetStream()

        val dispatcher =
            natsConnection.createDispatcher { msg ->
                log.info("successfully create dispatcher: $msg")
            }

        handlers.forEach { handler ->
            val channel = handler.channel
            val subject = channel.channelName
            val targetType = channel.type

            val natsMessageHandler =
                NatsMessageHandler { msg: Message ->
                    try {
                        val rawData = byteToObject(msg.data, Any::class.java)
                        val finalMessage = convertMessage(rawData, targetType)
                        (handler as MessageHandler<Any>).handle(finalMessage)
                        msg.ack()
                    } catch (e: Exception) {
                        // 재시도한 횟수 가져온다.
                        val deliveredCount = msg.metaData().deliveredCount()

                        log.error("NATS 처리 실패 [$deliveredCount/3] [Subject: $subject]: ${e.message}")
                        if (deliveredCount >= props.maxDelivery) {
                            log.error("최대 재시도 횟수 초과. Subject: $subject")
                            // TODO: DB에 남기거나 알림을 줘서 모니터링 할수 있도록 한다.
                            // term()을 호출해서 해당 메시지는 재전송 그만하고 끝내도록 알려준다.
                            msg.term()
                        } else {
                            // 아직 횟수에 도달하지 않았다면 다시 보내달라고 요청
                            msg.nak()
                        }
                    }
                }

            val instanceId = UUID.randomUUID().toString().substring(0, 8)
            val uniqueDurable = "${subject.replace(".", "_")}_$instanceId"

            val consumerConfiguration =
                ConsumerConfiguration
                    .builder()
                    .maxDeliver(props.maxDelivery)
                    .ackWait(Duration.ofSeconds(30))
                    .build()

            val options =
                PushSubscribeOptions
                    .builder()
                    .durable(uniqueDurable)
                    .configuration(consumerConfiguration)
                    .build()

            js.subscribe(
                subject,
                dispatcher,
                natsMessageHandler,
                false,
                options,
            )
            log.info("successfully NATS Fan-out subscriber: $subject (Durable: $uniqueDurable)")
        }
    }
}