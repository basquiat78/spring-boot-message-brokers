package io.basquiat.global.broker.nats

import io.basquiat.global.broker.common.MessagePublisher
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.logger
import io.basquiat.global.utils.objectToByte
import io.nats.client.Connection
import io.nats.client.JetStream
import io.nats.client.PublishOptions
import org.springframework.stereotype.Service
import java.util.*

@Service("natsEventPublisher")
class NatsEventPublisher(
    private val natsConnection: Connection,
    private val props: NatsProperties,
) : MessagePublisher {
    private val log = logger<NatsEventPublisher>()

    private val js: JetStream by lazy { natsConnection.jetStream() }

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // 내부적으로 지연 시간 null을 넘겨서 공통 로직 처리
        this.publishWithTtl(channel, message, null)
    }

    fun <T : Any> publishWithTtl(
        channel: BrokerChannel,
        message: T,
        millis: Long?,
    ) {
        val jsonBytes = objectToByte(message)
        val subject = channel.channelName

        val pubOptionsBuilder =
            PublishOptions
                .builder()
                .expectedStream(props.streamName)
                .messageId(UUID.randomUUID().toString().substring(0, 8))

        if (millis != null) pubOptionsBuilder.messageTtlSeconds(millis.toInt())

        val pubOptions = pubOptionsBuilder.build()

        js
            .publishAsync(subject, jsonBytes, pubOptions)
            .thenAccept { ack ->
                if (ack.isDuplicate) log.info("[NATS 발행 완료] 채널: $subject, Stream: ${ack.stream}, Seq: ${ack.seqno}")
            }.exceptionally { ex ->
                log.error("[NATS 발행 실패] 채널: $subject, 사유: ${ex.message}", ex)
                null
            }
    }
}