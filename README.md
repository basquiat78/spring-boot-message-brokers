# Redis Cache

캐쉬의 중요성은 딱히 설명할 필요가 있을까?

당연한 이야기겠지만 대부분의 웹은 디비를 사용하게 된다.

어떤 데이터를 저장하고 업데이트하고 수정하는 행위가 `RDBMS`를 통해 이뤄지게 된다.

트래픽이 많지 않다면 별 의미가 없겠지만 만일 프로모션이나 이벤트가 별어지면서 동시 접속 사용자가 많아지면 디비에 많은 요청을 하게 될것이다.

우리가 어떤 상품이나 리스트를 볼 때 이런 정보들은 특별한 경우가 아니면 데이터가 변경되지 않는다.

물론 재고관련 변경되는 정보들이 있겠지만 다른 정보들이 변경되는 일은 거의 없다.

동시 접속 사용자를 1000명이라고 특별한 이벤트를 하는 한 상품이 있다고 생각해 보자.

1000명이 특정상품을 보기 위해 접근할 것이고 상품 정보를 가져오기위해 1000번의 트랜잭션이 디비에서 일어나게 될것이다.

하지만 만일 최초 한번 디비에서 상품을 가져오고 그 이후 정보를 캐시에서 가져오게 된다면 이 횟수를 획기적으로 줄일 수 있다.

디비의 부하를 확 줄이면 그만큼 동시 접속 사용자의 트래픽을 처리하는데 부담이 굉장히 줄게 된다.

이걸 또 설명할 이유가 없겠지만 쓰고 보니 대충 끄적여 보면 그렇다

위에 언급한 내용을 토대로 다음과 같은 개념을 알고 있어야 한다.

이미 알고 있을 수 있겠지만 설명은 해야 하니깐...

# Cache Hit 와 Cache Miss

말 그대로다

캐시에 데이터가 있으면 그 정보를 가져온다.

어? 없네?

이런 상황이면 디비에서 데이터를 조회하고 그 데이터는 캐시에 넣는다.

# 의문점이 든다.

_**그럼 메모리에 올려놓고 사용하면 되겠네?**_

라고 하겠지만 만일 디비에서 조회하는 상품이 엄청 많다면 그만큼 메모리에 올라오게 될 것이다.

당연하겠지만 메모리도 리소스이다.

이런 이유로 무작정 메모리에 남겨둘 수 없다. 
그래서 보통 캐시가 유효한 시간인 `ttl`을 설정하게 된다.

비록 짧은 시간이지만 10000명이 거의 동시대에 들어온다 해도 이 짧은 시간 설정한 값만으로도 디비에 엄청난 부담을 덜어줄 수 있다.

이렇게 말하고 보니 개념이라고까지 설명할 필요도 없다.

# Cache Strategy

캐시는 크게 두가지 전략으로 나눈다.

하나는 `Read Cache Strategy`고 하나는 `Write Cache Strategy`

## Read Cache Strategy

### Look-Aside (Cache Aside) Pattern

가장 대중적이고 우리가 흔히 사용하는 캐시 전략이라면 이 방식일 것이다.

최초 한번 DB에서 조회한 정보를 메모리에 올리고 이때 설정한 `ttl`동안 캐시에서 가져오는 방식이다.

쉽게 구성할 수 있지만 이 방식은 `ttl`을 얼마나 유지하냐가 굉장히 중요해진다.

그리고 캐시가 없으면 디비에서 조회를 하고 그 데이터를 캐시에 쓰는 방식이다.

가장 쉽게 생각할 수 있는 방식이 아닌가?

#### Thundering Herd???

위에서 `cache miss`에 대해서 설명했는데 만일 엄청난 요청이 들어오고 있는데 `cache miss`가 일어난다면?

그 트래픽이 순식간에 디비로 몰리는 현상이 발생할 수 있다.

### Read Through Pattern

실제 사용해 본 적은 없고 개념적으로 설명하자면 이것이다.

~~난 무조건 캐시에서만 데이터를 조회하겠다!!!!!!~~

그러면 캐시가 없을 때 누가 캐시에 쓰나? 라는 궁금증이 생긴다.

이것은 미들웨어에 위임시키는 방식이다.

장점은 데이터의 정합성을 말하는데 사용해 본적이 없어서 이에 대한 자세한 이야기는 할 수 없다.

