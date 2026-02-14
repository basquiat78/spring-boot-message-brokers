@file:Suppress("DEPRECATION")

package io.basquiat.global.configuration

import io.basquiat.global.cache.CacheType
import io.basquiat.global.utils.logger
import io.basquiat.global.utils.mapper
import io.basquiat.global.utils.notFound
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.cache.interceptor.SimpleCacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
@EnableCaching
class RedisCacheConfig : CachingConfigurer {
    @Bean
    fun redisCacheManager(connectionFactory: RedisConnectionFactory): RedisCacheManager {
        val cacheConfigs =
            CacheType.entries.associate { type ->
                type.cacheName to createCacheConfiguration(type)
            }

        val defaultConfig =
            cacheConfigs[CacheType.DEFAULT.cacheName]
                ?: notFound("기본 캐시 설정(DEFAULT)이 Enum에 정의되지 않았습니다.")

        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }

    private fun createCacheConfiguration(type: CacheType): RedisCacheConfiguration {
        val serializer = Jackson2JsonRedisSerializer(mapper, type.clazz)
        return RedisCacheConfiguration
            .defaultCacheConfig()
            .entryTtl(type.ttl)
            .disableCachingNullValues()
            .computePrefixWith { cacheName -> "onepiece:$cacheName:" }
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()),
            ).serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer),
            )
    }

    /**
     * 캐시 관련해서 redis가 어떤 이유로 장애가 발생하면 문제에 대한 로그만 남기도록 한다.
     */
    override fun errorHandler(): CacheErrorHandler =
        object : SimpleCacheErrorHandler() {
            override fun handleCacheGetError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
            ) {
                logger<RedisCacheConfig>().error("Redis Get Error [Key: $key]: ${exception.message}")
            }

            override fun handleCachePutError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
                value: Any?,
            ) {
                logger<RedisCacheConfig>().error("Redis Put Error [Key: $key]: ${exception.message}")
            }

            override fun handleCacheEvictError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
            ) {
                logger<RedisCacheConfig>().error("Redis Evict Error [Key: $key]: ${exception.message}")
            }
        }
}