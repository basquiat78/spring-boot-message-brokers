package io.basquiat.global.broker.nats

import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.global.utils.convertMessage
import io.basquiat.global.utils.logger
import io.nats.client.*
import io.nats.client.api.ConsumerConfiguration
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class NatsEventSubscriber(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<NatsEventSubscriber>()

    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) return

        val js = natsConnection.jetStream()
        val jsm = natsConnection.jetStreamManagement()

        handlers.forEach { handler ->
            setupPullSubscription(js, jsm, handler)
        }
    }

    private fun setupPullSubscription(
        js: JetStream,
        jsm: JetStreamManagement,
        handler: MessageHandler<*>,
    ) {
        val channel = handler.channel
        val subject = channel.channelName
        val targetType = channel.type
        val streamName = props.apiStreamName

        val queueGroupDurable = "${subject.replace(".", "_")}${props.durableSuffix}"

        try {
            val consumerConfig =
                ConsumerConfiguration
                    .builder()
                    .durable(queueGroupDurable)
                    .filterSubject(subject)
                    .maxDeliver(props.maxDelivery)
                    .ackWait(Duration.ofSeconds(30))
                    // .inactiveThreshold(Duration.ofHours(1))
                    .build()

            jsm.addOrUpdateConsumer(streamName, consumerConfig)
        } catch (e: Exception) {
            log.error("Consumer [$queueGroupDurable] setup failed: ${e.message}")
            return
        }

        taskExecutor.execute {
            log.info("Starting NATS Pull polling: [${handler::class.simpleName}] on Subject: [$subject]")

            val options = PullSubscribeOptions.bind(streamName, queueGroupDurable)
            val sub = js.subscribe(subject, options)

            synchronized(subscriptions) { subscriptions.add(sub) }

            while (!Thread.currentThread().isInterrupted) {
                try {
                    val messages = sub.fetch(100, Duration.ofSeconds(1))
                    for (msg in messages) {
                        processMessage(msg, handler, targetType, subject)
                    }
                } catch (e: Exception) {
                    if (e is InterruptedException || Thread.currentThread().isInterrupted) {
                        log.info("NATS Polling interrupted: $subject")
                        Thread.currentThread().interrupt()
                        break
                    }
                    log.error("NATS Pull error [$subject]: ${e.message}. Retrying in 2s...")
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processMessage(
        msg: Message,
        handler: MessageHandler<*>,
        targetType: Class<*>,
        subject: String,
    ) {
        try {
            val rawData = byteToObject(msg.data, Any::class.java)
            val finalMessage = convertMessage(rawData, targetType)

            (handler as MessageHandler<Any>).handle(finalMessage)
            msg.ack()
        } catch (e: Exception) {
            val deliveredCount = msg.metaData().deliveredCount()
            log.error("NATS 처리 실패 [$deliveredCount/${props.maxDelivery}] [Subject: $subject]: ${e.message}")

            if (deliveredCount >= props.maxDelivery) {
                log.error("Max delivery reached. Terminating message.")
                // TODO: DB 저장 또는 알림으로 모니터링
                msg.term()
            } else {
                val delaySeconds = (deliveredCount * 2L).coerceAtLeast(1L)
                val delay = Duration.ofSeconds(delaySeconds)
                msg.nakWithDelay(delay)
            }
        }
    }

    /**
     * for graceful shutdown
     */
    @PreDestroy
    fun shutdown() {
        log.info("NATS all Subscribers closed...")
        synchronized(subscriptions) {
            subscriptions.forEach { sub ->
                try {
                    sub.unsubscribe()
                } catch (e: Exception) {
                    log.warn("NATS 구독 해제 중 오류 발생: ${e.message}")
                }
            }
            subscriptions.clear()
        }
        log.info("successfully NATS all Subscribers complete closed!")
    }

//    /**
//     * push 방식
//     */
//    @PostConstruct
//    @Suppress("UNCHECKED_CAST")
//    fun init() {
//        if (handlers.isEmpty()) return
//        val js = natsConnection.jetStream()
//
//        val dispatcher =
//            natsConnection.createDispatcher { msg ->
//                log.info("successfully create dispatcher: $msg")
//            }
//
//        handlers.forEach { handler ->
//            val channel = handler.channel
//            val subject = channel.channelName
//            val targetType = channel.type
//
//            val natsMessageHandler =
//                NatsMessageHandler { msg: Message ->
//                    try {
//                        val rawData = byteToObject(msg.data, Any::class.java)
//                        val finalMessage = convertMessage(rawData, targetType)
//                        (handler as MessageHandler<Any>).handle(finalMessage)
//                        msg.ack()
//                    } catch (e: Exception) {
//                        // 재시도한 횟수 가져온다.
//                        val deliveredCount = msg.metaData().deliveredCount()
//
//                        log.error("NATS 처리 실패 [$deliveredCount/3] [Subject: $subject]: ${e.message}")
//                        if (deliveredCount >= props.maxDelivery) {
//                            log.error("최대 재시도 횟수 초과. Subject: $subject")
//                            // TODO: DB에 남기거나 알림을 줘서 모니터링 할수 있도록 한다.
//                            // term()을 호출해서 해당 메시지는 재전송 그만하고 끝내도록 알려준다.
//                            msg.term()
//                        } else {
//                            // 아직 횟수에 도달하지 않았다면 다시 보내달라고 요청
//                            msg.nak()
//                        }
//                    }
//                }
//
//            val instanceId = UUID.randomUUID().toString().substring(0, 8)
//            val uniqueDurable = "${subject.replace(".", "_")}_$instanceId"
//
//            val consumerConfiguration =
//                ConsumerConfiguration
//                    .builder()
//                    .maxDeliver(props.maxDelivery)
//                    .ackWait(Duration.ofSeconds(30))
//                    .inactiveThreshold(Duration.ofHours(1))
//                    .build()
//
//            val options =
//                PushSubscribeOptions
//                    .builder()
//                    .durable(uniqueDurable)
//                    .configuration(consumerConfiguration)
//                    .build()
//
//            js.subscribe(
//                subject,
//                dispatcher,
//                natsMessageHandler,
//                false,
//                options,
//            )
//            log.info("successfully NATS Fan-out subscriber: $subject (Durable: $uniqueDurable)")
//        }
//    }
}