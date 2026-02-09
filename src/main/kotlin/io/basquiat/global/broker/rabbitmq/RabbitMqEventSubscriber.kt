@file:Suppress("DEPRECATION")

package io.basquiat.global.broker.rabbitmq

import com.rabbitmq.client.Channel
import io.basquiat.global.annotations.ConditionalOnAmqp
import io.basquiat.global.broker.common.MessageHandler
import io.basquiat.global.broker.rabbitmq.factory.RabbitMqListenerContainerFactory
import io.basquiat.global.utils.logger
import jakarta.annotation.PostConstruct
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.stereotype.Component

@ConditionalOnAmqp
@Component
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
            val adapter = createMessageListenerAdapter(handler)
            containerFactory.createContainer(queueName, adapter).start()
            log.info("Successfully subscribed: [${handler::class.simpleName}] -> Queue:[$queueName]")
        }
    }

    private fun createMessageListenerAdapter(handler: MessageHandler<*>): MessageListenerAdapter {
        return object : MessageListenerAdapter() {
            override fun onMessage(
                message: Message,
                channel: Channel?,
            ) {
                try {
                    // MessageListenerAdapter에 넘겨주는 메시지 컨버터를 사용하게 된다.
                    // 이것을 RabbitMQ에서 빈으로 등록한 컨버터를 사용하도록 레이블 사용
                    val data = this@RabbitMqEventSubscriber.messageConverter.fromMessage(message)
                    if (data == null) {
                        log.warn("Converted data is null for message: $message")
                        return
                    }
                    executeHandler(handler, data)
                } catch (e: Exception) {
                    log.error("Message handling failed: ${e.message}")
                    throw e
                }
            }
        }.apply {
            @Suppress("UsePropertyAccessSyntax")
            setMessageConverter(messageConverter)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeHandler(
        handler: MessageHandler<*>,
        data: Any,
    ) {
        log.info("Processing Message from [${handler.channel.channelName}]")
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
        log.error("[DEAD LETTER] retry failed. Queue: ${handler.channel.channelName} / message: $data")
    }
}