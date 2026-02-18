package io.basquiat.global.configuration

import io.basquiat.global.properties.RedlockProperties
import io.basquiat.global.utils.mapper
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RedissonConfig(
    private val props: RedlockProperties,
) {
    @Bean
    fun redissonClients(): List<RedissonClient> =
        props.nodes.map { node ->
            val config = Config()
            val redissonMapper = mapper.copy()

            config.apply {
                if (!node.password.isNullOrBlank()) {
                    password = node.password
                }
                nettyThreads = 16
                codec = TypedJsonJacksonCodec(Any::class.java, redissonMapper)
            }

            config.useSingleServer().apply {
                address = node.address
                database = props.database
                timeout = props.timeout
                connectTimeout = 5000
                connectionMinimumIdleSize = 10
                connectionPoolSize = 15
                retryAttempts = 3
                retryDelay = ConstantDelay(Duration.ofMillis(100))
            }

            try {
                Redisson.create(config)
            } catch (e: Exception) {
                throw RuntimeException("Redisson 연결 실패! 주소: $node.address, 원인: ${e.message}", e)
            }
        }
}