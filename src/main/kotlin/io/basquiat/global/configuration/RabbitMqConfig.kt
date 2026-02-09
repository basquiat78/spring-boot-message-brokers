@file:Suppress("DEPRECATION")

package io.basquiat.global.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import io.basquiat.global.annotations.ConditionalOnAmqp
import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.properties.RabbitMqProperties
import io.basquiat.global.utils.mapper
import org.aopalliance.intercept.MethodInterceptor
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import java.time.Duration

@Configuration
@ConditionalOnAmqp
class RabbitMqConfig(
    private val props: RabbitMqProperties,
) {
    /**
     * 재시도 정책 설정
     */
    @Bean
    fun retryInterceptor(): MethodInterceptor {
        val customPolicy =
            RetryPolicy
                .builder()
                .maxRetries(3)
                .delay(Duration.ofSeconds(2))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .build()

        return RetryInterceptorBuilder
            .stateless()
            .retryPolicy(customPolicy)
            .recoverer(RejectAndDontRequeueRecoverer())
            .build()
    }

    /**
     * 문제 발생시 재시도 발행하도록 설정
     */
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryPolicy =
            RetryPolicy
                .builder()
                .maxRetries(3)
                .delay(Duration.ofSeconds(1))
                .includes(Throwable::class.java) // 모든 예외 재시도 허용
                .build()
        return RetryTemplate(retryPolicy)
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter()
        template.setRetryTemplate(retryTemplate())
        return template
    }

    /**
     * BrokerChannel enum에 정의된 모든 채널을 보고 RabbitMQ의 Exchange, Queue, Binding을 자동 생성한다.
     */
    @Bean
    fun dynamicRabbitDeclarables(): Declarables {
        val mainExchange = DirectExchange(props.exchangeName)
        val declarables = mutableListOf<Declarable>(mainExchange)

        BrokerChannel.entries.forEach { channel ->
            val queueName = channel.channelName
            val delayQueueName = "$queueName-delay"

            val queue =
                QueueBuilder
                    .durable(queueName)
                    .withArgument("x-dead-letter-exchange", "")
                    .withArgument("x-dead-letter-routing-key", delayQueueName)
                    .build()

            val binding = BindingBuilder.bind(queue).to(mainExchange).with(queueName)

            val delayQueue =
                QueueBuilder
                    .durable(delayQueueName)
                    .withArgument("x-dead-letter-exchange", props.exchangeName)
                    .withArgument("x-dead-letter-routing-key", queueName)
                    .withArgument("x-message-ttl", 30000)
                    .build()

            declarables.add(queue)
            declarables.add(binding)
            declarables.add(delayQueue)
        }
        return Declarables(declarables)
    }

    /**
     * 지연 큐 (Delayed Message Queue) 설정
     */
    @Bean
    fun delayQueue(): Queue =
        QueueBuilder
            .durable(props.delayQueueName)
            .withArgument("x-dead-letter-exchange", props.exchangeName)
            .withArgument("x-dead-letter-routing-key", props.dlxRoutingKey)
            .build()

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = Jackson2JsonMessageConverter(mapper)
        return converter
    }
}