경험치 부족

## Write Cache Strategy

### Write Back Or Behind Pattern

이것도 사용해 본 적은 없다. 역시 경험치 부족이다.

다만 개념적으로는 이런 방식이다.

어떤 업데이트 정보가 있다면 이것을 바로 디비에 갱신하지 않는다.

그리고 스케쥴링또는 배치 작업을 통해 일정 기간이 지나면 그 캐시를 기준으로 디비와 동기화를 맞춘다.

외부의 관점에서 본다면 캐시는 일종의 큐같은 역할을 하게 된다.

`read/write`가 바로바로 되는것이 아닌 배치를 통해 일정기간이 지나면 실행이 되니 좋아보인다.

디비가 어떤 이유로 장애가 났을 경우 캐시가 그 역할을 해 줄테니 문제가 없겠지만 반대로 생각해 보자.

캐시가 문제가 생기면?

### Write Through Pattern

업데이트 정보가 생기면 캐시에도 저장하고 디비에도 저장을 한다.

외부의 관점에서 보면 두 번의 `write`가 발생하는 것처럼 보인다.

굉장히 안정적일 것이다. 두 번의 쓰기 행위는 데이터의 유실을 최소화하고 동기를 맞추기 때문에 일관성을 갖을 수 있을 것이다.

하지만 그만큼 빈번하게 발생하면 이것도 성능 이슈가 될 수 있지 않을까?

### Write Around Pattern

`Write Through Pattern`는 달리 캐시에 저장하지 않고 디비에만 저장한다.

그리고 `cache miss`가 발생하면 그때 디비와 캐시에 저장하는 방식이다.


# 가장 대중적인 방식

위 읽기/쓰기 전략을 보면 딱 눈에 들어온다.

`Look-Aside (Cache Aside) Pattern`과 `Write Around Pattern` 방식을 조합하는게 찰떡 궁합이 아닌가.

다른 것들은 내가 잘 모르기 때문에 여기서는 이방식으로 진행할 것이다.

사실 대부분 스프링에서 이것을 쉽게 구현할 수 있도록 제공하기 때문에 우리는 위 개념을 가지고 구현만 하면 된다.

# 설정

다음과 같이 `Redis`를 사용할 것이기 때문에 설정을 할 수도 있다.

```yaml
spring:
  cache:
    type: redis
    redis:
      # 명시적으로 만료 시간 10분 (기본값)
      time-to-live: 600s # 기본 만료 시간 (10분)
      # null값도 캐싱하도록 해서 대기 현상을 방지하도록 하자
      cache-null-values: true
```
하지만 우리는 직접 만들것이다.

먼저 `레디스 캐시`를 사용할 부분을 정의를 해서 자동으로 `enum`을 통해서 설정할 수 있도록 `enum`을 하나 만들자.

```kotlin
enum class CacheType(
    val cacheName: String,
    val ttl: Duration,
    val clazz: Class<out Any> = Any::class.java,
) {
    DEFAULT("default", Duration.ofMinutes(10)),
    PRODUCT("product", Duration.ofHours(1), ProductDto::class.java),
    RESERVATION("reservation", Duration.ofHours(1), ReservationDto::class.java),
    ORDER("order", Duration.ofHours(1), OrderDto::class.java),
}
```
이것을 통해 캐시 키와 `ttl`, 그리고 매핑되는 데이터 타입을 정의한다.

기본은 10분, 나머지는 1시간을 잡았는데 이 값은 시스템 상황이나 트래픽을 보고 최적화된 값을 찾는 것이 중요하다.


```kotlin
@file:Suppress("DEPRECATION")

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
```

현재 구조에서 테스트 범위를 좁힐 예정이다.

먼저 특정 아이디에 대한 조회 부분에서 

```kotlin
@Service
class ProductService(
    private val repository: ProductRepository,
) {
    fun create(entity: Product): Product = repository.save(entity)

    @Transactional(readOnly = true)
    fun findByIdOrThrow(
        id: Long,
        message: String? = null,
    ): Product = repository.findByIdOrThrow(id, message)

    @Cacheable(value = ["product"], key = "#productId")
    fun findByIdForCache(productId: Long): ProductDto {
        val product = repository.findByIdOrThrow(productId, "보물을 찾을 수 없습니다. 보물 아이디: $productId")
        return ProductDto.toDto(product)
    }

    @Transactional(readOnly = true)
    fun findByIdOrNull(id: Long): Product? = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    fun findAll(
        lastId: Long?,
        limit: Long,
    ): List<Product> = repository.findAllByCursor(lastId, limit)
}
```
캐시를 적용한 메소드를 하나 만들고

