# Redis Distributed Lock

이전 브랜치에서 `NATS`의 `Request-Reply`를 작업하면서 동시성 문제를 해결하지 않았다.

따라서 여기서는 `Redis`의 분산락에 대해 소개하고 이것을 통해서 동시성을 해결해 보고자 한다.

# 분산락

만일 단일 인스턴스에 떠있는 서버라면 우리가 가장 먼저 생각할 수 있는 것은 `synchronized`나 `ReentrantLock`를 이용해 락을 거는 방식이다.

또는 디비 레벨에서 지원하는 `FOR UPDATE`를 통한 `DB Row-level Lock`을 고려해 볼 수 있다.

하지만 만일 여러 대의 서버나 여러 개의 프로세스가 존재하게 되면 일단 `synchronized`나 `ReentrantLock`은 사용할 수 없다.

당연한 이야기겠지만 여러 개의 서버가 이 방식을 사용한다 하더라도 그건 서버내에서만 작동할 뿐 다른 서버의 락 여부는 알 수가 없다.

그러면 고려해 볼 수 있는 것은 `DB Row-level Lock`이다.

물론 불가능한것은 아니다. 하지만 경험상 이 방식은 확실히 디비에 부하를 준다.

이것은 디비쪽으로 트랜잭션과 락이 몰림 현상이 발생한다.

이런 현상이 생기면 먼저 생각나는 것은 디비 커넥션 문제이다.

개다가 `FOR UPDATE`를 사용할텐데 계속 락을 잡게 되면 다른 쿼리들에 사이드 이펙트 문제를 발생시킨다.

예를 들면 상품의 재고를 위해 특정 `row`를 락을 잡고 있는데 다른 곳에서 단지 상품의 이름을 알고 싶어도 접근하지 못하는 문제가 발생한다.

그래서 `Timeout`이 발생하는 경우도 볼 수 있다.

거기에 만일 디비를 샤딩한 구조를 가지고 있다면 이것도 거의 불가능하다고 생각한다.

그래서 가장 많이들 사용하는 방식이 `Redis`를 이용한 분산 락이다.

`etcd`를 통해서도 이것을 구현할 수 도 있다.

하지만 기존의 `Redis`를 세팅했으니 이 방식을 가져가보도록 한다.

`RedLock`은 사실 `Redis`를 클러스터나 센티넬로 구성할 때 사용하는데 최근 `Redisson`의 최신 버전에서는 일반적인 `RLock`으로도 충분히 커버가 가능하다.

*RedLock이 좀 복잡하긴 하다...*

# 분산락을 위한 애노테이션 설정

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DistributedLock(
    val key: String,
    val waitTime: Long = 5L,
    val leaseTime: Long = 3L,
    val useWatchdog: Boolean = true,
)
```
먼저 `AOP`를 위한 애노테이션을 설정한다.

그리고 이것을 구현하는 `Aspect`가 필요하다.


```kotlin
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class DistributedLockAspect(
    private val redissonClient: RedissonClient,
) {
    private val log = logger<DistributedLockAspect>()

    private val parser = SpelExpressionParser()

    @Around("@annotation(io.basquiat.global.annotations.DistributedLock)")
    fun lock(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature

        val distributedLock = signature.method.getAnnotation(DistributedLock::class.java)

        val dynamicKey =
            getDynamicValue(
                signature.parameterNames,
                joinPoint.args,
                distributedLock.key,
            )

        if (dynamicKey.isBlank()) distributedLockError("락 키를 생성할 수 없습니다. (Key: ${distributedLock.key})")

        val lockName = "lock:$dynamicKey"
        val redissonLock = redissonClient.getLock(lockName)

        // useWatchdog이 true이면 leaseTime을 -1로 설정하여 Redisson Watchdog 활성화
        // 이를 통해 락 릴리즈 시간을 자동으로 연장하도록 처리한다.
        val leaseTime = if (distributedLock.useWatchdog) -1L else distributedLock.leaseTime

        val startTime = System.currentTimeMillis()

        return try {
            val locked = redissonLock.tryLock(distributedLock.waitTime, leaseTime, TimeUnit.SECONDS)
            val waitDuration = System.currentTimeMillis() - startTime

            if (!locked) {
                log.warn("[DISTRIBUTED_LOCK] TIMEOUT | Key: {} | Wait: {}ms", lockName, waitDuration)
                distributedLockError("락 획득에 실패했습니다: $lockName")
            }

            log.info(
                "[DISTRIBUTED_LOCK] SUCCESS | Key: {} | Wait: {}ms | Watchdog: {}",
                lockName,
                waitDuration,
                distributedLock.useWatchdog,
            )

            // joinPoint 실행
            joinPoint.proceed()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            distributedLockError("락 대기 중 인터럽트: $e")
        } finally {
            // 락 해제 시 현재 스레드가 hold중인지 확실하게 체크한다.
            if (redissonLock.isHeldByCurrentThread) {
                try {
                    redissonLock.unlock()
                    log.info("락 해제 완료: $lockName")
                } catch (_: IllegalMonitorStateException) {
                    // 만료되었거나 어떤 알수 없는 이유로 해제할 수 없는 경우 방어적으로 예외 처리
                    log.warn("락이 이미 해제되었거나 소유하고 있지 않습니다: $lockName")
                } catch (e: Exception) {
                    log.error("락 해제 중 예상치 못한 에러 발생: $lockName", e)
                }
            }
        }
    }

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
        }.getOrNull() ?: distributedLockError("SpEL 파싱 실패 또는 결과가 null입니다. (Key: $key)")
    }
}
```

이것은 다음과 같은 흐름을 갖게 된다.


## 1. AOP에 의한 락 진입

먼저 `@DistributedLock`이 붙은 메소드가 호출하게 되면 위 `Aspect`가 실행된다.

이때 중요한 것은 `@Order(Ordered.HIGHEST_PRECEDENCE)`이다.

스프링에서 트랜잭션을 위해서 `@Transactional`을 붙인다.

잘 알려진 내용이지만 이것은 스프링의 `AOP Advice`로 동작하게 되는 내부적으로 스프링은 `@EnableTransactionManagement`을 통해 등록되어진다.

이 때 순서를 정하게 되는데 찾아 보면 `@Order(Ordered.LOWEST_PRECEDENCE)`로 옵션이 붙는다.

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({TransactionManagementConfigurationSelector.class})
public @interface EnableTransactionManagement {
    boolean proxyTargetClass() default false;

    AdviceMode mode() default AdviceMode.PROXY;

    int order() default Integer.MAX_VALUE;

    RollbackOn rollbackOn() default RollbackOn.RUNTIME_EXCEPTIONS;
}
```
코드를 보면 알겠지만 `Ordered.LOWEST_PRECEDENCE`인 `Integer.MAX_VALUE`가 기본값인 것을 알 수 있다.

