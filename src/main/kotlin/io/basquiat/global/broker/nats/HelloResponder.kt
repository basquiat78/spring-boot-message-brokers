package io.basquiat.global.broker.nats

import io.basquiat.global.utils.logger
import io.nats.client.Connection

// @Component
class HelloResponder(
    private val natsConnection: Connection,
) {
    private val log = logger<HelloResponder>()

    // @PostConstruct
    fun replyTo() {
        val dispatcher =
            natsConnection.createDispatcher { msg ->
                log.info("msg info: $msg")
                log.info("msg data: ${msg.data}")
                log.info("msg replyTo: ${msg.replyTo}")
                // 안녕이라는 인사에 대답하는 초 간단 응답
                val answer = "How Are You?"
                natsConnection.publish(msg.replyTo, answer.toByteArray())
                log.info("complete response")
            }
        dispatcher.subscribe("nats.bow.hello", "nats-api-service-group")
        log.info("waiting response...")
    }
}