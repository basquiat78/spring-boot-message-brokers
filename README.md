# Install RabbitMQ

기존에 사용하는 `RabbitMQ`가 있다면 상관없지만 로컬에서 테스트 하고자 한다면 프로젝트 루트에 있는 brokers라는 폴더를 살펴보자.

`brokers > rabbitmq` 폴더의 `docker-compose.yml`을 실행해서 로컬에서 테스트해볼 수 있다.

참고로 `port`, `username`, `password` 정보는 개인에 맞춰서 `docker-compose.yml`를 수정하는 것을 추천한다.

당연히 수정한 정보는 스프링 부트의 `yaml`에서도 수정하는 것을 잊지 말자!!!!

```shell
# 실행
brokers>rabbitmq>docker compose up -d

# 중지
brokers>rabbitmq>docker compose down -v
```

`docker-compose.yml`을 보면 내부 포트가 `5672`로 되어 있다.
보통 회사에서 사용하기 위해서는 `TLS/SSL`, 즉 암호화 연결용으로 사용하기 때문에 `5671`을 사용하게 된다.
`Amazon MQ`를 사용한다면 `5671`일 것이다.

하지만 로컬 테스트에서는 이것을 사용하지 않을 것이기 때문에 `5672`를 사용하고 아래처럼 `ssl.enabled`를 `false`로 둔다.

```yaml
spring:
  config:
    activate:
      on-profile: local
  rabbitmq:
    host: localhost
    port: 5674
    username: basquiat
    password: basquiat
    virtual-host: "/"
    # 메시지가 브로커에 잘 도달했는지 확인
    # 도달하지 못한 메시지를 되돌려받을지 여부
    publisher-confirm-type: correlated
    publisher-returns: true
    requested-heartbeat: 30s
    connection-timeout: 10000ms
    ssl:
      enabled: false
    template:
      retry:
        enabled: true
        initial-interval: 1000ms
        max-attempts: 3
        multiplier: 2.0
    listener:
      simple:
        # 성공적인 처리를 했다면 auto ack
        acknowledge-mode: auto
        prefetch: 1
        concurrency: 2
        max-concurrency: 5
        # 에러시 재시도 정보 세팅
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 2000ms
          multiplier: 2.0
          max-interval: 10000ms
        default-requeue-rejected: false

# property로 사용하기 위한 정보 세팅
basquiat:
  rabbitmq:
    exchange-name: basquiat-exchange
    queue-name: basquiat-queue
    delay-exchange-name: basquiat-delay-exchange
    delay-queue-name: basquiat-delay-queue
    dlx-routing-key: dlx-default-routing-key
```

이미 사용중인 `RabbitMQ`가 `Amazon MQ`를 사용하고 있다면 이에 맞춰 `username`, `password`를 변경한다.

포트 역시 세팅한 정보로 변경하고 반드시 `ssl.enabled`를 `true`롤 세팅해야 한다.

`RabbitMQ`를 `docker-compose.yml`로 사용할 때 제공하는 `ui`를 사용하고 싶다면 `management`가 붙은 버전을 사용해야 한다.

