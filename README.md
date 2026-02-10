# Install Redis

기존에 사용하는 `Reids`가 있다면 이 부분은 건너뛰어도 상관없다.

만일 테스트용으로 로컬에서 구동하겠다면 `RabbitMQ`처럼 `brokers > redis` 폴더의 `docker-compose.yml`을 실행해서 로컬에서 테스트해볼 수 있다.
 
```yaml
spring:
  config:
    activate:
      on-profile: local
  data:
    redis:
      host: localhost
      port: 6400
      password:
      database: 0
      timeout: 2000
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
  # 기존 설정...


redisson:
  protocol: "redis://"
  # 상용이나 ssl을 적용해야 한다면
  # protocol: "rediss://"
```

이미 사용중인 `Amazon elasticCache`를 사용한다면 해당 정보로 세팅을 한다.

`Redis`는 `GUI`가 존재한다.

무료 버전으로 사용하고 싶다면

[Redis Insight](https://redis.io/insight/)를 통해서 설치해서 사용하면 된다.

# About Redis

처음 `Redis`를 접하게 된 것은 `pub/sub`때문이었다.

하지만 `Redis`는 `Key–Value` 형식의 메모리 기반 데이터 구조 저장소로 대표적인 `in-Memory DB`이다.

이 외에도 스프링 프레임워크를 사용할 때 세션 기반의 경우에는 세션 저장소나 캐쉬 그리고 리스트 기반의 큐를 설정에 따라 `FIFO(First-In, First-Out)`나 `LIFO(Last-In, First-Out)`를 구현할 수도 있다.

`ZSET`같이 랭킹 관련에 특화된 자료구조 형식등 다양한 자료구조를 활용할 수 있고 분산 락 구현에 각광받는다.

`Streams`도 지원한다.

이렇게 다양한 장점들은 이미 많이 알려져 있기 때문에 이정도 선에서만 설명을 마친다.

# 스프링 부트에서 활용해 보자

스프링에서는 이것을 위한 라이브러리를 제공한다.

또한 `Redisson`을 사용할 것이기 때문에 다음과 같이 추가적으로 그레이들에 추가를 한다.

```groovy
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.redisson:redisson-spring-boot-starter:4.2.0")
```

`Redisson`의 최신 버전은 작성된 시점에서는 `4.2.0`버전이다.

캐쉬는 혹시 사용해 볼 생각이기 때문에 같이 추가를 한다.

기존 `build.gradle.kts`에 의존성을 적용하자.

# 기존 방식에서 벗어나서 직접 리스너를 등록하도록 설정하자.

스프링 부트에서는 `Consumer`를 구현하는 방식은 간단하게 리스너 애노테이션을 달아서 처리할 수 있었던 `RabbitMQ`와는 달리 자체 리스너 애노테이션을 제공하진 않는다.

애초에 `Redis`는 `In-Memory Data Structure Store`가 근간이라 `pub/sub`은 사실 부가적인 기능이라 할 수 있다.

그래서 `Spring Data`프로젝트에 포함된 `Redis`는 `RabbitMQ`에서 애노테이션 대신 직접 등록하도록 설계되어 있다.

여기서는 `Redisson`을 이용할 것이기 때문에 먼저 `RedissonConfig`를 작성하자.

```kotlin
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
```
일반적인 `Redisson`의 환경설정 구성이다.

# Producer 구현

`Redisson`을 통해서 메시지를 전달하는 방식은 단순하다.
채널 정보로 `topic`을 생성하고 이것을 통해 메시지를 전달하는 방식이다.


```kotlin
@Service("redisEventPublisher")
class RedisEventPublisher(
    private val redissonClient: RedissonClient,
) : MessagePublisher {
    private val log = logger<RedisEventPublisher>()

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // RedisChannel에 정의된 타입과 메시지 타입이 일치하는지 확인한다.
        if (!channel.type.isInstance(message)) notMatchMessageType("채널 ${channel.channelName}에 맞지 않는 메시지 타입입니다.")

        val topic = redissonClient.getTopic(channel.channelName)
        topic
            .publishAsync(message)
            .thenAccept { subscriber ->
                log.info("subscriber: $subscriber")
            }
    }
}
```
코드 자체는 정말 단순하다.

# Consumer 구현

`RabbitMQ`처럼 처리할 수 있다.

```kotlin
@Component
class RedisEventSubscriber(
    private val redissonClient: RedissonClient,
    private val handlers: List<MessageHandler<*>>,
) {
    private val log = logger<RedisEventSubscriber>()

    /**
     * 해당 코드를 수정하지 않고 RedisMessageHandler를 구현한 핸들러를 추가하면 자동으로 구독할 수 있도록 한다.
     */
    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) {
            log.warn("등록된 핸들러가 없습니다. redis 리스너를 생성하지 않습니다.")
            return
        }
        handlers.forEach { handler ->
            subscriptionSetup(handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> subscriptionSetup(handler: MessageHandler<T>) {
        val channel = handler.channel
        val topic = redissonClient.getTopic(channel.channelName)
        val messageType = channel.type as Class<T>

        topic.addListener(Any::class.java) { _, message ->
            try {
                val finalMessage = convertMessage(message, messageType)
                handler.handle(finalMessage)
            } catch (e: Exception) {
                log.error("메시지 변환 또는 핸들러 실행 중 오류 발생: ${e.message}", e)
            }
        }
        log.info("Redis 채널 자동 구독 완료: ${channel.channelName}")
    }
}
```
다만 `RabbitMQ`와는 달리 `BrokerUtil`에 작성한 `convertMessage`를 활용해서 사용할 것이다.

코드 자체는 이전과 거의 흡사하다.

`MessageHandler`를 구현한 핸들러를 `SpEL`을 통해서 `List`로 `DI`를 받고 루프를 돌면서 리스너에 등록하는 방식이다.

# MessageRouter 업데이트

이제는 `Redis`를 통해서 메시지를 발행할 수 있도록 다음과 같이 수정한다.

크게 어려운 부분은 없을 것이다.

```kotlin
@Component
class MessageRouter(
    private val publishers: Map<String, MessagePublisher>,
) {
    fun <T : Any> send(
        channel: BrokerChannel,
        type: BrokerType,
        message: T,
        delayMillis: Long? = null,
    ) {
        val publisher =
            when (type) {
                BrokerType.RABBITMQ -> publishers["rabbitEventPublisher"]
                BrokerType.REDIS -> publishers["redisEventPublisher"]
                else -> null
            } ?: notSupportBrokers("지원하지 않는 브로커 타입입니다: $type")

        publisher.publish(channel, message, delayMillis)
    }
}
```

컨셉 자체가 크게 벗어나지 않기 때문에 이렇게 세팅을 완료하고 바로 테스트 단계로 들어가보자

# Pub/Sub 테스트

```kotlin
@Test
fun `REDIS를 통한 publish, consume 테스트`() {
    // When
    messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.REDIS, AlarmToBot(message = "봇으로 알람 보내기 from Redis"))
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.REDIS, AlarmToLog(message = "로그 봇으로 알람 보내기 from Redis"))
    messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.RABBITMQ, AlarmToBot(message = "봇으로 알람 보내기 from RabbitMQ"))
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, AlarmToLog(message = "로그 봇으로 알람 보내기 from RabbitMQ"))

    // then
    Thread.sleep(5000)
}
```
다음과 같이 `Redis`, `RabbitMQ`에 메시지를 발행하자.


```text
2026-02-09T18:24:08.516+09:00  INFO 45379 --- [message-brokers-server] [  redisson-3-14] i.b.g.b.c.handler.AlarmToLogHandler      : 로그로 보내는 알람 메세지 정보: AlarmToLog(message=로그 봇으로 알람 보내기 from Redis, extra=null)
2026-02-09T18:24:08.516+09:00  INFO 45379 --- [message-brokers-server] [  redisson-3-13] i.b.g.b.c.handler.AlarmToBotHandler      : 봇으로 보내는 알람 메세지 정보: AlarmToBot(message=봇으로 알람 보내기 from Redis)
2026-02-09T18:24:08.519+09:00  INFO 45379 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.c.handler.AlarmToBotHandler      : 봇으로 보내는 알람 메세지 정보: AlarmToBot(message=봇으로 알람 보내기 from RabbitMQ)
2026-02-09T18:24:08.526+09:00  INFO 45379 --- [message-brokers-server] [pool-2-thread-6] i.b.g.b.c.handler.AlarmToLogHandler      : 로그로 보내는 알람 메세지 정보: AlarmToLog(message=로그 봇으로 알람 보내기 from RabbitMQ, extra=null)
```
다른 로그는 다 지우고 유의미한 로그만 남기면 4개의 메시지가 전부 발행이 잘되고 리스너를 통해 각 `Consumer`들이 받아서 처리한 것을 볼 수 있다.

# 이외의 작업

`RabbitMQ`처럼 애노테이션을 만들어 빈으로 등록할지 말지 선택하도록 `@ConditionalOnRedis`을 작업한다.

```yaml
app:
  messaging:
    use-amqp: true
    use-redis: true
```

단 `RedissonConfig`은 캐쉬 및 분산락을 위해서 항상 빈으로 등록하도록 해당 애노테이션을 달지 않을 것이다.

# 주의 사항

스프링에서는 구동시에 자동으로 빈을 등록하는 기능이 있다.
따라서 위에 조건부 애노테이션을 달더라도 빈을 등록하다가 에러가 발생하는 경우가 있다.

이런 경우에는 서버가 실행되는 진입점에 `이것은 빈으로 등록하지 말아줘`라고 알려줘야 한다.

```kotlin
@SpringBootApplication(
    exclude = [
        org.redisson.spring.starter.RedissonAutoConfiguration::class
    ]
)
@ConfigurationPropertiesScan
class SpringWithBrokersApplication
```

# Next Step

[Kafka를 이용한 메세지 큐 브랜치]()