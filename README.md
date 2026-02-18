# Redis Redlock Setup

`Redis`에서 `Redlock`은 센티넬 구성이 아닌 각기 다른 독립된 `Redis`을 통해서 락을 거는 것이다.

일반적으로 3대 이상으로 구성된 각기 다른 복제본이 아닌 독립된 마스터 레벨의 `Redis`를 통해서 락을 거는 방식이다.

위 설명대로라면 의문이 든다.

_왜 독립된 각각의 `Redis`를 통해서 분산락을 구현해야 하는거지??_

사실 나도 그런 의문이 들지만 정교하게 분산락을 거는건가라고만 생각을 했다.

다음과 같은 이유로 이런 방식을 택한다고 한다.

- 과반수(Quorum): 과반수 이상의 노드에서 승인을 얻어야 락이 유효함을 보장한다.
- 원자성: 시스템 장애 상황에서도 락을 원자적으로 보장한다.

이것은 단일 노드에서 발생하는 `SPOF`가 없고 하나의 노드가 죽어도 분산락에 영향을 주지 않는다.
따라서 안정적인 분산락을 구현한다는 것이다.

그러면 이것이 어떻게 동작하는지도 알아봐야 한다.

# RedLock 프로세스

1. Time Log (시간 기록)
    - 락 회득전에 먼저 시간을 기록한다.
2. 순차적 획득 시도
   - 3대 (또는 그 이상)의 모든 노드에 순차적으로 락 획득을 요청
   - 이때 각 요청의 Timeout은 매우 짧게 설정한다
   - 한 대가 죽어있더라도 락 획득 프로세스가 멈추지 않게 한다
3. 과반수 체크
   - 3대 중 2대 이상에서 락을 획득 (홀수대 중 2배수 짝수)
   - 락 획득에 걸린 총 시간이 락 유효 시간보다 짧은지 체크
4. 최종 유효 시간 계산
    - 락을 사용할 수 있는 시간은 [처음 설정한 유효 시간]에서 [락 획득에 걸린 총 시간]을 뺀 시간
5. 실패 시 해제
   - 과반수 획득에 실패했다면, 락을 획득했던 모든 노드에 해제(Unlock) 요청을 보낸다.


프로세스만 보면 몇 가지 드는 생각이 든다.

~~락을 획득하는데 무슨 과반수를 얻어야 하는거야????~~

~~헤드락 아니~~ 분산락을 거는데 진심이네?

하지만 `Redis`를 3대 이상 독립적으로 운영해야 한다는 단점과 아무래도 속도를 조금은 포기해야 하는 단점이 보인다.

속도를 조금은 포기하고 정말 정밀한 분산락을 요구하는 시스템이라면 고려해 볼 만한 방식이다라는 것을 먼저 언급하고 진행을 해보자.

# 3대의 독립된 Redis를 띄워보자.

현재 `broker > redis > redlock` 폴더내의 `docker-compose.yml`에 설정을 해 놨다.

보안을 중요하게 생각한다는 마인드로 `command: redis-server --requirepass basquiat`을 통해 비밀번호를 `basquiat`로 설정한 것을 확인할 수 있다.

기존에 띄운 정보를 다 내리고 이것을 실행하자.

# 설정 변경

기존 방식이 바뀌기 떄문에 다음과 같이 도커 컴포즈로 띄운 정보를 설정을 한다.

그리고 `spring:data`밑에 있는 기존 설정은 전부 주석처리를 해야 한다.

```yaml
redisson:
  protocol: "redis://"
  # 상용이나 ssl을 적용해야 한다면
  # protocol: "rediss://"

redlock:
  nodes:
    - address: "${redisson.protocol}127.0.0.1:6400"
      password: basquiat
    - address: "${redisson.protocol}127.0.0.1:6401"
      password: basquiat
    - address: "${redisson.protocol}127.0.0.1:6402"
      password: basquiat
  database: 0
  timeout: 3000
```

그리고 이것을 매핑할 객체를 하나 만들자.

```kotlin
@ConfigurationProperties(prefix = "redlock")
data class RedlockProperties
@ConstructorBinding
constructor(
    val nodes: List<RedlockNode>,
    val database: Int = 0,
    val timeout: Int = 3000,
)

data class RedlockNode(
    val address: String = "",
    val password: String? = null,
)
```

이제는 기존의 싱글로 띄운 `ReddisonConfig`는 다음과 같이 변경되어야 한다.

```kotlin
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
```
각각의 독립된 `Redis`의 클라이언트를 리스트로 만들어서 빈으로 등록한다.

이제는 `RedisCacheConfig`에도 영향을 준다.

따라서 아래와 같이 `RedisCacheConfig`를 변경한다.

빈으로 등록한 클라인트 리스트에서 첫번째 `Redis` 노드만 캐시로 사용한다.

```kotlin
@file:Suppress("DEPRECATION")
@Configuration
@EnableCaching
class RedisCacheConfig(
    private val redissonClients: List<RedissonClient>,
) : CachingConfigurer {
    @Bean
    fun redisCacheManager(): RedisCacheManager {
        // 첫번째 클라이언트에서 connectionFactory를 가져온다.
        val connectionFactory = RedissonConnectionFactory(redissonClients[0])

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
```

