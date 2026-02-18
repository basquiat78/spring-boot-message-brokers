package io.basquiat.global.configuration

import io.basquiat.global.utils.mapper
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.codec.TypedJsonJacksonCodec
import org.redisson.config.Config
import org.redisson.config.ConstantDelay
import org.redisson.config.NatMapper
import org.redisson.misc.RedisURI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RedissonConfig(
    @Value($$"${spring.data.redis.sentinel.master:basquiat}")
    private val masterAlias: String,
    @Value($$"${spring.data.redis.sentinel.nodes:127.0.0.1:26400}")
    private val sentinelNodes: List<String>,
    @Value($$"${spring.data.redis.password:}")
    private val redisPassword: String?,
    @Value($$"${spring.data.redis.database:0}")
    private val redisDatabase: Int,
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

        // useSingleServer 대신 useSentinelServers을 사용하자.
        config.useSentinelServers().apply {
            setMasterName(masterAlias)

            sentinelNodes.forEach { node ->
                addSentinelAddress("redis://$node")
            }

            setDatabase(redisDatabase)

            // redis에서 센티널 노드 리스트를 확인하지 않도록 false 처리한다.
            setCheckSentinelsList(false)
            setNatMapper(
                object : NatMapper {
                    override fun map(address: RedisURI): RedisURI {
                        // master-replica 도커 내부 포트
                        if (address.port == 6379) {
                            return RedisURI(address.scheme, "127.0.0.1", 6379)
                        }
                        // 모든 걸 무시하고 원래 주소를 반환하도록 한다.
                        return address
                    }
                },
            )

            setTimeout(3000)
            setConnectTimeout(5000)
            setMasterConnectionMinimumIdleSize(10)
            setMasterConnectionPoolSize(15)
            setSlaveConnectionMinimumIdleSize(10)
            setSlaveConnectionPoolSize(15)

            retryAttempts = 3
            retryDelay = ConstantDelay(Duration.ofMillis(100))
        }

        return try {
            Redisson.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Redisson 센티넬 연결 실패! 마스터: $masterAlias, 원인: ${e.message}", e)
        }
    }
}