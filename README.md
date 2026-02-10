# Install Kafka

여기서는 `Zookeeper`가 아닌 `KRaft`기반의 `Kafka`를 사용해 볼 생각이다.

```shell
# 실행
root>brokers>kafka> docker compose up -docker

# 내린다면
root>brokers>kafka> docker compose down -v
```

`docker-compose.yml`에는 `ui`도 설정했다.

실행이후 [kafka ui](http://localhost:8090/)

주키퍼를 사용할 때는 카프카용 `ui`을 따로 썼던 기억이 나는데 아예 같이 제공한다.

군더더기 없고 단순하며 깔끔하다.

# About Kafka

이게 최근에 대용량 트래픽 관련해서 언급되서 최근 기술로 알수 있지만 역사가 꽤 오래되었다.

2010년에 `LinkedIn`에서 내부적으로 사용하기 위해 개발된 것이고 2011년에 아파치 재단에서 등록되면서 알려진 메시지 브로커로 작게 보면 그렇다.

하지만 메시지 브로커 기능을 포함한 `분산 이벤트 스트리밍 플랫폼`이라고 보는게 맞을 것이다.

문득 이런 생각이 들것이다.

```text
RabbitMQ랑 차이가 뭐야???
```

# 파티션 개념

데이터를 토픽으로 나누는 부분까지는 비슷해 보인다.

하지만 카프카는 이 토픽을 여러 파티션에 분할을 한다.

쉽게 말하면 파티션은 로그로 순서대로 디스크에 기록이 되는 방식인데 이걸 카프카에서는 `append-only`로그라고 부른다.

그러면 파티션 단위로 기록을 하냐는 의문이 들 수 있는데 이것은 확장을 위해서이다.

클러스터를 구성한다고 하면 파티션 단위로 클러스터내의 여러 브로커에 분산이 용이하고 이것은 복제도 마찬가이다.

그래서 클러스터를 통한 수평적인 확장이 쉬워진다는 개념이다.

`Consumer`쪽에 특징이 또 있다.

`Consumer Group`이라 해서 같은 그룹내의 소비자들은 파티션 개념으로 파티션을 나눠서 읽어서 처리 가능하게 구현되었다.
이에 대한 장점은 로드밸런싱, 장애 관련 문제를 처리하는데 특화되어 있다고 한다.

여기서 `offset`이라는 개념이 있는데 `Consumer Group`은 이 `offset`을 기준으로 데이터를 읽는다.

이것은 `Consumer`측에서 어디까지 메시지를 읽고 소비했는지 알 수 있게 된다.

# 메시지 보관

`docker-compose.yml`을 보면 `retention` 부분이 눈에 띈다. 

디스크에 기록되는 데이터들은 이 정보를 기준으로 데이터를 보관하고 오래된 데이터를 삭제하는 기능을 가지고 있다.

단순하게 휘발성 메시지가 아니라는 의미인데 `Consumer`측에서 소비한 메시지를 삭제하는 구조가 아니고 로그에 기록하고 기능에 따라 삭제를 한다.

# 확장성

파티션 개념으로 인해서 수평확장에 대한 언급을 했는데 이 파티션을 늘려서 브로커를 추가하면 무중단 운영이 가능하다고 한다.

뭐 이렇게 운영을 해 본 적이 없어서 몸으로 느껴보진 않았지만 많은 사례들이나 동료들의 이야기를 보면 이게 꽤 강력한가 보다.

# 이외의 장점들

그 외에도 `Backpressure`가 가능하다.

뭐 백프레셔는 처리량을 조절하는 부분하는 부분이니 이 부분도 눈에 띄기도 한다.

위에서 `offset`과 일정 기간 로그에 기록한다고 언급했는데 이런 기능으로 `Consumer`측에서 이 `offset`를 세팅해서 기록된 과거 데이터를 다시 읽고 처리가 가능하다.

로그를 다시 분석하거나 할 때 유용하다고 하니 장점이라고 할 수 있을 것이다.

또한 에코시스템이 잘 되어 있다고 한다.

스트림 처리라든가 외부 시스템과의 연동등 장점들이 많은 것이 카프카이다!!

# 자 그래서??

사실 이렇게 보면 카프카를 단순하게 `pub/sub`을 위해서 사용하는 것은 오버스펙이다.

`RabbitMQ`나 그외 메시지 브로커들로도 충분히 커버가 가능하다는 의미이다.

하지만 최소한 카프카를 어떻게 활용해야 하는지는 이 부분에서부터 시작한다고 할 수 있다.

이를 통해서 이후 `Kafka Streams`나 실시간으로 데이터 스트림을 처리하기 위한 분산 처리 프레임워크인 아파치 `Flink`를 사용할 수 있지 않을까?

이벤트 버스로 볼 수 있는 `pub/sub`을 기반으로 다양한 기능을 활용할 수 있는 플랫폼인 만큼 이정도는 알고 이후 아이디어를 얻을 수 있을 것이라 본다.

# 그레이들 세팅

```groovy
implementation("org.springframework.boot:spring-boot-starter-kafka")
// kafka-streams
implementation("org.apache.kafka:kafka-streams")
```

사실 지금같이 그냥 메시지를 주고 받고 무언가를 처리한다면 `kafka-streams`는 불필요하다.

하지만 실시간으로 데이터를 처리해야 한다면 `Streams`방식을 고려해 봐야 한다.

일단 저것도 같이 세팅을 하자.

# 지금까지 한 방식으로 리스너 등록

카프카 역시 래빗엠큐처럼 `@KafkListener`를 제공한다.

하지만 카프카에서는 관련 애노테이션의 막강한 기능을 그대로 사용할 생각이다.

일단 다음과 같이 `yaml`에 설정을 한다.

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9094
    listener:
      # 수동 커밋 모드
      ack-mode: manual_immediate
      # 파티션 개수와 일치시켜 병렬성을 극대화
      concurrency: 3
      # 가상 스레드 환경에서는 폴링 대기 시간
      poll-timeout: 3000ms
    producer:
      # 0: 브로커가 받았는지 확인조차 안 함 / 1: 리더 브로커만 저장하면 성공 간주 / all (-1): 리더 + 모든 복제본이 저장해야 성공 간주
      acks: all
      retries: 3
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        # 요청 대기 시간
        request.timeout.ms: 30000
        # 대기 시간
        linger.ms: 10
        # 전송에 걸리는 최대 시간 제한 (요청 대기 및 대기 시간보다 크게 잡아야 함)
        delivery.timeout.ms: 120000
        # 재시도 사이의 간격
        retry.backoff.ms: 1000
        # 가상 스레드 환경에서 처리량을 높이기 위한 배치 설정
        batch.size: 32768
        # 성능과 압축률의 최적 밸런스
        compression.type: lz4
        # 타입 정보를 신뢰할 수 없을 때의 폴백 로직
        spring.json.add.type.headers: true
        # 멱등성 프로듀서 활성화 (중복 전송 방지)
        # acks를 all로 사용할 경우 묶어서 설정한다.
        enable.idempotence: true
        # 전송 순서 보장을 위한 최대 인플라이트 요청 수
        max.in.flight.requests.per.connection: 5
    consumer:
      group-id: basquiat-group
      # 오래된 메시지부터 순차적으로 읽을 것인지 (earliest)/ 됐고! 지금부터 들어오는 것을 읽을 것이지 (latest) 여부
      auto-offset-reset: earliest
      # 수동 ACK를 사용하기 위해 매뉴얼 모드 명시
      enable-auto-commit: false
      # 대량 처리시 유용, 단 이경우에는 메세지기 하나씩 오는게 아닌 리스트 형태로 오게 된다.
      # 하지만 가상 스레드 환경에서 단건으로 처리하도록 주석 처리
      # listener.type: batch
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
        # 직렬화 할때 해당 패키지의 dto는 믿을 수 있다는 것을 명시한다.
        spring.json.trusted.packages: "io.basquiat.*"
        # 타입 정보를 신뢰할 수 없을 때의 폴백 로직
        spring.json.use.type.headers: true
        # 가상 스레드 환경에서 리밸런싱 전략 최적화
        # Stop-the-world 현상을 최소화 -> 진행하고 있는 것은 그대로, 남는 것을 나눠서
        partition.assignment.strategy: org.apache.kafka.clients.consumer.CooperativeStickyAssignor
        # 가상 스레드 환경에서 세션 및 하트비트 설정
        session.timeout.ms: 45000
        max.poll.interval.ms: 300000
        # 한 번의 poll로 가져올 최대 레코드 수
        # 보통 가상 스레드 환경에서는 50에서 100개를 권장
        max.poll.records: 50
```

각각의 프로퍼티 정보에는 주석을 달아놨으니 해당 정보를 확인해 보는 것도 좋다.

```kotlin
@Configuration
class KafkaConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        configurer: ConcurrentKafkaListenerContainerFactoryConfigurer, // 스프링이 제공하는 설정 대리인
        consumerFactory: ConsumerFactory<Any, Any>,
    ): ConcurrentKafkaListenerContainerFactory<Any, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        // yaml 설정 그대로 구성한다.
        configurer.configure(factory, consumerFactory)
        factory.containerProperties.listenerTaskExecutor =
            SimpleAsyncTaskExecutor("kafka-virtual-threads-").apply {
                setVirtualThreads(true)
            }
        return factory
    }
}
```

먼저 `kafkaListenerContainerFactory`를 작성한다.

# Publisher

```kotlin
Service("kafkaEventPublisher")
class KafkaEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MessagePublisher {
    private val log = logger<KafkaEventPublisher>()

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        kafkaTemplate
            .send(channel.channelName, message)
            .thenAccept { result ->
                val metadata = result.recordMetadata
                log.info("[Kafka] 전송 성공: Topic=${metadata.topic()}, Offset=${metadata.offset()}")
            }.exceptionally { ex ->
                log.error("[Kafka] 전송 실패: ${ex.message}")
                null
            }
    }
}
```
`Redis`에서 작성한 방식과 상당히 유사하다.

이 부분은 특별한 부분이 없다.

# Consumer

```kotlin
@Component
class KafkaEventSubscriber(
    private val handlers: List<MessageHandler<*>>,
) {
    private val log = logger<KafkaEventSubscriber>()

    private val handlerMap: Map<String, MessageHandler<*>> by lazy {
        handlers.associateBy { it.channel.channelName }
    }

    companion object {
        private val TOPIC_SUFFIX_REGEX = Regex("-retry-\\d+|-dlt$")
    }

    @RetryableTopic(
        attempts = "3",
        numPartitions = "3",
        backOff = BackOff(delay = 2000, multiplier = 2.0),
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlt",
        dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
    )
    @KafkaListener(
        // SpEL을 사용하여 BrokerChannel Enum에 정의된 모든 channelName을 구독 리스트로 확보
        topics = ["#{T(io.basquiat.global.broker.common.code.BrokerChannel).values().![channelName]}"],
        groupId = $$"${spring.kafka.consumer.group-id:basquiat-group}",
    )
    @Suppress("UNCHECKED_CAST")
    fun onMessage(
        record: ConsumerRecord<String, Any>,
        ack: Acknowledgment,
    ) {
        val payload = record.value()
        val rawTopic = record.topic()

        val originalTopic = rawTopic.replace(TOPIC_SUFFIX_REGEX, "")

        handlerMap[originalTopic]?.let { handler ->
            try {
                (handler as MessageHandler<Any>).handle(payload)
                ack.acknowledge()
            } catch (e: Exception) {
                log.error("[Kafka] 핸들러 실패 [Topic: $rawTopic]: ${e.message}")
                // DLT로 보내도록
                throw e
            }
        } ?: run {
            log.warn("[Kafka] 매칭되는 핸들러 없음: $rawTopic (Original: $originalTopic)")
            ack.acknowledge()
        }
    }

    @DltHandler
    fun handleDlt(
        record: ConsumerRecord<String, Any>,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        @Header(KafkaHeaders.EXCEPTION_MESSAGE) errorMessage: String?,
    ) {
        log.error(
            """
            [DLT 인입] 최종 처리 실패
            - 원본 토픽: $topic
            - 오프셋: $offset
            - 에러 메시지: $errorMessage
            - 페이로드: ${record.value()}
            """.trimIndent(),
        )
        // TODO: 이후 어떻게 처리할 것인지
        // 1. 알림봇에 에러난 것을 푸시한다.
        // 2. 디비에 저장하고 차후 보상 로직 또는 스크립트로 처리할지 결정하자.
    }
}
```

`BrokerChanel`에 등록된 모든 채널 정보를 리스너 애노테이션을 통해 받도록 한다.

그리고 미리 메모리에 채널에 등록된 핸들러를 `Map`형식으로 받아놓고 토픽에 맞는 핸들러를 찾아서 컨슈머를 하도록 설정한다.

여기서 독특한 부분이 몇가지가 있는데 `@RetryableTopic`, `@DltHandler`부분이다.

`Kafka`는 컨슈머측에서 어떤 이유로 에러가 나거나 할때 재시도 처리를 할 수 있는데 이것을 `@RetryableTopic`을 통해 설정할 수 있다.

여기서 각각의 옵션 정보는 다음과 같다.

- attempts: 재시도
- numPartitions: 파티션 갯수
- backOff: 2초 단위로 재시도를 하는데, 재시도 할때마다 2.0를 곱해서 2 -> 4 초 로 딜레이를 주고 재시도한다.
- 이유는 빨리 요청을 하면 장애를 일으킬 수 있기 때문에 잠시 쉬어가도록
- topicSuffixingStrategy: 재시도시에는 토픽을 생성하게 되는데 이때 토픽명에 대한 전략이다.
- 이 전략은 예를 들면 alarm.to.bot-retry-0 이런 패턴으로 생성한다.
- ui에서 이 토픽을 통해 모니터링을 수월하게 할 수 있다.
- dltTopicSuffix: dlt, 일명 보관함에 쌓일때 토픽 생성시 접미사 패턴
- dltStrategy: dlt로 메시지를 보낼 때 조차 에러가 발생할 때의 전략 방식
- 현재 세팅은 극강의 메시지 유실을 하지 않도록 성공할때까지 처리하도록 설정


이 컨슈머의 전체 흐름은 다음과 같다.

1. 수신을 하다 에러가 발생
2. 총 3번의 재시도 시도 (xxx-retry-0, xxx-retry-1, xxx-retry-2)
3. 재시도를 3번 했는데도 에러가 난다면 보관함 dtl로 보낸다

# 테스트를 해보자

```kotlin
@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(locations = ["classpath:application-local.yaml"])
@Suppress("NonAsciiCharacters")
class KafkaPubSubTest
    @Autowired
    constructor(
        private val messageRouter: MessageRouter,
    ) {
        @Test
        fun `Kafka를 통한 publish, consume 테스트`() {
            // given
            val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
            val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

            // when
            messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.KAFKA, alarmToBotMessage)
            messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.KAFKA, alarmToLogMessage)

            // then: 로그를 위해 시간을 잡는다
            Thread.sleep(1000)
        }
    }
