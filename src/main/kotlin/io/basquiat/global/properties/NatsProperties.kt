package io.basquiat.global.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "nats")
data class NatsProperties
    @ConstructorBinding
    constructor(
        val server: String,
        val apiStreamName: String,
        val apiAllowSubject: String,
        val apiHeader: String,
        val maxDelivery: Long,
        val durableSuffix: String,
    )