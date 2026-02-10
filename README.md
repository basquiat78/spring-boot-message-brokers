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

# 하지만 이 방식은 문제가 있다.

`RabbitMQ`와 달리 지금까지 완성한 코드는 여러개의 컨슈머가 있는 상황이면 모든 컨슈머가 동시에 같은 작업을 할 수 있는 구조이다.

사실 이런 이유로 `Redis`는 캐시, 분산락 같은 메모리 디비나 특정 기능에 사용하고 메시지 브로커로는 `RabbitMQ`나 `Kafka`를 사용하는 방식이 오히려 나을 수 있다.

만일 그게 아닌 그저 발행자는 발행하고 다른 모든 컨슈머가 받아서 처리해야 하는 상황이라면 이 상태로도 충분히 그 역할을 할 수 있다.

하지만 그게 아니라면 `Redis`에서도 `Streams`방식을 통해 여러 개의 컨슈머중 하나만 처리하도록 작업이 가능하다.

참고로 이 방식을 사용하기 위해서는 `Redis 5.x`버전 이상을 사용해야 한다.

# Redis Streams (Consumer Group)

기존의 방식이 아닌 `Streams`를 통해 중복 처리 방지를 해야 한다.

이 때 컨슈머 그룹을 통해 하나의 단위로 묶고 그룹내에서 한명의 컨슈머만 처리하도록 하는 방식으로 진행한다.

방식이 달리진 만큼 `Publisher`도 변경이 되야 한다.

```kotlin
@ConditionalOnRedis
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

        val stream = redissonClient.getStream<String, T>(channel.channelName)

        stream
            .addAsync(StreamAddArgs.entry("payload", message))
            .thenAccept { id ->
                log.info("Successfully Redis subscriber id: $id")
            }.exceptionally { e ->
                log.error("Failed to publish message to Redis Stream: ${e.message}")
                null
            }
    }
}
```
기존 토픽에서 `publish`는 방식에서 `redissonClient`로 부터 `Stream`을 가져와 `payload`에 메시지를 보내는 형식으로 바뀌게 된다.

당연히 리스너쪽도 변경이 되어야 한다.

그 전에 컨슈머 그룹을 만들기 위해 

```yaml
redisson:
  group-name: basquiat-consumer-group
  protocol: "redis://"
```

`yaml`파일에 그룹 명 정보를 추가하자.

```kotlin
@Component
@ConditionalOnRedis
class RedisEventSubscriber(
    private val redissonClient: RedissonClient,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
    @Value($$"${redisson.group-name:consumer-group}")
    private val groupName: String,
) {
    private val log = logger<RedisEventSubscriber>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) {
            log.warn("등록된 핸들러가 없습니다. redis 리스너를 생성하지 않습니다.")
            return
        }
        handlers.forEach { handler ->
            setupStreamSubscription(handler)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setupStreamSubscription(handler: MessageHandler<T>) {
        val streamName = handler.channel.channelName
        val targetType = handler.channel.type
        val stream = redissonClient.getStream<String, T>(streamName)

        try {
            val streamCreateGroupArgs = StreamCreateGroupArgs.name(groupName).id(StreamMessageId.ALL).makeStream()
            stream.createGroup(streamCreateGroupArgs)
        } catch (e: Exception) {
            log.error("Consumer group [$groupName] already exists for stream [$streamName] / ${e.message}")
        }

        taskExecutor.execute {
            log.info("Starting Stream polling: [${handler::class.simpleName}] on Group:[$groupName]")
            while (!Thread.currentThread().isInterrupted) {
                try {
                    // message를 가져올 때 읽지 않은 메세지를 가져오도록 처리한다.
                    val messages =
                        stream.readGroup(
                            groupName,
                            "consumer-${randomUUID()}",
                            StreamReadGroupArgs.neverDelivered().count(1).timeout(Duration.ofSeconds(5)),
                        )

                    messages.forEach { (id, content) ->
                        try {
                            val message = content["payload"] as Any
                            val data = convertMessage(message, targetType) as T
                            handler.handle(data)
                            stream.ack(groupName, id)
                        } catch (e: Exception) {
                            log.error("Handler execution failed for message $id: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    log.error("Stream polling error on [$streamName]: ${e.message}")
                    Thread.sleep(2000)
                }
            }
        }
        log.info("Successfully registered Redis Stream Consumer: [${handler::class.simpleName}]")
    }
}
```
코드를 보면 독특한 부분이 있는데 컨슈머가 직접 메시지를 읽어야 하는 `pulling`방식이라는 것을 알 수 있다.

