# Install Nats

처음 써보는 메시지 브로커라 [NATS Docs](https://docs.nats.io/)에서 어떤 녀석인지 살펴봣다.

고랭으로 작성되어 있고 강력하고 단순하게 극강의 메시지 브로커라는 느낌이 좀 강하다.

일단 도커 컴포즈로 똑같지 않을까 해서 찾아본 정보는 [How to set up nats with docker and docker compose with token](https://dev.to/shaikhalamin/how-to-set-up-nats-with-docker-and-docker-compose-with-token-2ddb) 이것을 참조했다.

먼저 여기서 알려주는 방식은 `nats`에 대한 구성 파일을 따로 작성해서 구동하는 방식이다.

이게 전부일까 해서 `AI`를 비롯해서 여기저기 서칭해서 찾아낸 환경 구성 파일을 작성했다.

```text
# 실행
root>brokers>nats> docker compose up -docker

# 내린다면
root>brokers>nats> docker compose down -v
```

`nats.setup.conf`에 최대한 찾아보고 얻은 정보를 전부 셋업해서 주석으로 남겨둔다.

이것도 `Redis`, `Kafka`처럼 클러스터 구성이 가능하다.

# About NATS

공식 홈을 보면 고성능, 경량, 오픈소스의 메시지 시스템으로 설계되어 있다고 못을 박아두고 있다.

그래서 지금까지 진행해오면서 경험한 다른 메시지 브로커들에서 보이는 개념들을 공유하고 있다.

다만 `NATS`에는 눈여겨 볼 부분이 있다.

# nats-core

앞서 `Redis`파트에서 `At-most-once`에 대해 언급한 적이 있다.

`최대 한번 전달`이라는 개념은 `일단 메시지는 보냈어`이다.

이것은 컨슈머가 없거나 때마침 그때 컨슈머가 죽었다면 메시지를 폐기한다.

그래서 `RabbitMQ`나 `Kafka`에서는 다시 컨슈머가 살거나 붙었을 때 `At-least-once` 방식으로 처리한다.

물론 `Redis`파트에서도 `Streams`방식으로 이것을 구현할 수 있다.

`At-most-once`는 일단 보내고 컨슈머가 처리를 하든 메시지를 처리할 컨슈머가 없든 메시지를 폐기한다.

일명 `fire-and-forget`이다.

장점은 뭘까?

속도일 것이다.

하지만 프로덕트 레벨에서 메시지 유실을 감수하는 경우가 있을까?

사실 이런 경우로 사용하는 케이스를 거의 보지 못해서 언급하기 어렵긴 하지만 정말 이런 경우가 생긴다면 경량으로 사용할 수 있다.

말 그대로 가장 간단한 방식이다.

# jetStream

아마도 실서버에서는 `jetStream`을 활용할 가능성이 가장 크다고 본다.

크게 `Stream`과 `Consumer`라는 두 가지의 개념으로 나눠진다.


## Stream

`nats-core`는 메시지를 저장하거나 하지 않는다.

하지만 여기서는 `Stream`이라는 저장소가 존재하며 토픽이라는 개념을 여기서는 `Subject`로 표현하는 거 같다.

뭐 `주제`라고 하는데 여기서는 `.`을 활용해 계층적으로 설정이 가능하다.

각 브로커마다 표현하는 개념은 달라도 `RabbitMQ`의 `Topic Exchange`와 상당히 유사한 부분이 있다.

`Stream`을 저장소라고 표현했는데 그래서 서버가 죽거나 재시작할 때 옵션을 통해서 디스크에 저장을 하고 다시 불러 올 수 있기도 한다.
당연히 이런 방식은 메시지를 보관하는 정책들도 같이 따라올 것이다.

`Redis`에도 이런 옵션들이 있다.

## Consumer

[NATS.io - Consumer Details](https://docs.nats.io/using-nats/developer/develop_jetstream/consumers)

그렇다면 결국 `jetStream`은 여기 `Stream`에 메시지를 저장하고 컨슈머가 읽도록 하는 방식인데 `NATS`에서 말하는 `Consumer`는 약간 짬뽕느낌이 난다.

1. Durable Consumer
    어디까지 메시지를 읽었는지 서버에 저장을 하고 컨슈머가 이를 보고 이어서 읽을 수 있도록 한다.
    -> `kafka`의 `offset`이랑 동일한 개념이라고 봐도 무방하지 않을까?

2. Ephemeral Consumer
    생소하긴 한데 임시적인 컨슈머라고 해야 하나?
    일단 공홈의 설명대로라면 단일 인스턴스에 적합하며 컨슈머 클라이언트에 의해 자동으로 생성되고 연결이 끊기면 사라진다고 한다.

## PUSH or PULL?

`Redis`에서 스트림방식으로 처리한 것을 생각해 보면 `createGroup`과 컨슈머측에서 `readGroup`을 통해서 메시지를 가져오는 방식이었다.

일종에 `Pull`방식이라는 것이다.

일반적으로 우리가 생각하는 메시지 브로커는 대부분 `Push`방식이다.

특히 실시간 데이터를 처리한다면 `Push`방식이 좋지만 `Backpressure`를 고민하지 않을 수 없다.

예를 들면 메시지가 1000건씩 들어오는데 컨슈머가 900건만 처라할 수 있다면 100건을 처리하지 못하는 현상이 발생한다.

그래서 `Backpressure` 전략을 고민해야 한다.

`Kafka`는 아키텍쳐가 컨슈머가 플랫폼으로부터 데이터를 직접 댕겨오는 구조이다.

컨슈머 클라이언트가 처리할 수 있는 만큼 땡겨와서 처리하면 `offset`관리 차원에서도 유연하다.

`NATS`는 이 두가지 방식을 지원한다.

만일 실시간으로 빠르게 처리를 해야 한다면 `Push`방식을 안정적인 처리가 필요하다면 `Pull` 방식으로 가져가면 좋다.

다건의 데이터를 안정적으로 처리하는게 더 중요하게 생각하는 경향이 있기 때문에 많은 곳에서 `Pull` 방식을 선호하는 걸로 알고 있다!

게다가 자바의 경우에는 최신 버전에서 `Virtual Thread`, 가상 스레드를 적극적으로 활용해서 이 방식으로 극대화 할 수 있는 것도 영향을 주지 않을까?

# 일단 Pub/Sub

그레이들의 다음과 같이 세팅을 한다.

```groovy
 implementation("io.nats:jnats:2.25.1")
```

스프링에서는 자체 지원이 없기 때문에 `NATS`와 통신하는 빈을 직접 만들어야 한다.

```yaml
nats:
  server: nats://localhost:4222
  stream-name: BASQUIAT_STREAM
  max-delivery: 3
```

`yaml`에 설정을 추가한다.

상용 서버를 염두해 두고 `NATS`관련 에러 모니터링을 위한 리스너를 먼저 설정해 보자.

```kotlin
class NatsErrorListener : ErrorListener {
    private val log = logger<NatsErrorListener>()

    /**
     * 서버 오류 발생 시 호출된다.
     */
    override fun errorOccurred(
        conn: Connection?,
        error: String?,
    ) {
        log.error("Error Occurred: $error")
    }

    /**
     * 컨슈머의 처리 속도가 느려진다면 이 부분에 로그가 찍힌다.
     */
    override fun slowConsumerDetected(
        conn: Connection?,
        consumer: Consumer?,
    ) {
        val subject =
            if (consumer is Subscription) {
                consumer.subject
            } else {
                "Unknown Subject"
            }
        log.warn(
            """
            Slow Consumer Detected:
            - Subject: $subject
            - Pending Messages: ${consumer?.pendingMessageCount} / ${consumer?.pendingMessageLimit}
            - Pending Bytes: ${consumer?.pendingByteCount} / ${consumer?.pendingByteLimit}
            - Dropped Messages: ${consumer?.droppedCount}
            """.trimIndent(),
        )
    }

    /**
     * 클라이언트 컨슈머 장애 발생시
     */
    override fun exceptionOccurred(
        conn: Connection?,
        e: Exception?,
    ) {
        log.error("NATS 클라이언트 예외 발생: ${e?.message}", e)
    }

    /**
     * jetStream을 위한 heartbeat 에러
     */
    override fun heartbeatAlarm(
        conn: Connection?,
        jetStreamSubscription: JetStreamSubscription?,
        lastStreamSeq: Long,
        lastConsumerSeq: Long,
    ) {
        log.warn("jetStream Heartbeat Alarm: check this Subject: ${jetStreamSubscription?.subject}")
    }
}
```

```kotlin
@ConfigurationProperties(prefix = "nats")
data class NatsProperties
@ConstructorBinding
constructor(
    val server: String,
    val streamName: String,
)

@Configuration
class NatsConfig(
    private val props: NatsProperties,
) {
    private val log = logger<NatsConfig>()

    @Bean
    fun natsConnection(): Connection {
        val options =
            Options
                .Builder()
                .server(props.server)
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .connectionListener { _, event -> log.info("NATS 연결 상태 변경: $event") }
                .errorListener(NatsErrorListener())
                .build()

        return Nats.connect(options).also {
            setupJetStream(it)
        }
    }

    private fun setupJetStream(connection: Connection) {
        val jsm = connection.jetStreamManagement()

        val subjects = BrokerChannel.allChannelNames
        val streamName = props.streamName

        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects(subjects)
                .storageType(StorageType.File)
                //.replicas(3)
                .maxAge(Duration.ofDays(7))
                .maxMessages(100_000)
                .maxBytes(1024 * 1024 * 1024)
                .build()

        try {
            val streamNames = jsm.streamNames
            if (!streamNames.contains(streamName)) {
                jsm.addStream(streamConfig)
                log.info("successfully NATS jetStream created: $streamName")
            } else {
                jsm.updateStream(streamConfig)
                log.info("NATS jetStream config updated: $streamName")
            }
        } catch (e: Exception) {
            log.error("jetStream 설정 중 오류 발생: ${e.message}")
        }
    }
}
```

`setupJetStream`부분 코드를 보면 기존의 메시지 브로커의 경우에는 컨슈머측에서 채널별 (`NATS`에서는 `subject`)로 등록을 한다.

하지만 여기서는 생성시에 주제를 미리 등록하는 방식이다.

이것은 `jetStream`의 특징인데 `Stream`이라는 것을 저장소라고 표현을 했다.

메시지 저장소의 역할을 하는데 미리 `subject`를 생성하지 않으면 메시지 발행시 컨슈머가 없다면 메시지를 폐기하게 된다.

결국 위 코드는 다음과 같이 이해하면 된다.

```text
+--------- jetStream (BASQUIAT_STREAM) --------+
|   - subject: alarm.to.bog                    | 
|   - subject: alarm.to.log                    | 
+----------------------------------------------+
```
이렇게 `BASQUIAT_STREAM`이라는 이름을 준 독립적인 `jetStream`의 메시지 보관소에 각 `subject`를 미리 생성해서 관리한다고 보면 된다.

`replicas`는 클러스터 복제본 수를 세팅하는 부분이다.
로컬에서는 한대만 띄우기 때문에 주석처리한다.

또한 스트림에 메시지 보관 정책을 설정할 수 있다.

`Kafka`처럼 일단 7일이 지나면 지우도록 설정 (`maxAge`)하고 메시지가 10만개가 넘어가면 오래된 메시지부터 삭제하도록 (`maxMessages`)한다.
용량 기준으로 볼때 1GB가 넘어가면 삭제하도록 (`maxBytes`)를 일단 설정한다.

# Producer

```kotlin
@Service("natsEventPublisher")
class NatsEventPublisher(
    private val natsConnection: Connection,
    private val props: NatsProperties,
) : MessagePublisher {
    private val log = logger<NatsEventPublisher>()

    private val js: JetStream by lazy { natsConnection.jetStream() }

    override fun <T : Any> publish(
        channel: BrokerChannel,
        message: T,
    ) {
        // 내부적으로 ttl을 null을 넘겨서 공통 로직 처리
        this.publishWithTtl(channel, message, null)
    }

    fun <T : Any> publishWithTtl(
        channel: BrokerChannel,
        message: T,
        millis: Long?,
    ) {
        val jsonBytes = objectToByte(message)
        val subject = channel.channelName

        val pubOptionsBuilder =
            PublishOptions
                .builder()
                .expectedStream(props.streamName)
                .messageId(UUID.randomUUID().toString().substring(0, 8))

        if (millis != null) pubOptionsBuilder.messageTtlSeconds(millis.toInt())

        val pubOptions = pubOptionsBuilder.build()

        js
            .publishAsync(subject, jsonBytes, pubOptions)
            .thenAccept { ack ->
                if (ack.isDuplicate) log.info("[NATS 발행 완료] 채널: $subject, Stream: ${ack.stream}, Seq: ${ack.seqno}")
            }.exceptionally { ex ->
                log.error("[NATS 발행 실패] 채널: $subject, 사유: ${ex.message}", ex)
                null
            }
    }
}
```
발행자 코드에서 확인해 볼 만한 부분은 `expectedStream`이다.

`jetStream`에서 구성에 따라서 여러개의 스트림, 즉 메시지 저장소가 존재할 수 있을텐데 메시지 저장소를 지정하는 부분이다. 

`messageId`는 중복 메시지를 방지하기 위해서이다.

하단에 `thenAccept`부분에 `ack.isDuplicate`라는 부분이 있는데 이미 보낸 메시지인데 `messageId`로 이미 존재한다면 로그를 통해 알려주도록 처리한다.

`messageTtlSeconds`는 기본적으로 `messageTtl`의 확장 함수이다.

이것은 위에서 우리가 `NatsConfig`를 생성할 때 `jetStream`의 설정값에서 유추해 볼 수 있다.

만일 어떤 인증 정보를 메시지로 보낸다고 생각해 보자.

`본인인증`을 예로 들면 인증 이후 특정 시간안에 인증을 하도록 한다.

그리고 시간이 지나면 다시 하라고 요청을 할 것이다.

이런 경우에는 각 발행한 메시지별로 `TTL`을 설정하는 것이다.

기본적으로 7일이지만 여기에 이 정보를 세팅하게 된다면 그 시간만큼만 저장소에 머물었다가 삭제하게 된다.

이와 관련 수정된 클래스인 `MessageRouter`, `MessagePublisher` 부분을 확인하자.

# Consumer

```kotlin
@Component
class NatsEventSubscriber(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val handlers: List<MessageHandler<*>>,
) {
    private val log = logger<NatsEventSubscriber>()

    @PostConstruct
    @Suppress("UNCHECKED_CAST")
    fun init() {
        if (handlers.isEmpty()) return
        val js = natsConnection.jetStream()

        val dispatcher =
            natsConnection.createDispatcher { msg ->
                log.info("successfully create dispatcher: $msg")
            }

        handlers.forEach { handler ->
            val channel = handler.channel
            val subject = channel.channelName
            val targetType = channel.type

            val natsMessageHandler =
                NatsMessageHandler { msg: Message ->
                    try {
                        val rawData = byteToObject(msg.data, Any::class.java)
                        val finalMessage = convertMessage(rawData, targetType)
                        (handler as MessageHandler<Any>).handle(finalMessage)
                        msg.ack()
                    } catch (e: Exception) {
                        // 재시도한 횟수 가져온다.
                        val deliveredCount = msg.metaData().deliveredCount()

                        log.error("NATS 처리 실패 [$deliveredCount/3] [Subject: $subject]: ${e.message}")
                        if (deliveredCount >= props.maxDelivery) {
                            log.error("최대 재시도 횟수 초과. Subject: $subject")
                            // TODO: DB에 남기거나 알림을 줘서 모니터링 할수 있도록 한다.
                            // term()을 호출해서 해당 메시지는 재전송 그만하고 끝내도록 알려준다.
                            msg.term()
                        } else {
                            // 아직 횟수에 도달하지 않았다면 다시 보내달라고 요청
                            msg.nak()
                        }
                    }
                }

            val instanceId = UUID.randomUUID().toString().substring(0, 8)
            val uniqueDurable = "${subject.replace(".", "_")}_$instanceId"

            val consumerConfiguration =
                ConsumerConfiguration
                    .builder()
                    .maxDeliver(props.maxDelivery)
                    .ackWait(Duration.ofSeconds(30))
                    .build()

            val options =
                PushSubscribeOptions
                    .builder()
                    .durable(uniqueDurable)
                    .configuration(consumerConfiguration)
                    .build()

            js.subscribe(
                subject,
                dispatcher,
                natsMessageHandler,
                false,
                options,
            )
            log.info("successfully NATS Fan-out subscriber: $subject (Durable: $uniqueDurable)")
        }
    }
}
```
코드를 보면 이게 무엇인가 할 것이다.

일단 `jetStream`에서 `durable`은 `Consumer Group`처럼 보이게 한다.

`jetStream`입장에서는 같은 `subject`를 구독하고 있는 컨슈머들이거 같은데 `durable`이 같다면 같은 `Consumer Group`으로 본다.
이렇게 되면 `at-least-once`처럼 작동한다.

따라서 여러 컨슈머들이 동시에 처리하는게 아니라 먼저 컨슈머들이 순차적으로 들어온 메시지를 처리하기 때문에 중복 작업을 하지 않는다.

하지만 같은 `subject`를 구독하지만 이 값이 다르면 서로 독립된 `Consumer Group`이라고 인식하게 된다.

`RabbitMQ`의 `Fanout Exchange`처럼 작동하게 되어 모든 컨슈머에게 메시지를 전부 발행하게 된다.

여기까지가 가장 기본적인 `pub/sub` 로직이다.

# 테스트

```kotlin
@Test
fun `NATS를 통한 fan-out, push 방식의 publish, consume 테스트`() {
    // given
    val alarmToBotMessage = AlarmToBot(message = "봇으로 알람 보내기")
    val alarmToLogMessage = AlarmToLog(message = "로그 봇으로 알람 보내기", extra = "extra data")

    // when
    messageRouter.send(BrokerChannel.ALARM_TO_BOT, BrokerType.NATS, alarmToBotMessage)
    messageRouter.send(BrokerChannel.ALARM_TO_LOG, BrokerType.NATS, alarmToLogMessage)

    // then: 로그를 위해 시간을 잡는다
    Thread.sleep(1000)
}
```
메시지를 전송하고 수신하는 로그를 확인해 보면 잘 가는 것을 볼 수 있다.

# PUSH에서 PULL 방식으로

코드를 보면 마지막 구독을 하는 코드에 넘기는 `options`정보를 보면



# Next Step

[Nats를 이용한 메시지 큐 브랜치](https://github.com/basquiat78/spring-boot-message-brokers/tree/04-with-nats)