[RabbitMQ UI](http://localhost:15672/)

위에 설정한 `username`, `password`로 로그인하면 된다.

# About RabbitMQ

보통 `RabbitMQ`에 대해 찾아보면 고가용성과 비동기성, 확장성 등의 장점들을 가지고있는 메시지 큐 중 하나로 소개를 한다.

`AMQP(Advanced Message Queuing Protocol)`를 사용하는 대표적인 메세지 큐인데 정의를 찾아보면 대부분 다음과 같은 내용이다.

```text
서로 다른 이기종간의 시스템들 간의 최대한 효율적인 방법으로 메세지를 교환을 목적으로 하고 있다.
메시지관리, 큐잉, 라우팅(peer to peer, pub-sub), 신뢰성, 보안 등에 대해 정의하고 있다.
```

대부분의 `Message Broker`의 특징을 크게 잡아보면 다음과 같이 크게 볼 수 있다.

- Producer
- Consumer

이런 개념은 단순하게 `RabbitMQ`에서뿐만 아니라 앞으로 진행할 다른 녀석들도 마찬가지이다.

`Producer`라는 개념은 그냥 쉽게 생각하면 메시지를 생성해서 보내는 주체이고 `Consumer`는 `Producer`가 보낸 메시지를 소비하는 주체이다.

목적을 가지고 이야기를 한다면 어떤 일을 처리하도록 특정 데이터를 메시지로 보내면 `Consumer`가 그 메시지를 받아 특정 목적에 맞게 데이터를 처리한다고 보면 된다.

보통 `pub/sub`이라고 표현하게 된다.

가장 손쉽게 사용할 수 있는 `Redis`의 경우에는 채널을 통해 이런 방식을 주고 받는다.

하지만 `RabbitMQ`는 `exchange`라는 개념이 있는데 다음과 같이 크게 나눠서 분류할 수 있다.

### Direct Exchange

이름에서 냄새가 바로 나는 부분인데 라우팅 키가 정확히 일치하는 Queue에 메시지를 전송한다.

예를 들면 `alarm.to.bot`이라는 채널에 메시지를 전송하면 이 채널을 구독하는 구독자가 메시지를 받는다.

정확하게 매핑되는 [1대1] 개념이라고 봐도 무방하다.

### Fanout Exchange

팬아웃이라고 하면 디지털 논리회로에서 논리 게이트에 연결될 수 있는 입력과 출력의 최대값을 의미한다.

이 말대로라면 이 `Exchange`에 존재하는 여러 개의 채널이 등록이 되어 있을 때 해당 채널을 구독하는 모든 소비자에게 메시지를 전송하는 방식이라고 볼 수 있다.

[1대N] 개념이라고 볼 수 있다.

사실 이걸 사용해 본적이 없는데 이걸 어디에 사용해 볼 수 있을까 생각하면 딱 하나가 떠오르긴 한다.

보통 채널 정보와는 상관없이 전체 공지같은 것을 뿌려야 하는 경우 사용하기 좋지 않을까?

일단 전체공지일테니 그냥 다 받아라 하고 말이다.

### Topic Exchange

패턴 기반으로 알려진 방식이다. 라우팅 키와 바인딩 키를 통해서 패턴 기반으로 유연하게 메시지를 전달하는 방식이다.

### Headers Exchange

실제 사용해 본적은 없고 개념적으로 알고 있는 것으로 `key:value`로 이뤄진 header를 보고 그에 일치하는 큐에 메시지를 전송하는 방식이다.

# Direct Exchange vs. Topic Exchange

`Direct Exchange`는 굉장히 단순하다.

예를 들면 `Exchange`에 바인딩 키로 `alarm.to.bot`를 큐와 등록한다고 생각하자.

`Producer는` 라우팅 키로 `alarm.to.bot`로 메시지를 보낸다.

바인딩 키와 라우팅 키가 정확하게 일치하면 된다.

장점은 하나다.

완벽하게 1대1로 매칭을 하기 때문에 직관적이다.

`Topic Exchange`은 패턴 기반으로 예를 들면 `mart.fruit`, `mart.vegetable`처럼 바인팅 키를 `.`을 통해서 게층적으로 패턴을 생성할 수 있다.

전체 마트에 대한 모든 정보를 수집한다면 `mart.#`처럼 설정할 수 있고 `mart.fruit.apple`, `mart.vegetabel.tomato`처럼 모든 계층의 하위 정보를 수집하게 된다.

필요에 따라서 `mart.fruit.*`, `mart.vegetable.*`처럼 바인딩 키를 처리할 수 있다.

`Producer`입장에서는 정확하게 `mart.fruit.appl`처럼 보내고 `Consumer`측에서는 필요에 따라 와일드 카드 `*`를 활용해서 유연하게 메시지를 수집할 수 있다.

# 여기서는 Direct Exchange

변경되는 환경, 앞으로의 요구사항이 변경되는 것을 생각해 본다면 `Topic Exchange`를 선택하는 것이 최선의 방법일 것이다.
하지만 여기서는 정말 단순한 방식인 `Direct Exchange`를 먼저 살펴보고 차후 번외편에서 `Topic Exchange`를 적용할 것이다.

# x-dead-letter-exchange

`RabbitMQ`에는 지연 메시지 기능이 있다.

`ttl`을 활용해서 바로 메시지를 보내는 것이 아닌 특정 시간이 지나면 메시지를 보낼 수 있도록 설계할 수 있다.

단지 이 방식은 그에 따른 큐를 생성하기 때문에 무작위 라우팅 키를 생성하게 되면 큐가 무한대로 생성될 수 있다.

이에 대한 몇가지 방식은 이 라우팅 키를 `Redis`에 넣어두고 `Consumer`측에서 해당 지연 메시지를 처리하면 이 키를 가지고 이후 메시지가 있는지 먼저 확인한다.
그리고 없으면 `RabbitMQ`에서 지우는 방식을 사용할 수 있다.

또는 특정 `ttl`을 단위별로 설정해서 무한으로 생성되는 것을 방지하는 방식을 고려해 볼 수 있다.
하지만 이 방식에는 단점이 있는데 `지연 큐 백로그`문제가 발생한다.

예를 들면 어떤 메시지를 다음과 같은 라우팅 키에 넣어서 지연 메시지를 보낸다고 생각해 보자.

`ttl-1000-delayed`라는 라우팅 키를 잡고 `200ms`, `500ms`를 지연시켜 메시지를 보낸다고 할 때 문제가 발생한 다는 것이다.

처음 `200ms`가 큐에 들어오고 `500ms`가 뒤에 들어온다고 하면 `500ms`는 앞에 `200ms` 지연 메시지가 처리될 때까지 머문다.

그래서 실제로 `700ms` 이후에 지연 메시지가 발생한다.

내가 원하는건 `500ms` 이후 메세지가 던져지길 기대했는데 이런 문제가 발생하게 된다.

해결법은 실제 `RabbitMQ`을 직접 설치하고 관련 플러그인을 세팅하는 방식이다.

하지만 `Amazon MQ`를 사용할 때는 플러그인을 설치할 수 없는 걸로 알고 있는데 (잘못 알고 있을 수 있다) 그래서 코드레벨에서 처리하도록 할 생각이다.

`docker-compose`를 활용한다면 직접 도커를 생성할 때 플러그인을 설치하고 구동하는 방식도 있긴 하다.

# 스프링 부트에서 활용해 보자

스프링 부트에서는 `amqp`와 관련된 라이브러리를 제공한다.

```groovy
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
```
기존 `build.gradle.kts`에 의존성을 적용하자.

# 기존 방식에서 벗어나서 직접 리스너를 등록하도록 설정하자.

스프링 부트에서는 `Consumer`를 구현하는 방식은 간단하게 리스너 애노테이션을 달아서 처리하는 것이다.

예를 들면

```kotlin
@Component
class AlarmToBotConsumer {
    @RabbitListener(queues = ["alarm.to.bot"])
    fun handle(payload: AlarmToBot) {
    }
}
```
이런 방식으로 처리한다.

스프링의 `SpEL`과 `yaml`을 통한 자동 설정으로 재시도 부분등 손대지 않고 코푸는 방식으로 사용할 수 있다.

하지만 여기서는 다른 메세지 브로커를 공통으로 사용하기 위해서 이 방식보다는 직접적으로 리스너를 통해 등록하는 방식으로 처리하고자 한다.

# 전체적인 컨셉

전체적인 컨셉은 특정 메시지 브로커에 종속되지 않고 공통으로 사용할 수 있도록 하는 것이다.

크게 보면 메세지 보로커들의 공통적인 흐름 방식은 아래와 같다.

```text
        [Producer / Publisher]
                 |
                 | 1. Producer가 메시지 전송
                 v
        +--------------------+
        |   Message Broker   |
        +--------------------+
                 |
                 | 2.Consumer가 메시지 구독
                 |  - 메시지가 전송되면 수신
                 v
        [Consumer / Subscriber]
```

정말 간단하지 않은가?

어떤 정보를 생산자가 특정 채널로 보내면 해당 채널을 구독하고 있는 소비자가 메시지가 들어올 떄 받는다는 간단하 흐름도이다.

하지만 각각의 브로커들마다 일종의 시그니쳐같은 특징들이 존재하기 때문에 실제 동적하는 방식은 비슷해 보이지만 다르다.

물론 이것은 외부의 관점에서 볼때의 흐름이다.

즉, 각 브로커별로 자신들의 방식대로 지지고 볶든 뭐를 하든 결국 [보낸다 --> broker --> 받는다]이런 흐름은 바뀌지 않는다.

그래서 어떤 브로커가 오더라도 공통 방식으로 메시지를 발행하고 수신할 수 있도록 이 컨셉에 맞춰 라우터를 먼저 만들어 볼것이다.

## MessageRouter

메시지 라우터는 파라미터 정보를 보고 어느 메시지 브로커로 보낼지 결정한다.

대단한건 아니다.

```kotlin
enum class BrokerType {
    REDIS,
    KAFKA,
    NATS,
    RABBITMQ,
}

enum class BrokerChannel(
    val channelName: String,
    val type: Class<*>,
) {
    ALARM_TO_BOT("alarm.to.bot", AlarmToBot::class.java),
    ALARM_TO_LOG("alarm.to.log", AlarmToLog::class.java),
    ;

    companion object {
        val allChannelNames: List<String>
            get() = entries.map { it.channelName }

        fun findByName(name: String) = entries.find { it.channelName == name }
    }
}

data class AlarmToBot(
    val message: String,
)

data class AlarmToLog(
    val message: String,
    val extra: String? = null,
)

interface MessagePublisher {
    fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    )
}

@Component
class MessageRouter(
    private val publishers: Map<String, MessagePublisher>,
) {
    fun <T : Any> send(
        channel: BrokerChannel,
        type: BrokerType,
        message: T,
    ) {
        val publisher =
            when (type) {
                BrokerType.RABBITMQ -> // TODO
                else -> null
            } ?: notSupportBrokers("지원하지 않는 브로커 타입입니다: $type")

        publisher.publish(channel, message, delayMillis)
    }
}
```
일단 저런 방식이 될 것이다.

별거 없지 않은가?

인터페이스 `MessagePublisher`를 보면 알겠지만 심플하게 처리하도록 하고 있다.

각 메시지 브로커가 해당 인터페이스를 구현하기만 하면 될 것이다.

`MessagePublisher`를 구현한 컴포넌트들은 스프링이 알아서 `IoC`를 할 것이다.

여러 개의 메시지 브로커가 구현한 컴포넌트들은 `SpEL`을 통해서 `DI`를 하게 되면 `Map`형식으로 받을 수 있다.

그러면 먼저 `RabbitMqConfig`를 살펴보자.

기존 애노테이션이 아닌 방식이기 때문에 설정 정보들을 수동으로 작성해야 한다.

```kotlin
// yaml에 정의한 exchange, 큐등 정의한 정보를 프로퍼티로 들고 다닐 수 있도록 설정
@ConfigurationProperties(prefix = "basquiat.rabbitmq")
data class RabbitMqProperties
    @ConstructorBinding
    constructor(
        val exchangeName: String,
        val queueName: String,
        val delayExchangeName: String,
        val delayQueueName: String,
        val dlxRoutingKey: String,
    )

@Configuration
class RabbitMqConfig(
    private val props: RabbitMqProperties,
) {
    /**
     * 재시도 정책 설정
     */
    @Bean
    fun retryInterceptor(): MethodInterceptor {
        val customPolicy =
            RetryPolicy
                .builder()
                .maxRetries(3)
                .delay(Duration.ofSeconds(2))
                .multiplier(2.0)
                .maxDelay(Duration.ofSeconds(10))
                .build()

        return RetryInterceptorBuilder
            .stateless()
            .retryPolicy(customPolicy)
            .recoverer(RejectAndDontRequeueRecoverer())
            .build()
    }

    /**
     * 문제 발생시 재시도 발행하도록 설정
     */
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryPolicy =
            RetryPolicy
                .builder()
                .maxRetries(3)
                .delay(Duration.ofSeconds(1))
                .includes(Throwable::class.java) // 모든 예외 재시도 허용
                .build()
        return RetryTemplate(retryPolicy)
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter()
        template.setRetryTemplate(retryTemplate())
        return template
    }

    /**
     * BrokerChannel enum에 정의된 모든 채널을 보고 RabbitMQ의 Exchange, Queue, Binding을 자동 생성한다.
     */
    @Bean
    fun dynamicRabbitDeclarables(): Declarables {
        val mainExchange = DirectExchange(props.exchangeName)
        val declarables = mutableListOf<Declarable>(mainExchange)

        BrokerChannel.entries.forEach { channel ->
            val queueName = channel.channelName
            val delayQueueName = "$queueName-delay"

            val queue =
                QueueBuilder
                    .durable(queueName)
                    .withArgument("x-dead-letter-exchange", "")
                    .withArgument("x-dead-letter-routing-key", delayQueueName)
                    .build()

            val binding = BindingBuilder.bind(queue).to(mainExchange).with(queueName)

            val delayQueue =
                QueueBuilder
                    .durable(delayQueueName)
                    .withArgument("x-dead-letter-exchange", props.exchangeName)
                    .withArgument("x-dead-letter-routing-key", queueName)
                    .withArgument("x-message-ttl", 30000)
                    .build()

            declarables.add(queue)
            declarables.add(binding)
            declarables.add(delayQueue)
        }
        return Declarables(declarables)
    }

    /**
     * 지연 큐 (Delayed Message Queue) 설정
     */
    @Bean
    fun delayQueue(): Queue =
        QueueBuilder
            .durable(props.delayQueueName)
            .withArgument("x-dead-letter-exchange", props.exchangeName)
            .withArgument("x-dead-letter-routing-key", props.dlxRoutingKey)
            .build()

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        val rabbitMapper = mapper.copy()
        rabbitMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val converter = Jackson2JsonMessageConverter(rabbitMapper)
        return converter
    }
}
```
먼저 재시도 및 큐채널 자동 등록을 위한 설정 그리고 지연 메시지를 사용할 수 있으니 이와 관련 빈을 등록하도록 한다.

`messageConverter`의 경우에는 `utils` 패키지에서 사용하는 `mapper`를 복사해서 사용한다.

이것은 각 메시지 브로커별로 독립적으로 사용해서 다른 브로커들과 분리를 하기 위한 방법이다.

# 지연 큐

지연 큐는 다음과 같은 방식으로 작동한다.


```text

                        [Producer]
                            |
                            |
                            v
                   +-----------------+
                   |   Delay Queue   |
                   | (TTL + DLX 설정) |
                   +-----------------+
                            |
                            | TTL 만료 → DLX로 Dead-lettering
                            v
          +-----------------------------------+
          | Dead Letter Exchange (DLX)        |
          |  - x-dead-letter-exchange         |
          |  - x-dead-letter-routing-key=Main |
          +-----------------------------------+
                            |
                            | 이 정보를 보고 바인딩 된 메인 큐로 보낸다.
                            v
                   +----------------+
                   |   Main Queue   |
                   +----------------+
                            |
                            |
                            v
                        [Consumer]
```
원래 `exchange`로 설정할 때 이것을 `x-dead-letter-exchange`로 바인딩을 한다.
그리고 지연을 위한 라우팅 키를 `x-dead-letter-routing-key`로 설정한다.

이것을 보면 특정 지연 교환소에 `TTL`을 걸고 큐를 뒀다고 만료가 되면 `exchange`를 통해 구독자에게 보내는 방식이다.

그래서 실제로 이것을 사용하게 되면 `RabbitMQ UI`에서 [Queues and Streams]탭에 보면 지연 메시지를 위한 큐가 생성된다.

먼저 메시지를 보내는 `Publisher`를 만들어 보자.

```kotlin
@Service("rabbitEventPublisher")
class RabbitMqEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val props: RabbitMqProperties,
) : MessagePublisher {
    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // 내부적으로 지연 시간 null을 넘겨서 공통 로직 처리
        this.publishWithDelay(channel, message, null)
    }

    fun <T : Any> publishWithDelay(
        channel: BrokerChannel,
        message: T,
        delayMillis: Long?,
    ) {
        val routingKey = channel.channelName
        if (delayMillis != null && delayMillis > 0) {
            // 전용 지연 큐 이름으로 전송 (ex: alarm.to.bot-delay)
            val targetDelayQueue = "$routingKey-delay"
            @Suppress("UsePropertyAccessSyntax")
            rabbitTemplate.convertAndSend("", targetDelayQueue, message) { msg ->
                msg.messageProperties.setExpiration(delayMillis.toString())
                msg
            }
        } else {
            rabbitTemplate.convertAndSend(props.exchangeName, routingKey, message)
        }
    }
}
```

기본적인 `publish`메소드 외에도 지연 메시지를 보내기 위한 메소드도 존재한다.

`publish`는 지연 메시지 메소드인 `publishWithDelay`을 통해서 보내게 되어 있다.

다른 메시지 브로커들도 지연 메시지를 보낼 수 있는 방법이 존재하긴 하지만 `RabbitMQ`와는 다르기 때문에 이 경우에는 위와 같이 처리한다.

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
                else -> null
            } ?: notSupportBrokers("지원하지 않는 브로커 타입입니다: $type")

        publisher.publish(channel, message, delayMillis)
    }
}
```
이것을 적용하면 라우터는 위와 같이 변경이 가능하다.

# Consumer 생성

```kotlin
@Component
class AlarmToBotHandler : MessageHandler<AlarmToBot> {
    private val log = logger<AlarmToBotHandler>()