```kotlin
@Service
class FetchProductHandler(
    private val productService: ProductService,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId
        val product =
            productService
                .findByIdForCache(productId)
        return FetchProductResponse(product = product)
    }
}
```
다음과 같이 `Responder`쪽에서 이 메소드를 사용하도록 한다.

다만 고려해 볼 것은 주문이 들어왔을 때 해당 상품의 재고가 차감될 수 있다.

그래서 주문을 받아서 처리하는 `Responsder`에서는 캐시에 저장되어 있는 상품 정보를 `Evict`할 필요가 있다.


```kotlin
@Service
class PlaceOrderHandler(
    private val productService: ProductService,
    private val orderService: OrderService,
) {
    @Transactional
    @DistributedLock(key = "#request.productId", waitTime = 10L, leaseTime = 3L, useWatchdog = true)
    @CacheEvict(value = ["product"], key = "#request.productId") // 핵심: 낡은 캐시를 파괴한다!
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


# 테스트

```kotlin
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class CacheTest
    @Autowired
    constructor(
        private val fetchProductUsecase: FetchProductUsecase,
    ) {
        @Test
        fun `Cache 테스트하기`() {
            // given
            val productId = 7L

            // when
            val requestCount = 50
            val executorService = Executors.newFixedThreadPool(15)
            val latch = CountDownLatch(requestCount)

            // When
            for (i in 1..requestCount) {
                executorService.submit {
                    try {
                        val response = fetchProductUsecase.execute(productId)
                        println(objectToString(response))
                    } catch (e: Exception) {
                        println("해적단 합류 실패 원인: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
        }
    }
```

이 코드를 테스트하게 되면 로그상에서 최초 한번 데이터를 조회하는 쿼리가 실행된 이후 쿼리가 더 이상 실행되지 않는 것을 확인할 수 있다.

그리고 지금 `CacheType`에 보면 `ttl`을 1시간으로 잡아놨기 때문에 테스트를 다시 한번 실행하면 쿼리가 전혀 날아가지 않는다.

하지만 응답값으로 캐시에 있는 정보를 불러와서 응답을 주는 것을 확인할 수 있다.

`Redis`편에서 만일 `Redis Insight`를 설치해서 모니터링 하게 되면 `onepiece:product:7`가 생성되고 

```json
{
"id" : 7,
"name" : "니코 로빈 포네그리프 사본",
"price" : 870000,
"quantity" : 100,
"createdAt" : "2026-02-12T13:40:48.593326",
"updatedAt" : "2026-02-12T13:40:48.593326"
}
```

아래와 같은 캐시 데이터를 확인할 수 있다.

만일 1시간내에 주문이 들어오게 되면 어떻게 될까?

스웨거를 통해서 주문을 해보자.

그러면 바로 `Redis Insight`에서 해당 키로 잡힌 캐시 정보가 사라지게 되는 것을 볼 수 있다.

그리고 다시 스웨거를 통해서 해당 상품을 조회하면 다시 쿼리가 날아가고 계속 조회를 하면 캐시가 사자리기 전까지 쿼리 로그가 찍히지 않는 것을 확인할 수 있다.

# 고민해 볼 사항

캐시를 적용하면 좋지만 그냥 손놓고 있으면 안된다.

실제로 `Redis Cache`에 쌓인 데이터가 가득 차서 문제가 발생한 경험이 있기 때문이다.

그 때 해당 문제를 해결할떄 찾아본 것 중 가장 현실적이었던 방식은 `allkeys-lru`이다.

LRU (Least Recently Used), 결국 오랫동안 사용되지 않은 키를 삭제해서 문제를 해결했다.

여러 방법이 있겠지만 상황에 따라서 결정이 될 건데 하고 싶은 말은 그냥 캐시 잘돌아가니 문제없다고 하면 안된다.

모니터링을 해야 한다.

# Next Step

[번외 - Kafka Stream](https://github.com/basquiat78/spring-boot-message-brokers/tree/07-kafka-stream)