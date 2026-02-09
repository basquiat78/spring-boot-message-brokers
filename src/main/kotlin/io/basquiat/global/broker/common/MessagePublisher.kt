package io.basquiat.global.broker.common

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.broker.rabbitmq.RabbitMqEventPublisher

interface MessagePublisher {
    fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    )
}

/**
 * RabbitMQ의 경우에는 delay 기능을 추가할 수 있다.
 * 따라서 기존 코드를 건드리지 않고 처리하기 위해서 코틀린 확장 함수를 이용한다.
 */
fun <T : Any> MessagePublisher.publish(
    channel: BrokerChannel,
    message: T,
    delayMillis: Long?,
) {
    when (this) {
        is RabbitMqEventPublisher -> this.publishWithDelay(channel, message, delayMillis)
        else -> this.publish(channel, message)
    }
}