    override val channel = BrokerChannel.ALARM_TO_BOT

    override fun handle(message: AlarmToBot) {
        log.info("봇으로 보내는 알람 메세지 정보: $message")
    }
}

@Component
class AlarmToLogHandler : MessageHandler<AlarmToLog> {
    private val log = logger<AlarmToLogHandler>()

    override val channel = BrokerChannel.ALARM_TO_LOG

    override fun handle(message: AlarmToLog) {
        log.info("로그로 보내는 알람 메세지 정보: $message")
    }
}
```
다음과 같이 핸들러를 만들자.

스프링부트에서 제공하는 애노테이션 대신 `channel` 정보를 세팅하고 리스너 등록시 이 정보를 보고 큐 매핑을 하게 만들도록 한다.

이 핸들러는 모든 메시지 브로커에서 처리하도록 만들 생각이다.

# Consumer Listener 생성

```kotlin
@Component
class RabbitMqListenerContainerFactory(
    private val connectionFactory: ConnectionFactory,
    private val retryInterceptor: MethodInterceptor,
) {
    private val log = logger<RabbitMqListenerContainerFactory>()

    /**
     * 리스너 컨테이너 팩토리를 커스터마이즈로 생성하자.
     */
    fun createContainer(
        queueName: String,
        messageListener: MessageListener,
    ): DirectMessageListenerContainer =
        DirectMessageListenerContainer(connectionFactory).apply {
            setQueueNames(queueName)
            setAdviceChain(retryInterceptor)
            setConsumersPerQueue(1)
            setMessageListener(messageListener)
        }
}

