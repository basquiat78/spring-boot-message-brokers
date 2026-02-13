package io.basquiat.nats.requester

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.byteToObject
import io.basquiat.global.utils.logger
import io.basquiat.global.utils.natsTimeout
import io.basquiat.global.utils.objectToByte
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import java.time.Duration

abstract class AbstractRequester<R, T>(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val apiSubject: ApiSubject,
    private val responseType: Class<T>,
) {
    protected val log = logger<AbstractRequester<*, *>>()

    fun sendRequest(request: R): T {
        val subject = apiSubject.subject
        val js = natsConnection.jetStream()

        // 1. 임시 응답 통로(Inbox) 설정
        val replyTo = natsConnection.createInbox()
        val replySubscription = natsConnection.subscribe(replyTo)

        return try {
            val headers = Headers().apply { add(props.apiHeader, replyTo) }
            val payload = objectToByte(request)

            val msg =
                NatsMessage
                    .builder()
                    .subject(subject)
                    .headers(headers)
                    .data(payload)
                    .build()

            js.publish(msg, PublishOptions.builder().expectedStream(props.apiStreamName).build())
            log.info("NATS 요청 발행 완료: $subject, ReplyTo: $replyTo")

            val replyMsg =
                replySubscription.nextMessage(Duration.ofSeconds(10))
                    ?: natsTimeout("NATS 응답자가 응답하지 않습니다. (Subject: $subject)")

            byteToObject(replyMsg.data, responseType)
        } finally {
            replySubscription.unsubscribe()
        }
    }
}