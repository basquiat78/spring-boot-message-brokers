package io.basquiat.global.broker.nats

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.logger
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.*
import io.nats.client.api.ConsumerConfiguration
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class HelloResponderWithJetStream(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<HelloResponderWithJetStream>()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        val jsm = natsConnection.jetStreamManagement()
        val streamName = props.apiStreamName
        val durable = ApiSubject.HELLO.durable
        val subject = ApiSubject.HELLO.subject

        val js = natsConnection.jetStream()
        val consumerConfig =
            ConsumerConfiguration
                .builder()
                .durable(durable)
                .filterSubject(subject)
                .maxDeliver(props.maxDelivery)
                .ackWait(Duration.ofSeconds(30))
                .build()

        jsm.addOrUpdateConsumer(streamName, consumerConfig)

        taskExecutor.execute {
            runPolling(js, streamName, durable, subject)
        }
    }

    private fun runPolling(
        js: JetStream,
        streamName: String,
        durable: String,
        subject: String,
    ) {
        val options = PullSubscribeOptions.bind(streamName, durable)
        val sub = js.subscribe(subject, options)
        synchronized(subscriptions) { subscriptions.add(sub) }

        while (!Thread.currentThread().isInterrupted) {
            try {
                val messages = sub.fetch(10, Duration.ofMillis(500))
                for (msg in messages) {
                    handleMessage(msg)
                }
            } catch (_: InterruptedException) {
                log.info("[$subject] 폴링 스레드가 종료 신호를 받아 정지합니다.")
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                if (Thread.currentThread().isInterrupted) break
                log.error("Polling Error [$subject]: ${e.message}")
                try {
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun handleMessage(msg: Message) {
        try {
            // 헤더에서 응답할 replyTo를 가져온다.
            val replyTo = msg.headers?.getFirst(props.apiHeader)
            log.info("received Data: ${String(msg.data)}, replyTo: $replyTo")

            if (replyTo != null) {
                val answer = "How Are You?"
                natsConnection.publish(replyTo, answer.toByteArray())
                log.info("successfully send to: $replyTo")
            } else {
                log.warn("헤더에 ${props.apiHeader} 정보가 없습니다.")
            }
            msg.ack()
        } catch (e: Exception) {
            log.error("error occurred: ${e.message}")
        }
    }

    @PreDestroy
    fun stop() {
        synchronized(subscriptions) {
            subscriptions.forEach {
                try {
                    it.unsubscribe()
                } catch (_: Exception) {
                    // pass
                }
            }
            subscriptions.clear()
        }
    }
}