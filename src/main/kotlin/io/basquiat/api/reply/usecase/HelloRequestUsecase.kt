package io.basquiat.api.reply.usecase

import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.natsTimeout
import io.basquiat.nats.subject.ApiSubject
import io.nats.client.Connection
import io.nats.client.PublishOptions
import io.nats.client.impl.Headers
import io.nats.client.impl.NatsMessage
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class HelloRequestUsecase(
    private val natsConnection: Connection,
    private val props: NatsProperties,
) {
    fun execute(): String {
        val subject = ApiSubject.HELLO.subject
        val js = natsConnection.jetStream()

        // inbox를 생성
        val replyTo = natsConnection.createInbox()
        // 응답을 받은 임시 구독 생성
        val replySubscription = natsConnection.subscribe(replyTo)

        return try {
            // 헤더를 생성한다.
            val headers = Headers()
            headers.add(props.apiHeader, replyTo)

            val msg =
                NatsMessage
                    .builder()
                    .subject(subject)
                    .headers(headers)
                    .data("hello".toByteArray())
                    .build()

            js.publish(msg, PublishOptions.builder().expectedStream(props.apiStreamName).build())
            val replyMsg = replySubscription.nextMessage(Duration.ofSeconds(10))
            replyMsg?.let { String(it.data) } ?: natsTimeout("NATS 응답자가 응답하지 않습니다. (Subject: $subject)")
        } finally {
            // 최종적으로 임시로 만든 구독 정보 삭제
            replySubscription.unsubscribe()
        }
    }
}