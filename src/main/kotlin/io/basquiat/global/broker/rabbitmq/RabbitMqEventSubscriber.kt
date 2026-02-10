@file:Suppress("DEPRECATION")

package io.basquiat.global.broker.rabbitmq

import io.basquiat.global.annotations.ConditionalOnAmqp
import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.broker.rabbitmq.factory.RabbitMqListenerContainerFactory
import io.basquiat.global.utils.logger
import jakarta.annotation.PostConstruct
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.stereotype.Component

@Component
@ConditionalOnAmqp
class RabbitMqEventSubscriber(
    private val containerFactory: RabbitMqListenerContainerFactory,
    private val handlers: List<MessageHandler<*>>,
    private val messageConverter: Jackson2JsonMessageConverter,
) {
    private val log = logger<RabbitMqEventSubscriber>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) return

        handlers.forEach { handler ->
            val queueName = handler.channel.channelName
            val listener = createMessageListener(handler)
            containerFactory.createContainer(queueName, listener).start()
            log.info("Successfully RabbitMQ subscribed: [${handler::class.simpleName}] -> Queue:[$queueName]")
        }
    }

    private fun createMessageListener(handler: MessageHandler<*>): ChannelAwareMessageListener {
        return ChannelAwareMessageListener { message, _ ->
            try {
                val data = messageConverter.fromMessage(message)
                if (data == null) {
                    log.warn("Converted data is null for message: $message")
                    return@ChannelAwareMessageListener
                }
                executeHandler(handler, data)
            } catch (e: Exception) {
                log.error("RabbitMQ 리스너 처리 중 에러 발생: ${e.message}", e)
                throw e
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeHandler(
        handler: MessageHandler<*>,
        data: Any,
    ) {
        try {
            (handler as MessageHandler<Any>).handle(data)
        } catch (e: Exception) {
            handleFailureLog(handler, data)
            throw e
        }
    }

    private fun handleFailureLog(
        handler: MessageHandler<*>,
        data: Any,
    ) {
        log.error("[DEAD LETTER] 처리 실패. Queue: ${handler.channel.channelName} / message: $data")
    }
}