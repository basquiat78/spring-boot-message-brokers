package io.basquiat.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "basquiat.rabbitmq")
data class RabbitMqProperties
    @ConstructorBinding
    constructor(
        val exchangeName: String,
        val queueName: String,
        val delayExchangeName: String,
        val delayQueueName: String,
        val dlxRoutingKey: String,
    )