package io.basquiat.global.annotations

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.messaging.use-amqp"], havingValue = "true")
annotation class ConditionalOnAmqp