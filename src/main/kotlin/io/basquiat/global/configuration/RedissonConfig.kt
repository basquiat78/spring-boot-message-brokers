package io.basquiat.global.configuration

import io.basquiat.global.utils.mapper
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RedissonConfig(
    @Value($$"${spring.data.redis.host:localhost}")
    private val redisHost: String,
    @Value($$"${spring.data.redis.port:6379}")
    private val redisPort: Int,
    @Value($$"${spring.data.redis.password:}")
    private val redisPassword: String?,
    @Value($$"${spring.data.redis.database:0}")
    private val redisDatabase: Int,
    @Value($$"${redisson.protocol:redis://}")
    private val protocol: String,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()

        val redissonMapper = mapper.copy()

        config.apply {
            if (!redisPassword.isNullOrBlank()) {
                password = redisPassword
            }
            nettyThreads = 16
            codec = TypedJsonJacksonCodec(Any::class.java, redissonMapper)
        }

        config.useSingleServer().apply {
            address = "$protocol$redisHost:$redisPort"
            database = redisDatabase
            timeout = 3000
            connectTimeout = 5000
            connectionMinimumIdleSize = 10
            connectionPoolSize = 15
            retryAttempts = 3
            retryDelay = ConstantDelay(Duration.ofMillis(100))
        }
        return try {
            Redisson.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Redisson 연결 실패! 주소: redis://$redisHost:$redisPort, 원인: ${e.message}", e)
        }
    }
}