@Component
class RabbitMqEventSubscriber(
    private val containerFactory: RabbitMqListenerContainerFactory,
    private val handlers: List<MessageHandler<*>>,
    private val messageConverter: Jackson2JsonMessageConverter,
) {
    private val log = logger<RabbitMqEventSubscriber>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) return

        handlers.forEach { handler ->
            val queueName = handler.channel.channelName
            val listener = createMessageListener(handler)
            containerFactory.createContainer(queueName, listener).start()
            log.info("Successfully RabbitMQ subscribed: [${handler::class.simpleName}] -> Queue:[$queueName]")
        }
    }

    private fun createMessageListener(handler: MessageHandler<*>): ChannelAwareMessageListener {
        return ChannelAwareMessageListener { message, _ ->
            try {
                val data = messageConverter.fromMessage(message)
                if (data == null) {
                    log.warn("Converted data is null for message: $message")
                    return@ChannelAwareMessageListener
                }
                executeHandler(handler, data)
            } catch (e: Exception) {
                log.error("RabbitMQ 리스너 처리 중 에러 발생: ${e.message}", e)
                throw e
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun executeHandler(
        handler: MessageHandler<*>,
        data: Any,
    ) {
        try {
            (handler as MessageHandler<Any>).handle(data)
        } catch (e: Exception) {
            handleFailureLog(handler, data)
            throw e
        }
    }

    private fun handleFailureLog(
        handler: MessageHandler<*>,
        data: Any,
    ) {
        log.error("[DEAD LETTER] 처리 실패. Queue: ${handler.channel.channelName} / message: $data")
    }
}
```
아무래도 스프링부트의 자동 설정 기능을 사용하는 것이 아니기 때문에 앞서 만든 `RabbitMQConfig`에서 작성한 빈들을 실제 `ConnectionFactory`에 적용하도록 `RabbitMqListenerContainerFactory`라는 커스텀 팩토리를 하나 만든다.

코드를 보면 뭔가 엄청 복잡해 보이지만 사실은 단순한 형태이다.

```text
1. 스프링 부트를 통해 등록된 핸들러 배열을 순회한다.
2. 핸들러를 순회하면서 각 핸들러를 리스너에 등록한다.
```

## Pub/Sub 테스트

```kotlin
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class RabbitMqPubSubTest
    @Autowired
    constructor(
        private val connectionFactory: ConnectionFactory,
        private val messageRouter: MessageRouter,
    ) {
        @Test
        fun `RabbitMQ 연결 확인`() {
            connectionFactory.createConnection().use { conn ->
                println("Connected: ${conn.localPort}")
            }
        }

        @Test
        fun `RabbitMQ pub-sub 테스트`() {
            // given
            val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")
            
            // when
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.RABBITMQ, alarmToBotMessage)
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(5000)
        }
    }
```
위 코드를 테스트 해보자.

```text
2026-02-09T13:57:30.058+09:00  INFO 21414 --- [message-brokers-server] [pool-2-thread-6] i.b.g.b.c.handler.AlarmToBotHandler      : 봇으로 보내는 알람 메세지 정보: AlarmToBot(message=봇으로 알람 보내기)
2026-02-09T13:57:30.063+09:00  INFO 21414 --- [message-brokers-server] [pool-2-thread-7] i.b.g.b.c.handler.AlarmToLogHandler      : 로그로 보내는 알람 메세지 정보: AlarmToLog(message=로그 봇으로 알람 보내기, extra=extra data)
```
많은 로그가 뜨겠지만 딱 핸들러내에 로그를 찍는 부분을 확인하면 된다.

## 재시도 여부 확인

`RabbitMqConfig`, `RabbitMqListenerContainerFactory`를 통해서 설정한 재시도 관련 테스트를 해보자.

```kotlin
@Bean
fun retryInterceptor(): MethodInterceptor {
    val customPolicy =
        RetryPolicy
            .builder()
            .maxRetries(3)
            .delay(Duration.ofSeconds(2))
            .multiplier(2.0)
            .maxDelay(Duration.ofSeconds(10))
            .build()
```
그러면 실제로 3번의 재시도를 하는지 확인해 보는 테스트 코드를 확인해 보자.

```kotlin
@MockitoSpyBean
lateinit var alarmToLogHandler: AlarmToLogHandler

@Test
fun `메시지 처리 실패 시 재시도 로직 검증`() {
    // given: 메소드 실행시 에러 발생하도록 mockito 설정
    doThrow(RuntimeException("에러 발생 - 리트라이 테스트"))
        .`when`(alarmToLogHandler)
        .handle(any())

    val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

    // when
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage)

    // then: 로그를 위해 시간을 잡는다
    Thread.sleep(10000)

    // 구성 파일에서 재시도 3번을 세팅했기 때문에 3번은 실행이 되야 한다.
    verify(alarmToLogHandler, atLeast(3)).handle(any())
}
```

실제로 실행을 해보면 `mokito`설정에 의해 무조건 에러를 던질 것이다.

이 상황에서 로그에서 3번의 재시도 로그가 남는지 확인해 보면 된다.


```text
2026-02-09T14:03:36.065+09:00 DEBUG 26416 --- [message-brokers-server] [ 127.0.0.1:5674] o.s.a.r.c.PublisherCallbackChannelImpl   : PublisherCallbackChannelImpl: AMQChannel(amqp://basquiat@127.0.0.1:5674/,3) PC:Ack:1:false
2026-02-09T14:03:36.083+09:00  INFO 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Processing Message from [alarm.to.log]
2026-02-09T14:03:36.093+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : [DEAD LETTER] retry failed. Queue: alarm.to.log / message: AlarmToLog(message=로그 봇으로 알람 보내기, extra=extra data)
2026-02-09T14:03:36.093+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Message handling failed: 에러 발생 - 리트라이 테스트
2026-02-09T14:03:38.097+09:00  INFO 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Processing Message from [alarm.to.log]
2026-02-09T14:03:38.099+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : [DEAD LETTER] retry failed. Queue: alarm.to.log / message: AlarmToLog(message=로그 봇으로 알람 보내기, extra=extra data)
2026-02-09T14:03:38.099+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Message handling failed: 에러 발생 - 리트라이 테스트
2026-02-09T14:03:42.107+09:00  INFO 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Processing Message from [alarm.to.log]
2026-02-09T14:03:42.110+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : [DEAD LETTER] retry failed. Queue: alarm.to.log / message: AlarmToLog(message=로그 봇으로 알람 보내기, extra=extra data)
2026-02-09T14:03:42.111+09:00 ERROR 26416 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Message handling failed: 에러 발생 - 리트라이 테스트
2026-02-09T14:03:46.092+09:00 DEBUG 26416 --- [message-brokers-server] [ionShutdownHook] o.s.a.r.c.CachingConnectionFactory       : Closing cached Channel: PublisherCallbackChannelImpl: AMQChannel(amqp://basquiat@127.0.0.1:5674/,3)
```
로그를 보면 3번까지 재시도를 하고 이후 종료하는 것을 확인할 수 있다.

# 지연 메시지

지연 메세지를 보내기 위해서 `MessagePublisher`에 다음과 같이 확장 함수를 하나 설정을 해보자

```kotlin
/**
 * RabbitMQ의 경우에는 delay 기능을 추가할 수 있다.
 * 따라서 기존 코드를 건드리지 않고 처리하기 위해서 코틀린 확장 함수를 이용한다.
 */
fun <T : Any> MessagePublisher.publish(
    channel: BrokerChannel,
    message: T,
    delayMillis: Long?,
) {
    when (this) {
        is RabbitMqEventPublisher -> this.publishWithDelay(channel, message, delayMillis)
        else -> this.publish(channel, message)
    }
}
```
다음과 같이 딜레이 시간을 주고 테스트를 한다.

```kotlin
@Test
fun `RabbitMQ 지연 발송 테스트`() {
    // given: 5초 지연 설정
    val delayMillis = 5000L
    val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

    // when
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.RABBITMQ, alarmToLogMessage, delayMillis)

    // then: 로그를 위해 시간을 잡는다
    Thread.sleep(100000)
    println("지연이후 로그 부분")
}
```

메시지를 보내고 5초후에 핸들러에서 로그가 남는다면 지연 발송 테스트가 완료된다.


```text
2026-02-09T15:30:19.709+09:00 DEBUG 98747 --- [message-brokers-server] [    Test worker] o.s.amqp.rabbit.core.RabbitTemplate      : Publishing message [(Body:'[B@53466b67(byte[79])' MessageProperties [headers={__TypeId__=io.basquiat.global.broker.common.handler.AlarmToLog}, contentType=application/json, contentEncoding=UTF-8, contentLength=79, deliveryMode=PERSISTENT, expiration=5000, priority=0, deliveryTag=0])] on exchange [], routingKey = [alarm.to.log-delay]
2026-02-09T15:30:19.715+09:00 DEBUG 98747 --- [message-brokers-server] [ 127.0.0.1:5674] o.s.a.r.c.PublisherCallbackChannelImpl   : PublisherCallbackChannelImpl: AMQChannel(amqp://basquiat@127.0.0.1:5674/,3) PC:Ack:1:false
2026-02-09T15:30:24.727+09:00 DEBUG 98747 --- [message-brokers-server] [pool-2-thread-5] o.s.a.r.l.DirectMessageListenerContainer : SimpleConsumer [queue=alarm.to.log, index=0, consumerTag=amq.ctag-lGkDmYPzcXgIG8yuF4ttLA identity=7ce35d57] received (Body:'[B@5a803330(byte[79])' MessageProperties [headers={x-first-death-exchange=, x-last-death-reason=expired, x-death=[{reason=expired, original-expiration=5000, count=1, exchange=, time=Mon Feb 09 15:30:24 KST 2026, routing-keys=[alarm.to.log-delay], queue=alarm.to.log-delay}], x-first-death-reason=expired, x-first-death-queue=alarm.to.log-delay, x-last-death-queue=alarm.to.log-delay, x-last-death-exchange=, __TypeId__=io.basquiat.global.broker.common.handler.AlarmToLog}, contentType=application/json, contentEncoding=UTF-8, contentLength=0, receivedDeliveryMode=PERSISTENT, priority=0, redelivered=false, receivedExchange=basquiat-exchange, receivedRoutingKey=alarm.to.log, deliveryTag=1, consumerTag=amq.ctag-lGkDmYPzcXgIG8yuF4ttLA, consumerQueue=alarm.to.log])
2026-02-09T15:30:24.764+09:00  INFO 98747 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.r.RabbitMqEventSubscriber        : Processing Message from [alarm.to.log]
2026-02-09T15:30:24.765+09:00  INFO 98747 --- [message-brokers-server] [pool-2-thread-5] i.b.g.b.c.handler.AlarmToLogHandler      : 로그로 보내는 알람 메세지 정보: AlarmToLog(message=로그 봇으로 알람 보내기, extra=extra data)
지연이후 로그 부분
```
로그를 보면 `2026-02-09T15:30:19`에 메세지가 발행되고 `2026-02-09T15:30:24`를 보면 5초후에 수신 한것을 확인할 수 있다.

# 이외의 초기 작업

하나의 서버에서 많은 메시지 브로커를 사용할 수 있겠지만 다른 메시지 브로커를 사용하지 않는 경우도 생길 수 있다.

이것을 `yaml`에서 설정할 수 있도록 `ConditionalOnProperty`를 활용해 볼 생각이다.

```kotlin
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ConditionalOnProperty(name = ["app.messaging.use-amqp"], havingValue = "true")
annotation class ConditionalOnAmqp
```

단순한 애노테이션이다.

`@ConditionalOnProperty` 이것은 특정 설정 값에 따라 해당 빈으로 등록할지를 결정하는 애노테이션이다.

이렇게 만드는 이유는 저 긴 옵션을 공통으로 만들어서 쉽게 사용하기 위한 방법으로 잘 알려진 방식이다.

이제는 `yaml`에 다음과 같이 설정 옵션을 두자.

이것은 상위 `application.yaml`에 설정하자.

물론 환경별로 각기 다르게 사용하겠다면 각각의 환경별 `yaml`에 따로 설정해도 상관없다.


```yaml
app:
  messaging:
    use-amqp: true
```

`RabbitMQ`와 관련된 클래스에 해당 애노테이션을 붙이면 된다.

위 설정값의 `true`, `false`에 따라 로그에 보면 관련 빈이 등록되는지 아닌지를 확인할 수 있다.

# Next Step

[Redis를 이용한 메세지 큐 브랜치](https://github.com/basquiat78/spring-boot-message-brokers/tree/02-with-redis)