순서를 가장 낮게 잡는다는 것을 알 수 있다.

그렇다면 이게 왜 중요할까?

트랜잭션이 벌어지기 전에 먼저 락을 잡아야 하기 때문이다.

만일 트랜잭션이 먼저 잡히고 락을 잡으면 예상치 않은 문제가 발생한다.

# 트랜잭션 이후 락을 잡는다면

이렇게 생각해 보자

```text
트랜잭션 시작 → 락 획득 시도 → 비지니스 로직 → 락 해제 → 트랜잭션 종료
```

문제가 없어보이지만 여러개의 요청이 들어오게 되면 모든 요청들이 트랜잭션을 먼저 시작하게 될 것이다.

동시성을 보장하기 위해서인데 일단 트랜잭션을 모든 요청들이 일단 실행되면 불필요한 커넥션이 생기고 인한 리소스 낭비가 먼저 벌어진다.

그 속에서 다른 많은 요청들이 붙잡고 있을 트랜잭션 시간이라든가 이런것들을 고려해 봐야 한다.

하지만 가장 큰 문제는 `락 해제 -> 트랜잭션 종료`여기서 문제가 발생할 수 있다.

아직 트랜잭션에 의한 DB 커밋이 안되는 문제가 발생하게 되면 다른 스레드와 `Race Condition`이 발생할 수 있다.

잘 생각해 보자. 락이 해제되는 시점에는 아직 커밋이 되지 않았다.

하지만 다른 스레드에서 접근이 가능해지게 되는데 정말 그 짧은 시간에 커밋이 안되었다면 다른 스레드는 커밋전 데이터를 보게 된다.

만일 재고같은 데이터를 처리한다면 문제가 발생할 수 있는 것이다.

그래서 분산락을 위한 `AOP`진입 시점에 먼저 실행이 되도록 순서를 조절하게 되면 아래와 같은 흐름으로 진행된다.

```text
락 획득 → 트랜잭션 시작  → 비지니스 로직 → 트랜잭션 종료 → 락 해제
```

많은 요청, 즉 다건의 스레드가 돌 때 락을 먼저 걸게 되면 커넥션 풀은 락에 의해 하나만 잡게 되므로 먼저 리소스에 대한 부담이 줄어 든다.

그리고 락이 걸린 상태에서 모든 프로세스를 진행하고 락을 해제하게 되므로 `Race Condition`도 발생하지 않는다.