```

지금 실행하게 되면 현재 로그가 어마어마하게 뜰 것이다.

`Kafka`는 애노테이션에 정의된 정보를 보고 미리 재시도 관련 토픽과 dlt 토픽을 생성한다.

실제로는 원래 토픽인 `alarm.to.log`, `alarm.to.bot`을 보면 메시지를 받은 숫자와 받은 메시지의 사이즈 정보가 뜨는 것을 확인 할 수 있다.

# 재시도 이후 dlt 토픽으로 넘어가는 테스트

```kotlin
@Test
fun `Kafka 메시지 처리 실패 시 재시도 및 최종 DLT 이동 검증`() {
    // given: Mockito를 사용하여 해당 핸들러 호출 시 무조건 에러 발생 설정하자
    doThrow(RuntimeException("테스트용 강제 에러"))
        .`when`(alarmToLogHandler)
        .handle(any())

    val alarmToLogMessage = AlarmToLog(message = "재시도 테스트 메시지", extra = "extra data")

    // when: Kafka 채널로 메시지 전송
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.KAFKA, alarmToLogMessage)

    // then: 로그를 위해 시간을 잡는다
    Thread.sleep(10000)

    // 3번의 재시도이니 재시도 횟수 검증
    verify(alarmToLogHandler, times(3)).handle(any())
}
```
다음과 같이 에러를 강제로 발생시켜 3번의 재시도 및 최종적으로 dlt로 넘어가는지 확인해 보는 테스트를 거쳐보자.

```text
2026-02-10T18:58:49.214+09:00  INFO 50063 --- [message-brokers-server] [rver-producer-1] i.b.g.broker.kafka.KafkaEventPublisher   : [Kafka] 전송 성공: Topic=alarm.to.log, Offset=1
2026-02-10T18:58:49.244+09:00 ERROR 50063 --- [message-brokers-server] [tual-threads-10] i.b.g.broker.kafka.KafkaEventSubscriber  : [Kafka] 핸들러 실패 [Topic: alarm.to.log]: 테스트용 강제 에러
2026-02-10T18:58:49.792+09:00  INFO 50063 --- [message-brokers-server] [rtual-threads-7] o.a.k.c.c.i.ClassicKafkaConsumer         : [Consumer clientId=consumer-basquiat-group-retry-0-7, groupId=basquiat-group-retry-0] Seeking to offset 0 for partition alarm.to.log-retry-0-0
2026-02-10T18:58:49.792+09:00  INFO 50063 --- [message-brokers-server] [rtual-threads-7] o.s.k.l.KafkaMessageListenerContainer    : Record in retry and not yet recovered
2026-02-10T18:58:51.352+09:00 ERROR 50063 --- [message-brokers-server] [rtual-threads-7] i.b.g.broker.kafka.KafkaEventSubscriber  : [Kafka] 핸들러 실패 [Topic: alarm.to.log-retry-0]: 테스트용 강제 에러
2026-02-10T18:58:51.883+09:00  INFO 50063 --- [message-brokers-server] [rtual-threads-4] o.a.k.c.c.i.ClassicKafkaConsumer         : [Consumer clientId=consumer-basquiat-group-retry-1-4, groupId=basquiat-group-retry-1] Seeking to offset 0 for partition alarm.to.log-retry-1-0
2026-02-10T18:58:51.884+09:00  INFO 50063 --- [message-brokers-server] [rtual-threads-4] o.s.k.l.KafkaMessageListenerContainer    : Record in retry and not yet recovered
2026-02-10T18:58:55.428+09:00 ERROR 50063 --- [message-brokers-server] [rtual-threads-4] i.b.g.broker.kafka.KafkaEventSubscriber  : [Kafka] 핸들러 실패 [Topic: alarm.to.log-retry-1]: 테스트용 강제 에러
2026-02-10T18:58:55.430+09:00 ERROR 50063 --- [message-brokers-server] [rtual-threads-4] k.r.DeadLetterPublishingRecovererFactory : Record: topic = alarm.to.log-retry-1, partition = 0, offset = 0, main topic = alarm.to.log threw an error at topic alarm.to.log-retry-1 and won't be retried. Sending to DLT with name alarm.to.log-dlt.

org.springframework.kafka.listener.ListenerExecutionFailedException: Listener failed...

2026-02-10T18:58:55.955+09:00 ERROR 50063 --- [message-brokers-server] [rtual-threads-1] i.b.g.broker.kafka.KafkaEventSubscriber  : [DLT 인입] 최종 처리 실패
- 원본 토픽: alarm.to.log-dlt
- 오프셋: 0
- 에러 메시지: Listener failed; 테스트용 강제 에러
- 페이로드: AlarmToLog(message=재시도 테스트 메시지, extra=extra data)
```

로그를 보면 전송이 성공한 이후 처음 에러가 나고 2번의 재시도를 통해 최종 3번의 시도를 하고 결국 dlt로 빠지는 것을 볼 수 있다.

[kafka ui](http://localhost:8090/)에서도 바로 확인이 가능하다.

# Next Step

[Nats를 이용한 메시지 큐 브랜치]()