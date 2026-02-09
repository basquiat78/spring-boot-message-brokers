package io.basquiat.global.broker.rabbitmq

import io.basquiat.global.annotations.ConditionalOnAmqp
import io.basquiat.global.broker.common.MessagePublisher
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.properties.RabbitMqProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@ConditionalOnAmqp
@Service("rabbitEventPublisher")
class RabbitMqEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val props: RabbitMqProperties,
) : MessagePublisher {
    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // 내부적으로 지연 시간 null을 넘겨서 공통 로직 처리
        this.publishWithDelay(channel, message, null)
    }

    fun <T : Any> publishWithDelay(
        channel: BrokerChannel,
        message: T,
        delayMillis: Long?,
    ) {
        val routingKey = channel.channelName
        if (delayMillis != null && delayMillis > 0) {
            // 전용 지연 큐 이름으로 전송 (ex: alarm.to.bot-delay)
            val targetDelayQueue = "$routingKey-delay"
            @Suppress("UsePropertyAccessSyntax")
            rabbitTemplate.convertAndSend("", targetDelayQueue, message) { msg ->
                msg.messageProperties.setExpiration(delayMillis.toString())
                msg
            }
        } else {
            rabbitTemplate.convertAndSend(props.exchangeName, routingKey, message)
        }
    }
}