![해당 로직 Mermaid 흐름도](https://github.com/basquiat78/spring-boot-message-brokers/blob/05-with-distributed-lock/ddl/distributedlock.png)


위 그림은 `DistributedLockAspect`의 흐름을 도식화한 이미지이다.

여기서는 `Redisson`의 `Watchdog`기능 여부에 따라 자동으로 연장할 수 있도록 처리하게 했다.

락 해제 시간을 어느 정도 잡고 갈 수 있는 부분이 있다면 락을 붙잡고 있는 시간을 결정할 수 있다.

하지만 어떤 문제로 만일 락 해제 시간내에 로직 수행이 완료되지 않을 것을 고려해 자동으로 연장을 해야 하는데 `Watchdog`기능을 활용한다.

또한 락 해제하는 시점에 `isHeldByCurrentThread`을 두고 있는데 이것은 내 락이 아닌 다른 락을 실수로 해제하는 것을 방지한다.

# 더 이상 자세한 설명이 필요할까?

사실 이 내용은 너무나 잘 알려진 내용이다.

현재 로직은 예전에 분산락 관련 작업하면서 보완했던 코드로 다른 분들과 큰 차이가 없을 것이다.

# 그렇다면 이제는 테스트!

해적단에 합류하거나 (order) 또는 해적단 합류에 예약/취소의 경우에는 실제 재고 차감이 이뤄진다.

해적단 합류 확정은 이미 재고를 선점하고 하는 것이라 불필요하지만 저 3개의 로직은 락을 통해서 재고에 대한 동시성 문제를 제어해야 한다.

`sql.ddl`에 보면 `니코 로빈 포네그리프 사본`의 재고가 20개 이다.

만일 100명이 해적단을 합류하겠다고 할때 20명만 해적단에 들어올 수 있고 나머지는 해적단 합류를 할 수 없다.


```kotlin
@Service
class PlaceOrderHandler(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    @DistributedLock(key = "#request.productId", waitTime = 10L, leaseTime = 3L, useWatchdog = true)
    fun execute(request: PlaceOrder): PlaceOrderResponse {
        val (productId, quantity) = request

        val product =
            productRepository
                .findById(productId)
                .orElseThrow { NoSuchElementException("해당 보물을 찾을 수 없습니다.") }
        if (product.quantity < quantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        product.quantity -= quantity
        val entity =
            Order(
                product = product,
                quantity = quantity,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderRepository.save(entity)
        return PlaceOrderResponse(orderId = completeOrder.id)
    }
}
```
이렇게 앞서 생성한 애노테이션을 붙이고 아래 테스트 코드를 확인해 보자


```kotlin
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class LockTest
    @Autowired
    constructor(
        private val placeOrderUsecase: PlaceOrderUsecase,
        private val productService: ProductService,
    ) {
        @Test
        fun `Lock 테스트하기`() {
            // given
            val productId = 7L
            val quantity = 1
            val request = OrderCreateDto(productId = productId, quantity = quantity)
            // when
            val requestCount = 100
            val executorService = Executors.newFixedThreadPool(15)
            val latch = CountDownLatch(requestCount)

            val successCount =
                AtomicInteger(0)
            val failCount =
                AtomicInteger(0)

            // When
            for (i in 1..requestCount) {
                executorService.submit {
                    try {
                        val response = placeOrderUsecase.execute(request)
                        if (response.orderId != null) {
                            println("해적단 합류 성공 아이디: ${response.orderId}")
                            successCount.incrementAndGet()
                        } else {
                            unableToJoin(response.error)
                        }
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                        println("해적단 합류 실패 원인: ${e.message}")
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()

            val product = productService.findByIdOrNull(productId) ?: notFound("해당 보물을 찾을 수 없습니다. 해적단 합류 아이디: $productId")
            assertThat(product.quantity).isEqualTo(0)
            // 성공 횟수 20 실패 횟수 80이 나와야 한다.
            println("성공 횟수: ${successCount.get()}, 실패 횟수: ${failCount.get()}")
        }
    }
```

서버를 띄운 상태에서 포스트맨을 통해서 테스트를 하고자 한다면 다음과 같이 해보자.

```text
1. 포스트맨 좌측 메뉴 [Collections]을 클릭
2. 검색창 좌측에 있는 [+]버튼을 눌러서 새로운 collections을 만든다.
3. New Collections가 생길텐데 그 라인에 [+]을 누른다.
4. 그러면 메인 상단에 API를 입력하는 창이 나온다.
5. POST로 http://localhost:8080/api/orders를 입력한다.
6. Body를 선택하고 raw (JSON) 세팅
{
  "productId": 7,
  "quantity": 1
}
7. 그리고 저장을 한다.
8. New Collections 라인에 [...]을 보면 Run을 클릭한다.
9. 화면이 바뀌는데 아까 저장한 API를 선택하고 Run Configuration에서 iterations에 동시에 보낼 숫자를 입력한다.
10. 실행
```

# Next Step