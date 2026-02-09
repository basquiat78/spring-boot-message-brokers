package io.basquiat.global.broker.rabbitmq.factory

import io.basquiat.global.annotations.ConditionalOnAmqp
import org.aopalliance.intercept.MethodInterceptor
import org.springframework.amqp.core.MessageListener
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer
import org.springframework.stereotype.Component

@Component
@ConditionalOnAmqp
class RabbitMqListenerContainerFactory(
    private val connectionFactory: ConnectionFactory,
    private val retryInterceptor: MethodInterceptor,
) {
    /**
     * 리스너 컨테이너 팩토리를 커스터마이즈로 생성하자.
     */
    fun createContainer(
        queueName: String,
        messageListener: MessageListener,
    ): DirectMessageListenerContainer =
        DirectMessageListenerContainer(connectionFactory).apply {
            setQueueNames(queueName)
            setAdviceChain(retryInterceptor)
            setConsumersPerQueue(1)
            setMessageListener(messageListener)
        }
}