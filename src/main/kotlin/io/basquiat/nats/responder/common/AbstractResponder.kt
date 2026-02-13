package io.basquiat.nats.responder.common

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.logger
import io.basquiat.global.utils.objectToByte
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.*
import io.nats.client.api.ConsumerConfiguration
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import java.time.Duration

abstract class AbstractResponder<T>(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    @Qualifier("natsTaskExecutor")
    private val taskExecutor: TaskExecutor,
    private val apiSubject: ApiSubject, // 도메인별 설정 (durable, subject 등)
) {
    protected val log = logger<AbstractResponder<*>>()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        val jsm = natsConnection.jetStreamManagement()
        val streamName = props.apiStreamName
        val subject = apiSubject.subject
        val durable = apiSubject.durable

        setupConsumer(jsm, streamName, subject, durable)

        val js = natsConnection.jetStream()
        taskExecutor.execute { runPolling(js, streamName, durable, subject) }
        log.info("### Polling Start: $subject")
    }

    private fun setupConsumer(
        jsm: JetStreamManagement,
        streamName: String,
        subject: String,
        durable: String,
    ) {
        val consumerConfig =
            ConsumerConfiguration
                .builder()
                .durable(durable)
                .filterSubject(subject)
                .maxDeliver(props.maxDelivery)
                .ackWait(Duration.ofSeconds(30))
                .build()
        jsm.addOrUpdateConsumer(streamName, consumerConfig)
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
                    processMessage(msg)
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

    private fun processMessage(msg: Message) {
        val meta = msg.metaData()
        val deliveredCount = meta.deliveredCount()
        try {
            val replyTo = msg.headers?.getFirst(props.apiHeader)

            log.info("메시지 수신 [${msg.subject}] - ReplyTo: $replyTo")

            val response = handleRequest(msg)

            if (replyTo != null && response != null) {
                natsConnection.publish(replyTo, objectToByte(response))
            }
            msg.ack()
        } catch (e: Exception) {
            if (deliveredCount >= props.maxDelivery) {
                log.error("최대 재시도 횟수 초과 -> Seq: ${meta.streamSequence()}")
                msg.term()
            } else {
                val delaySeconds = (deliveredCount * 2L).coerceAtLeast(1L)
                val delay = Duration.ofSeconds(delaySeconds)
                msg.nakWithDelay(delay)
            }
        }
    }

    /**
     * 이 부분이 구현할 포인트
     */
    protected abstract fun handleRequest(msg: Message): T?

    @PreDestroy
    fun stop() {
        synchronized(subscriptions) {
            subscriptions.forEach {
                try {
                    it.unsubscribe()
                } catch (_: Exception) {
                    log.warn("구독 해제 실패")
                }
            }
            subscriptions.clear()
        }
    }
}