기존의 애노테이션도 다음과 같이 변경한다.

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedRedLock(
    val key: String,
    val waitTime: Long = 10L,
    val leaseTime: Long = 5L,
    // 락을 걸고 릴리즈하는 시간의 단위를 명시적으로 넘긴다.
    val timeUnit: TimeUnit = TimeUnit.SECONDS,
)
```
이 때 기존에 사용한 `WatchDog`옵션은 의미가 없어진다. 그리고 시간 단위를 명확하게 명시하자.

최종적으로 다음과 같이 `Aspect`도 변경되어야 한다.

```kotlin
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DistributedRedLockAspect(
    private val redissonClients: List<RedissonClient>,
) {
    private val log = logger<DistributedRedLockAspect>()
    private val parser = SpelExpressionParser()

    @Around("@annotation(io.basquiat.global.annotations.DistributedRedLock)")
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val method = signature.method
        val annotation = method.getAnnotation(DistributedRedLock::class.java)

        val dynamicKey = getDynamicValue(signature.parameterNames, joinPoint.args, annotation.key)
        if (dynamicKey.isBlank()) distributedLockError("Redlock 키를 생성할 수 없습니다. (Key: ${annotation.key})")

        val lockName = "redlock:$dynamicKey"

        // 기존과 다른 점은 3개의 독립된 Redis로부터 락을 가져온다.
        val locks = redissonClients.map { it.getLock(lockName) }

        // 이제는 RedissonMultiLock을 통해 Redlock을 사용해야 한다.
        // 2개 이상의 노드에서 성공해야 락을 획득할 수 있다. RedLock 알고리즘
        val multiLock = RedissonMultiLock(*locks.toTypedArray())

        val startTime = System.currentTimeMillis()
        log.info("[REDLOCK_ATTEMPT] Target: {} | Nodes: {}", lockName, redissonClients.size)

        return try {
            val locked =
                multiLock.tryLock(
                    annotation.waitTime,
                    annotation.leaseTime,
                    annotation.timeUnit,
                )

            val waitDuration = System.currentTimeMillis() - startTime

            if (!locked) {
                log.warn("[REDLOCK_TIMEOUT] Failed to acquire lock for {} | Wait: {}ms", lockName, waitDuration)
                distributedLockError("Redlock 획득 실패 (과반수 노드 응답 없음 또는 시간 초과): $lockName")
            }

            log.info("[REDLOCK_SUCCESS] Acquired lock for {} | Wait: {}ms", lockName, waitDuration)

            joinPoint.proceed()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            distributedLockError("Redlock 대기 중 인터럽트 발생: ${e.message}")
        } catch (e: Exception) {
            log.error("[REDLOCK_ERROR] 로직 수행 중 예외 발생: {}", e.message)
            throw e
        } finally {
            try {
                if (multiLock.isHeldByCurrentThread) {
                    multiLock.unlock()
                    log.info("[REDLOCK_RELEASE] Lock released for {}", lockName)
                }
            } catch (e: Exception) {
                log.error("[REDLOCK_RELEASE_ERROR] Lock release failed for {}: {}", lockName, e.message)
            }
        }
    }

    /**
     * SpEL 파싱 로직
     */
    private fun getDynamicValue(
        parameterNames: Array<String>,
        args: Array<Any?>,
        key: String,
    ): String {
        val context = StandardEvaluationContext()
        for (i in parameterNames.indices) {
            context.setVariable(parameterNames[i], args[i])
        }

        return runCatching {
            parser.parseExpression(key).getValue(context, Any::class.java)?.toString()
        }.getOrNull() ?: distributedLockError("SpEL 파싱 실패: $key")
    }
}
```

기존과 크게 다르지 않지만 3개의 독립된 `Redis`로 부터 락을 획득하고 `Redisson`의 `RedissonMultiLock`을 활용한다.

`Redisson`에서는 `RedLock`알고리즘을 제공하고 있기 때문에 우리는 고수준 `API`로 쉽게 활용할 수 있다.

# 테스트

이전에 우리가 활용한 `PlaceOrderHandler`에 이것을 적용해 보자.


```kotlin
@Service
class PlaceOrderHandler(
    private val productService: ProductService,
    private val orderService: OrderService,
) {
    @Transactional
    @DistributedRedLock(key = "#request.productId", waitTime = 10L, leaseTime = 5L)
    @CacheEvict(value = ["product"], key = "#request.productId")
    fun execute(request: PlaceOrder): PlaceOrderResponse {
        val (productId, quantity) = request

        val product =
            productService
                .findByIdOrThrow(productId, "해당 보물을 찾을 수 없습니다. 보물 아이디: $productId")
        if (product.quantity < quantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        product.quantity -= quantity
        val entity =
            Order(
                product = product,
                quantity = quantity,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderService.create(entity)
        return PlaceOrderResponse(orderId = completeOrder.id)
    }
}
```

기존에 `LockTest`에서 수량을 디비에서 변경하고 입맛에 맞게 테스트를 해보면 된다.


# At a Glance

이거면 정말 완벽할까?

사실 `RLock`으로도 충분하다고 하는데 `RedLock`에 대한 일명 `Martin Kleppmann의 비판`이 존재한다.

요지는 다음과 같다.


1. Redlock이 TTL·시간 가정에 너무 의존한다.

2. 클라이언트 스톨/네트워크 지연 상황에서 두 클라이언트가 동시에 락을 가진 상태가 될 수 있다.

3. 펜싱 토큰 같은 메커니즘이 없다.
    - 데이터 정합성이 걸린 핵심 락으로는 안전하지 않다는 주장.

`GC Pause Problem`, `clock drift 현상`, `펜싱 토큰 부재`라고 말한다.

이 내용에 대해서 제안하는 내용은 다음과 같다.

```text
ZooKeeper / etcd / Consul 같은 CP 계열 합의 시스템 위에 락 서비스를 두고, 락마다 단조 증가하는 펜싱 토큰(fencing token) 을 발급해서, 실제 리소스 쪽에서 “토큰이 예전 것(낮은 값)이면 거부” 하는 구조를 쓰라는 것
```

`etcd`도 지금 살펴보는 중이다.

내용이 정리되는대로 한번 이것도 다뤄볼 생각이다.