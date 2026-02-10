package io.basquiat.global.configuration

import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory

@Configuration
class KafkaConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer, // 스프링이 제공하는 설정 대리인
        consumerFactory: ConsumerFactory<Any, Any>,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        // yaml 설정 그대로 구성한다.
        configurer.configure(factory, consumerFactory)
        factory.containerProperties.listenerTaskExecutor =
            SimpleAsyncTaskExecutor("kafka-virtual-threads-").apply {
                setVirtualThreads(true)
            }
        return factory
    }
}