그래서 그룹명으로 먼저 스트림즈에 그룹을 생성하고 그 그룹에 컨슈머들을 등록한다.

`pulling`방식을 효율적으로 처리하기 위해서 쓰레드방식을 사용해서 컨슈머가 메시지를 가져온다.

가져오는 부분은 `readGroup`을 보면 알겠지만 그룹내에서 읽지 않은 메시지를 가져와서 처리하는 방식으로 구성되어져 있다.

코드내에는 기존 코드를 주석처리했으니 필요에 따라 선택적으로 사용하면 될거 같다.

# Acknowledgment

소위 `ack`라는 개념을 여기서는 매뉴얼 방식으로 직접 다루고 있다.

사실 스프링에 포함됨 `amqp`라이브러리가 이것을 고수준 `API`에서 다루고 있긴 하지만 이것이 무엇인지 먼저 언급할 필요가 있다.

왜냐하면 `Redis`를 포함해 `RabbitMQ`나 개념이 다르긴 하지만 `Kafka`에도 존재하기 때문이다.

브로커 입장에서는 컨슈머가 메시지를 처리할 때 컨슈머가 메시지를 제대로 처리했는지 알 수 있는 방법이 없다.

예를 들면 컨슈머가 브로커로부터 메시지를 수신했는데 제대로 처리했는지 아니면 컨슈머가 죽었는지 모른다.

그래서 메시지를 전달하거나 읽어갔을 때 브로커에서는 해당 메시지를 바로 삭제하지 않고 일단 `pending`상태로 둔다.

또는 `unacknowledged`상태로 두고 따로 보관을 하는 방식이다.

컨슈머가 메시지를 받아서 다 처리한 이후 브로커에게 `처리 완료`라는 메세지를 주면 더 좋지 않을까?

그러면 브로커는 이 신호를 받고 삭제를 하든 일정 기간 로그에 남기든 할 것이다.

이것을 `ack`라고 한다. 일종의 약속같은 개념이다.

만일 일정 기간 `ack`가 안온다면 다른 컨슈머에게 던지거나 다른 컨슈머가 읽어갈 수 있지 않을까?

이런 방식을 통해서 다양한 방법을 고려해 보게 된다.

# at-most-once vs. at-least-once

번역하자면 `최대 한번 전달 vs. 최소 한번 전달`로 볼 수 있는데 여기에는 몇가지 차이가 있다.

`최대 한번 전달`이라는 의미를 잘 생각해 보면 브로커 입장에서는 `일단 메시지는 보냈어`라고 생각할 수 있다.
그래서 중복 메시지 전달은 없겠지만 어떤 이유로 메시지가 유실될 수 있다.

하지만 `최소 한번 전달`의 경우에는 `ack`를 통해 다시 전달할 수 있다.

이 경우에는 중복 처리가 될 수 있기 때문에 `멱등성`이 중요하다면 이것을 고려해야 한다.

매뉴얼대로 처리해 본다면 `at-most-once`의 경우에는 코드레벨에서 수신하자 마자 일단 `ack`를 날리고 후처리하는 방식을 고려해 볼 수 있다.
이 경우에는 딱히 중요하지 않는 경우에 사용할 수 있겠다.

빠르게 `ack`를 날리고 후처리하는 도중 에러가 나더라도 그냥 패스할 수 있는 경우일텐데 개인적으로 이렇게 사용하는 것을 본적이 없다.

어째든 `RabbitMQ`의 경우 스프링이 제공하는 라이브러리에서 자동으로 해주기 때문에 우리가 다루지 않았지만 실제로 매뉴얼 방식으로 처리할 수도 있다.

예를 들면 `yaml`에 설정한 정보 중에 `acknowledge-mode` 요부분이 있는데 지금은 `auto`로 되어 있지만 이것을 `manual`로 바꾸고 코드레벨에서 처리해도 된다. 

`Kafka`도 마찬가지이지만 적어도 이 부분에 대해서는 설명을 해야 할 것 같아서 따로 내용을 작성했다.

참고로 `Kafka`에서는 `ack`라고 하긴 하지만 보통 `offset commit`으로 부른다.

# Next Step

[Kafka를 이용한 메시지 큐 브랜치](https://github.com/basquiat78/spring-boot-message-brokers/tree/03-with-kafka)