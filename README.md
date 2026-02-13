# Install Nats

처음 써보는 메시지 브로커라 [NATS.io - NATS Docs](https://docs.nats.io/)에서 어떤 녀석인지 살펴봣다.

고랭으로 작성되어 있고 강력하고 단순하게 극강의 메시지 브로커라는 느낌이 좀 강하다.

일단 도커 컴포즈로 똑같지 않을까 해서 찾아본 정보는 [How to set up nats with docker and docker compose with token](https://dev.to/shaikhalamin/how-to-set-up-nats-with-docker-and-docker-compose-with-token-2ddb) 이것을 참조했다.

먼저 여기서 알려주는 방식은 `NATS`에 대한 구성 파일을 따로 작성해서 구동하는 방식이다.

이게 전부일까 해서 `AI`를 비롯해서 여기저기 서칭해서 찾아낸 환경 구성 파일을 작성했다.

```text
# 실행
root>brokers>nats> docker compose up -docker

# 내린다면
root>brokers>nats> docker compose down -v
```

`nats.conf`에 최대한 찾아보고 얻은 정보를 전부 셋업해서 주석으로 남겨둔다.

이것도 `Redis`, `Kafka`처럼 클러스터 구성이 가능하다.

[NATS UI - Nui](http://localhost:31311/)

도커 컴포즈를 실행하면 `Nui`가 실행이 될 것이다.

접속한 이후 [NEW]를 통해 연결 정보를 기입해야 한다.

일단 [BASE] 부분에 `NAME`은 왼쪽 판넬에 보여질 이름이고 `HOST`가 있다.

도커 컴포즈로 한번에 띄웠기 떄문에 `HOST`부분에 `nats://nats-server:4222`를 기입하고 저장하면 성공 표시가 뜨게 된다.

만일 상용으로 인증 설정을 했다면 그에 따른 정보를 기입하면 될것이다.

# About NATS

공식 홈을 보면 고성능, 경량, 오픈소스의 메시지 시스템으로 설계되어 있다고 못을 박아두고 있다.

그래서 지금까지 진행해오면서 경험한 다른 메시지 브로커들에서 보이는 개념들을 공유하고 있다.

다만 `NATS`에는 눈여겨 볼 부분이 있다.

# nats-core

앞서 `Redis`파트에서 `At-most-once`에 대해 언급한 적이 있다.

`최대 한번 전달`이라는 개념은 `일단 메시지는 보냈어`이다.

이것은 컨슈머가 없거나 때마침 그때 컨슈머가 죽었다면 메시지를 폐기한다.

그래서 `RabbitMQ`나 `Kafka`에서는 다시 컨슈머가 살거나 붙었을 때 `At-least-once` 방식으로 처리한다.

물론 `Redis`파트에서도 `Streams`방식으로 이것을 구현하기도 했다.

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

하지만 여기서는 `Stream`이라는 저장소가 존재하며 토픽이라는 개념을 여기서는 `subject`로 표현하는 거 같다.

뭐 `주제`라고 하는데 `subject`는 `.`을 이용한 계층 방식으로 이름을 지어야 한다.

[NATS.io - Subject hierarchies](https://docs.nats.io/nats-concepts/subjects#subject-hierarchies)

이렇게 사용해야 하는 이유는 `NATS`내부에서 사용하는 `Subject Routing Tree`에 기반한다.

개념은 달라도 `RabbitMQ`의 `Topic Exchange`와 상당히 유사한 부분이 있다.

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

일반적으로 우리가 생각하는 메시지 브로커는 `Push`방식일 것이다.

특히 실시간 데이터를 처리한다면 `Push`방식이 좋지만 `Backpressure`를 고민하지 않을 수 없다.

예를 들면 메시지가 1000건씩 들어오는데 컨슈머가 900건만 처라할 수 있다면 100건을 처리하지 못하는 현상이 발생한다.

그래서 `Backpressure` 전략을 고민해야 한다.

`Kafka`의 아키텍쳐는 컨슈머가 플랫폼으로부터 데이터를 직접 댕겨오는 구조이다.

컨슈머 클라이언트가 처리할 수 있는 만큼 땡겨와서 처리하면 `offset`관리 차원에서도 유연하다.

`NATS`는 이 두가지 방식을 지원한다.

만일 실시간으로 빠르게 처리를 해야 한다면 `Push`방식을 선택하고 안정적인 처리가 필요하다면 `Pull` 방식으로 가져가면 좋다.

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
                .allowMessageTtl(true)
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

하지만 `NATS`는 생성시에 주제를 미리 등록하는 방식이다.

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
                    .inactiveThreshold(Duration.ofHours(1))
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

`NATS`는 이걸 `Queue Group`이라고 표현한다.

[NATS.io - Queue Groups](https://docs.nats.io/nats-concepts/core-nats/queue)

따라서 여러 컨슈머들이 동시에 처리하는게 아니라 먼저 컨슈머들이 순차적으로 들어온 메시지를 처리하기 때문에 중복 작업을 하지 않는다.

하지만 같은 `subject`를 구독하지만 이 값이 다르면 서로 독립된 `Queue Group`이라고 인식하게 된다.

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

코드를 보면 마지막 구독을 하는 코드에 넘기는 `options`정보를 보면 `Push`방식을 사용하고 있다.

`PULL`방식은 `PUSH`와는 다른 방식을 선택해야 한다.

```kotlin
@Component
class NatsEventSubscriber(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<NatsEventSubscriber>()

    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) return

        val js = natsConnection.jetStream()
        val jsm = natsConnection.jetStreamManagement()

        handlers.forEach { handler ->
            setupPullSubscription(js, jsm, handler)
        }
    }

    private fun setupPullSubscription(
        js: JetStream,
        jsm: JetStreamManagement,
        handler: MessageHandler<*>,
    ) {
        val channel = handler.channel
        val subject = channel.channelName
        val targetType = channel.type
        val streamName = props.streamName

        val instanceId = UUID.randomUUID().toString().substring(0, 8)
        val uniqueDurable = "${subject.replace(".", "_")}_$instanceId"

        try {
            val consumerConfig =
                ConsumerConfiguration
                    .builder()
                    .durable(uniqueDurable)
                    .filterSubject(subject)
                    .maxDeliver(props.maxDelivery)
                    .ackWait(Duration.ofSeconds(30))
                    .inactiveThreshold(Duration.ofHours(1))
                    .build()

            jsm.addOrUpdateConsumer(streamName, consumerConfig)
        } catch (e: Exception) {
            log.error("Consumer [$uniqueDurable] setup failed: ${e.message}")
            return
        }

        taskExecutor.execute {
            log.info("Starting NATS Pull polling: [${handler::class.simpleName}] on Subject: [$subject]")

            val options = PullSubscribeOptions.bind(streamName, uniqueDurable)
            val sub = js.subscribe(subject, options)

            synchronized(subscriptions) { subscriptions.add(sub) }

            while (!Thread.currentThread().isInterrupted) {
                try {
                    val messages = sub.fetch(1, Duration.ofSeconds(1))
                    for (msg in messages) {
                        processMessage(msg, handler, targetType, subject)
                    }
                } catch (e: Exception) {
                    if (e is InterruptedException || Thread.currentThread().isInterrupted) {
                        log.info("NATS Polling interrupted: $subject")
                        Thread.currentThread().interrupt()
                        break
                    }
                    log.error("NATS Pull error [$subject]: ${e.message}. Retrying in 2s...")
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun processMessage(
        msg: Message,
        handler: MessageHandler<*>,
        targetType: Class<*>,
        subject: String,
    ) {
        try {
            val rawData = byteToObject(msg.data, Any::class.java)
            val finalMessage = convertMessage(rawData, targetType)

            (handler as MessageHandler<Any>).handle(finalMessage)
            msg.ack()
        } catch (e: Exception) {
            val deliveredCount = msg.metaData().deliveredCount()
            log.error("NATS 처리 실패 [$deliveredCount/${props.maxDelivery}] [Subject: $subject]: ${e.message}")

            if (deliveredCount >= props.maxDelivery) {
                log.error("Max delivery reached. Terminating message.")
                // TODO: DB 저장 또는 알림으로 모니터링
                msg.term()
            } else {
                msg.nak()
            }
        }
    }

    /**
     * for graceful shutdown
     */
    @PreDestroy
    fun shutdown() {
        log.info("NATS all Subscribers closed...")
        synchronized(subscriptions) {
            subscriptions.forEach { sub ->
                try {
                    sub.unsubscribe()
                } catch (e: Exception) {
                    log.warn("NATS 구독 해제 중 오류 발생: ${e.message}")
                }
            }
            subscriptions.clear()
        }
        log.info("successfully NATS all Subscribers complete closed!")
    }
}
```
뭔가 복잡해 보이지만 사실 `Redis`의 `Streams`방식과 다를게 없다.

기존의 작성한 테스트 코드를 실행을 해보자.

다만 눈여겨 볼 부분은 다음이다.

```kotlin
synchronized(subscriptions) { subscriptions.add(sub) }

while (!Thread.currentThread().isInterrupted) {
    try {
        val messages = sub.fetch(1, Duration.ofSeconds(1))
        for (msg in messages) {
            processMessage(msg, handler, targetType, subject)
        }
        //...
```
`subscription`에서 메시지를 1개만 가져오도록 설정했다.

사실 `batchSize`를 크게 잡는 것이 아무래도 성능면에서 유리할 수 있다.

만일 10개를 처리할 수 있는데 하나씩 가져온다면 총 10번의 라운드 트립이 생기는데 차라리 10개나 100개를 한번에 가져와 처리하는 방법을 생각해 볼 수 있다.

로그를 통해 확인할 수 있고 [NATS UI - Nui](http://localhost:31311/)을 통해 오고 간 메시지 흔적을 확인할 수 있다.

# Queue Group

이제는 이전부터 해왔던 방식으로 여러개의 컨슈머가 있으면 로드-밸런싱 방식으로 각각의 컨슈머가 중복처리 없이 메시지를 소비하도록 하자.

기존의 코드에서 크게 바뀔건 없다. 

그저 한부분만 바꾸면 된다.

```kotlin
@Component
class NatsEventSubscriber(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val handlers: List<MessageHandler<*>>,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<NatsEventSubscriber>()

    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        if (handlers.isEmpty()) return

        val js = natsConnection.jetStream()
        val jsm = natsConnection.jetStreamManagement()

        handlers.forEach { handler ->
            setupPullSubscription(js, jsm, handler)
        }
    }

    private fun setupPullSubscription(
        js: JetStream,
        jsm: JetStreamManagement,
        handler: MessageHandler<*>,
    ) {
        val channel = handler.channel
        val subject = channel.channelName
        val targetType = channel.type
        val streamName = props.streamName

        val queueGroupDurable = "${subject.replace(".", "_")}_group"

        try {
            val consumerConfig =
                ConsumerConfiguration
                    .builder()
                    .durable(queueGroupDurable)
                    .filterSubject(subject)
                    .maxDeliver(props.maxDelivery)
                    .ackWait(Duration.ofSeconds(30))
                    //.inactiveThreshold(Duration.ofHours(1))
                    .build()

            jsm.addOrUpdateConsumer(streamName, consumerConfig)
        } catch (e: Exception) {
            log.error("Consumer [$queueGroupDurable] setup failed: ${e.message}")
            return
        }

        taskExecutor.execute {
            //...
        }
    }
 }
```
부분적인 코드만 살펴보자.

일단 그룹명의 접미사로 붙는 정보는 `yaml`과 `NatsProperties`를 업데이트하고 같은 `subject`에 대해서 동일한 이름을 부여한다.

이 때 `Queue Group`은 공용이 되기 때문에 `inactiveThreshold`은 주석처리를 한다.

만일 오랜 기간 사용되지 않는 경우라면 이 부분에 긴 시간을 주어서 이후 자동으로 삭제가 되도록 설정할 수 있다.

테스트 코드에는 `TTL`을 적용한 테스트도 같이 작업을 해 놨다.

여기서는 초단위기때문에 100초이후 메시지가 사라진 것을 [NATS UI - Nui](http://localhost:31311/)에서 확인 가능하다.

# NATS 몰라도 상관없지만 이것때문에 여기까지 온 것은 바로 Request-Reply 기능

이 프로젝트를 진행하게 된 강려크한 동기이다.

### **아니! 이걸 마치 `RESTful API`처럼 사용할 수 있다고??????????**


일단 내용을 살펴보자.

[NATS.io - Request-Reply](https://docs.nats.io/nats-concepts/core-nats/reqreply)

공홈에 있는 `The Pattern`을 보면 저게 어떻게 가능할까 유심히 보고 있다.

내용은 간단해 보이긴 한다.

이와 관련 공홈에 공식 개발 가이드가 있다.

[NATS.io - Including a Reply Subject](https://docs.nats.io/using-nats/developer/sending/replyto)

먼저 이것을 위한 특정 `API`를 호출하면 그에 맞는 임시 주소를 만든다. `inbox`라고 부르는 여기에 특화된 `subject`를 생성한다.

`Ephemeral Subscription (휘발성 구독)`이라는 표현이 좀 의미심장한데 어째든 요청에 대한 유니크한 주소 정보를 생성한다.

요청받은 구독자 입장에서는 무언가를 처리하겠지만 이 정보도 같이 받게 되면 이것을 다시 발행해서 원래 요청한 쪽으로 보낸다는 것이다.

내부적으로 어떻게 구성되어 있는지는 위 링크에 있는 이미지를 보면 감이 온다.

![이미지](https://docs.nats.io/~gitbook/image?url=https%3A%2F%2F1487470910-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252F-LqMYcZML1bsXrN3Ezg0%252Fuploads%252Fgit-blob-dc10798d4afca301adba55c1e85c599b25a2ae24%252Freqrepl.svg%3Falt%3Dmedia%26token%3Dc224906b-c08b-492f-b083-cf247bc60efd&width=768&dpr=2&quality=100&sign=3e8a6ff3&sv=2)

이 그림을 보면 밑에 `Reply`라는 녀석이 바로 `Ephemeral Subscription`이다.

이것을 통해 요청자/응답자간의 연결고리가 생기면서 응답을 주고 받게 되는것이다.

`pub/sub`을 극강으로 사용하는 느낌적인 느낌이다.

요청자와 응답자는 사실 서버를 나눠서 작업해야 하지만 테스트이기 때문에 자웅동체로 만들어 보자.

# Responder

이 방식은 `Ephemeral Subscription`표현대로 `NatsConfig`에서 미리 `subject`를 등록하지 않는다.

내용을 보면 `nats-core`인거 같은데...

_**이러면 메시지가 유실될 수 있는거 아니야?_**

이런 생각은 뒤로 하고 일단 응답을 줄 핸들러를 만들어 보자

```kotlin
@Component
class HelloResponder(
    private val natsConnection: Connection,
) {
    private val log = logger<HelloResponder>()

    @PostConstruct
    fun replyTo() {
        val dispatcher =
            natsConnection.createDispatcher { msg ->
                log.info("msg info: $msg")
                log.info("msg data: ${msg.data}")
                log.info("msg replyTo: ${msg.replyTo}")
                // 안녕이라는 인사에 대답하는 초 간단 응답
                val answer = "How Are You?"
                natsConnection.publish(msg.replyTo, answer.toByteArray())
                log.info("complete response")
            }
        dispatcher.subscribe("nats.bow.hello", "nats-api-service-group")
        log.info("waiting response...")
    }
}
```

`natConnection`을 통해서 요청받을 `dispatcher`를 생성한다.

그리고 이 `dispatcher`에 특정 `subject`와 `Queue Group`을 등록해 보자.

`dispatcher`를 자세히 보면 특정 정보를 요청할 때 받은 메시지로부터 `replyTo`로 획득해서 바이트 배열로 다시 `publish`를 하는 구조이다.


```kotlin
@RestController
@RequestMapping("/api/nats")
@Tag(name = "NATS Request-Reply Controller")
class NatsRequestReplyController(
    private val natsConnection: Connection,
) {
    private val log = logger<NatsRequestReplyController>()

    @GetMapping("/request/hello")
    fun hello(): String {
        val subject = "nats.bow.hello"
        val payload = "hello".toByteArray()
        val replyFuture = natsConnection.request(subject, payload)
        return try {
            val reply = replyFuture.get(3, TimeUnit.SECONDS)
            String(reply.data)
        } catch (e: Exception) {
            log.error("error message: $e")
            "에러 발생: ${e.message}"
        }
    }
}
```

`natConnection`을 통해서 `request`라는 메소드로 요청하고 응답을 받는 아주 간단한 구조이다.

[Swagger](http://localhost:8080/swagger-ui/index.html)

스웨거를 통해서 테스트를 해보면 다음과 같이 로그가 찍힌다.

```text
[message-brokers-server] [         nats:4] i.b.global.broker.nats.HelloResponder    : msg replyTo: _INBOX.4KDFo6ihmJKpIEPfpyrAdV.4KDFo6ihmJKpIEPfpyrAqz
```

`msg`를 로그를 찍으면 `msg replyTo: _INBOX.4KDFo6ihmJKpIEPfpyrAdV.4KDFo6ihmJKpIEPfpyrAqz` 이 부분이 눈에 띈다.

흐름은 요청시에 유니크한 `_INBOX.4KDFo6ihmJKpIEPfpyrAdV.4KDFo6ihmJKpIEPfpyrAqz`이 정보를 같이 보내서 응답받을 때 들어올 정보를 보낸다.

응답을 받는 곳에서 요청을 처리하고 응답을 할때 이 주소를 통해 다시 `publish`한다.

위 이미지에 보이는 `Reply`가 이것을 보고 어디로 다시 메시지를 보내야 하는지 알 수 있고 요청한 곳으로 응답을 보낸다.

# jetStream을 통해서 메시지 유실없이 안정적으로 사용해 볼 수 없을까?

`nats-core`를 통해서 `Request/Reply`는 빠른 속도를 자랑한다.

하지만 컨슈머가 잠시 멈췄거나 어떤 장애가 발생하면 메시지가 유실된다는 것은 이미 위에서 언급했다.

`jetStream`으로 전환을 해서 안정적으로 운영하기 위해 `Responder`쪽을 `Pull`방식으로 전환해 보자.

# ReplyTo는 jetStream에 의해 변경된다?

지금 우리는 기존처럼 `Pull`방식을 사용할 것이다.

문제는 `nat-core`방식으로 `request`메소드를 통해 요청할 수 없다.

따라서 `jetStream`으로 변경시에는 `nat-core`에서 사용하는 방식으로 직집적으로 구현을 해야 할 필요가 있다.

기존 방식에서는 내부적으로 `replyTo`를 통해서 통신을 하게 되는데 `jetStream`방식에서는 이 방식을 사용할 수 없다.

처음 코드 작업을 하고 테스트를 해보면 응답자가 메시지를 수신하고 처리를 한것까지 로그로 확인을 했지만 요청자가 응답자로부터 어떤 정보도 받지 못했다.

그래서 `inBox`를 임시로 생성해서 보내준다 해도 `jetStream`은 자신이 관리하는 주소로 변경하기 때문에 요청을 하고 `Responder`가 처리를 하더라도 요청자는 응답을 받지 못한다.

왜냐하면 `replyTo` 임시 주소가 응답자 입장에서는 바껴서 들어오기 때문이다.

```text

1. 요청자 -> replyTo를 위한 inBox 생성
2. 응답자 -> 요청을 받고 응답을 하는데 이때 jetStream에 의해 새로 만들어진 주소로 발행을 한다.
3. 요청자가 보낸 replyTo와 응답자가 보낸 replyTo 불일치
4. 요청자는 기다리다가 응답이 없어 타입아웃 발생
```

# 기존의 Stream과 구분해서 사용해 보자

스트림이 메시지 보관소라고 언급했는데 `Request-Reply`용 스트림을 생성해서 보관소를 분리를 먼저 해보자.

```yaml
nats:
  server: nats://localhost:4222
  stream-name: BASQUIAT_STREAM
  api-stream-name: BASQUIAT_API_STREAM
  api-header: API_REPLY_TO
  max-delivery: 3
  durable-suffix: _group
```

전략은 요청자가 메시지를 발행할 때 헤더 정보를 설정할 수 있다.

`Responder`에서 응답을 할 때는 헤더에서 `replyTo`정보를 가져와 응답을 하는 방식을 취한다.

# Requester

```kotlin
@Service
class HelloRequestUsecase(
    private val natsConnection: Connection,
    private val props: NatsProperties,
) {
    fun execute(): String {
        val subject = ApiSubject.HELLO.subject
        val js = natsConnection.jetStream()

        // inbox를 생성
        val replyTo = natsConnection.createInbox()
        // 응답을 받은 임시 구독 생성
        val replySubscription = natsConnection.subscribe(replyTo)

        return try {
            // 헤더를 생성한다.
            val headers = Headers()
            headers.add(props.apiHeader, replyTo)

            val msg =
                NatsMessage
                    .builder()
                    .subject(subject)
                    .headers(headers)
                    .data("hello".toByteArray())
                    .build()

            js.publish(msg, PublishOptions.builder().expectedStream(props.apiStreamName).build())
            val replyMsg = replySubscription.nextMessage(Duration.ofSeconds(10))
            replyMsg?.let { String(it.data) } ?: natTimeout("NATS 응답자가 응답하지 않습니다. (Subject: $subject)")
        } finally {
            // 최종적으로 임시로 만든 구독 정보 삭제
            replySubscription.unsubscribe()
        }
    }
}
```
다음과 같이 헤더에 `replyTo`정보를 넘겨준다.

기존 방식과는 달리 임시 구독 정보를 생성하고 해당 `replyTo`주소로 응답받아 응답값으로 반환하도록 만든다.

`nextMessage`는 공홈에 보면 `Request-Reply Semantics`항목에서 확인할 수 있다.

```javascript
sub, err := nc.SubscribeSync(replyTo)
if err != nil {
    log.Fatal(err)
}

// Send the request immediately
nc.PublishRequest(subject, replyTo, []byte(input))
nc.Flush()

// Wait for a single response
for {
    msg, err := sub.NextMsg(1 * time.Second)
    if err != nil {
        log.Fatal(err)
    }

    response = string(msg.Data)
    break
}
sub.Unsubscribe()
```
위 코드는 공식 홈페이지의 코드 가이드로 이것을 비슷하게 구현해 보는 것이다.

# Responder With jetStream

```kotlin
@Component
class HelloResponderWithJetStream(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val taskExecutor: TaskExecutor,
) {
    private val log = logger<HelloResponderWithJetStream>()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        val jsm = natsConnection.jetStreamManagement()
        val streamName = props.apiStreamName
        val durable = ApiSubject.HELLO.durable
        val subject = ApiSubject.HELLO.subject

        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects(subject)
                .storageType(StorageType.File)
                .maxAge(Duration.ofHours(1))
                .discardPolicy(DiscardPolicy.Old)
                .build()

        try {
            jsm.getStreamInfo(streamName)
            jsm.updateStream(streamConfig)
        } catch (e: JetStreamApiException) {
            if (e.errorCode == 10058) {
                // 기존 STREAM과 중돌이 발생하면 지우고 다시 설정
                jsm.deleteStream(streamName)
                jsm.addStream(streamConfig)
            } else {
                jsm.addStream(streamConfig)
            }
        }

        val js = natsConnection.jetStream()
        val consumerConfig =
            ConsumerConfiguration
                .builder()
                .durable(durable)
                .filterSubject(subject)
                .maxDeliver(props.maxDelivery)
                .ackWait(Duration.ofSeconds(30))
                .build()

        jsm.addOrUpdateConsumer(streamName, consumerConfig)

        taskExecutor.execute {
            runPolling(js, streamName, durable, subject)
        }
    }

    private fun runPolling(
        js: JetStream,
        streamName: String,
        durable: String,
        subject: String,
    ) {
        val options = PullSubscribeOptions.bind(streamName, durable)
        val sub = js.subscribe(subject, options)
        synchronized(subscriptions) { subscriptions.add(sub) }

        while (!Thread.currentThread().isInterrupted) {
            try {
                val messages = sub.fetch(10, Duration.ofMillis(500))
                for (msg in messages) {
                    handleMessage(msg)
                }
            } catch (e: Exception) {
                if (Thread.currentThread().isInterrupted) break
                log.error("Polling Error: ${e.message}")
                Thread.sleep(2000)
            }
        }
    }

    private fun handleMessage(msg: Message) {
        try {
            // 헤더에서 응답할 replyTo를 가져온다.
            val replyTo = msg.headers?.getFirst(props.apiHeader)
            log.info("received Data: ${String(msg.data)}, replyTo: $replyTo")

            if (replyTo != null) {
                val answer = "How Are You?"
                natsConnection.publish(replyTo, answer.toByteArray())
                log.info("successfully send to: $replyTo")
            } else {
                log.warn("헤더에 ${props.apiHeader} 정보가 없습니다.")
            }
            msg.ack()
        } catch (e: Exception) {
            log.error("error occurred: ${e.message}")
        }
    }

    @PreDestroy
    fun stop() {
        synchronized(subscriptions) {
            subscriptions.forEach {
                try {
                    it.unsubscribe()
                } catch (_: Exception) {
                    // pass
                }
            }
            subscriptions.clear()
        }
    }
}
```

코드는 현재 완성되어져 있다.

이것을 토대로 스웨거에서 테스트를 해보면 응답값이 잘 넘어오는 것을 볼 수 있다.

# 피규어, 해적단 합류(예약) 요청

원피스도 이제 내용이 점점 산으로 가고 있는데 원피스 메인 주인공 피규어를 사는 것으로 해적단에 합류한다는 흐름으로 `API`작업을 해보고자 한다.

관련 코드는 `api`패키지에 만들어 놨으니 코드를 살펴보면 된다.

필요한 나머지 코드는 이미 `domain`에 구현해 논 상태이로 `NATS`의 `Request-Reply`로 각 도메인을 호출해서 처리하는 방식으로 진행한다.

흐름은 간단하다.

다만 결제 부분은 빠졌다.

```text
1. 즉시 피규어를 구입해 해적단에 합류한다.
2. 특정 시간에 피규어를 예약해서 해적단에 합류에 예약한다.
3. 예약 아이디로 구매 확정을 해 해적단에 합류한다.
```

정말 간단하지 않은가?

# Responder 추상화

컨슈머 입장에서 응답자는 엔드 포인트에 해당한다.

하지만 지금 코드 방식으로는 중복되는 코드가 많아질텐데 이 부분을 추상화를 먼저 해보자.

그리고 우리는 `String`으로 보낼 것이 아니라 보통 객체로 보낼 것이기 때문에 이것을 적용해서 추상화 작업을 한다.

그전에 추상화를 할 때 `setupStream`을 하는 부분에 `StreamConfiguration`을 생성해서 처리하는 부분이 있다.

여기서는 여러개의 `subject`을 계층적으로 유연하게 처리하기 위해서는 다음과 같이 `yaml`과 `NatsProperties`를 업데이트 해야 한다.

```yaml
nats:
  server: nats://localhost:4222
  stream-name: BASQUIAT_STREAM
  api-stream-name: BASQUIAT_API_STREAM
  api-header: API_REPLY_TO
  max-delivery: 3
  durable-suffix: _group
  api-allow-subject: "basquiat.>"
```

다음은 `ApiSubject` 클래스이다.

```kotlin
enum class ApiSubject(
    val subject: String,
    val durable: String,
) {
    HELLO("nats.bow.hello", "hello-api-group"),

    FETCH_PRODUCT("basquiat.product.get", "product-get-durable"),

    PLACE_ORDER("basquiat.order.create", "order-create-durable"),
    FETCH_ORDER("basquiat.order.get", "order-get-durable"),

    RESERVATION("basquiat.reservation", "reservation-request-durable"),
    RESERVATION_CONFIRM("basquiat.reservation.confirm", "reservation-confirm-durable"),
}
```

현재 `subject`를 `basquiat.`를 시작으로 계층적으로 설정했다.

이 부분은 개인에 맞춰서 설정할 수 있는데 `basquiat.>`이라는 표현은 이 하위의 모든 것을 와일드카드형식으로 받겠다는 표현을 한다.

이와 관련해 [NATS.io - Subject-Based Messaging](https://docs.nats.io/nats-concepts/subjects) 여기에서 내용을 확인할 수 있다.

이유는 여러개의 `subject`가 등록이 될텐데 `API`를 위한 스트림에서는 여러 개의 `Responder`가 생성될 때 이것을 덮어씌우는 현상이 발생을 한다.

따라서 스트림에서는 이렇게 와일드카드 형식으로 유연하게 묶어서 처리할 수 있도록 설정을 하는 것이다.

```kotlin
abstract class AbstractResponder<T>(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val taskExecutor: TaskExecutor,
    private val apiSubject: ApiSubject, // 도메인별 설정 (durable, subject 등)
) {
    protected val log = logger<AbstractResponder<*>>()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        val jsm = natsConnection.jetStreamManagement()
        val streamName = props.apiStreamName
        val allowApiSubject = props.apiAllowSubject
        val subject = apiSubject.subject
        val durable = apiSubject.durable

        setupStream(jsm, streamName, allowApiSubject)
        setupConsumer(jsm, streamName, subject, durable)

        val js = natsConnection.jetStream()
        taskExecutor.execute { runPolling(js, streamName, durable, subject) }
    }

    private fun setupStream(
        jsm: JetStreamManagement,
        streamName: String,
        allowApiSubject: String,
    ) {
        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects(allowApiSubject)
                .storageType(StorageType.File)
                .maxAge(Duration.ofHours(1))
                .discardPolicy(DiscardPolicy.Old)
                .build()

        try {
            jsm.getStreamInfo(streamName)
            jsm.updateStream(streamConfig)
        } catch (e: JetStreamApiException) {
            // 10058: 설정 충돌 / 10065: Subject 중복 에러 처리
            if (e.errorCode == 10058 || e.errorCode == 10065) {
                jsm.deleteStream(streamName)
                jsm.addStream(streamConfig)
            } else {
                jsm.addStream(streamConfig)
            }
        }
    }

    private fun setupConsumer(
        jsm: JetStreamManagement,
        streamName: String,
        subject: String,
        durable: String,
    ) {
        val consumerConfig =
            ConsumerConfiguration
                .builder()
                .durable(durable)
                .filterSubject(subject)
                .maxDeliver(props.maxDelivery)
                .ackWait(Duration.ofSeconds(30))
                .build()
        jsm.addOrUpdateConsumer(streamName, consumerConfig)
    }

    private fun runPolling(
        js: JetStream,
        streamName: String,
        durable: String,
        subject: String,
    ) {
        val options = PullSubscribeOptions.bind(streamName, durable)
        val sub = js.subscribe(subject, options)
        synchronized(subscriptions) { subscriptions.add(sub) }

        while (!Thread.currentThread().isInterrupted) {
            try {
                val messages = sub.fetch(10, Duration.ofMillis(500))
                for (msg in messages) {
                    processMessage(msg)
                }
            } catch (e: Exception) {
                if (Thread.currentThread().isInterrupted) break
                log.error("Polling Error [$subject]: ${e.message}")
                Thread.sleep(2000)
            }
        }
    }

    private fun processMessage(msg: Message) {
        val meta = msg.metaData()
        val deliveredCount = meta.deliveredCount()
        try {
            val replyTo = msg.headers?.getFirst(props.apiHeader)

            val response = handleRequest(msg)

            if (replyTo != null && response != null) {
                natsConnection.publish(replyTo, objectToByte(response))
            }
            msg.ack()
        } catch (e: Exception) {
            if (deliveredCount >= props.maxDelivery) {
                log.error("최대 재시도 횟수 초과 -> Seq: ${meta.streamSequence()}")
                msg.term()
            } else {
                val delaySeconds = (deliveredCount * 2L).coerceAtLeast(1L)
                val delay = Duration.ofSeconds(delaySeconds)
                msg.nakWithDelay(delay)
            }
        }
    }

    /**
     * 이 부분이 구현할 포인트
     */
    protected abstract fun handleRequest(msg: Message): T?

    @PreDestroy
    fun stop() {
        synchronized(subscriptions) {
            subscriptions.forEach {
                try {
                    it.unsubscribe()
                } catch (_: Exception) {
                    log.warn("구독 해제 실패")
                }
            }
            subscriptions.clear()
        }
    }
}
```
중복 코드들을 추상화 하고 실제 `Responder`는 `handlerRequest`만 구현하도록 처리하면 된다.

당연히 `NATS`를 통해 요청하는 요청자 부분도 추상화할 필요가 있다.

```kotlin
abstract class AbstractRequester<R, T>(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    private val apiSubject: ApiSubject,
    private val responseType: Class<T>,
) {
    protected val log = logger<AbstractRequester<*, *>>()

    fun sendRequest(request: R): T {
        val subject = apiSubject.subject
        val js = natsConnection.jetStream()

        // 1. 임시 응답 통로(Inbox) 설정
        val replyTo = natsConnection.createInbox()
        val replySubscription = natsConnection.subscribe(replyTo)

        return try {
            val headers = Headers().apply { add(props.apiHeader, replyTo) }
            val payload = objectToByte(request)

            val msg =
                NatsMessage
                    .builder()
                    .subject(subject)
                    .headers(headers)
                    .data(payload)
                    .build()

            js.publish(msg, PublishOptions.builder().expectedStream(props.apiStreamName).build())
            log.info("NATS 요청 발행 완료: $subject, ReplyTo: $replyTo")

            val replyMsg =
                replySubscription.nextMessage(Duration.ofSeconds(10))
                    ?: natsTimeout("NATS 응답자가 응답하지 않습니다. (Subject: $subject)")

            byteToObject(replyMsg.data, responseType)
        } finally {
            replySubscription.unsubscribe()
        }
    }
}
```
제너릭을 통해서 추상화를 한다.

```kotlin
@Component
class PlaceOrderResponder(
    natsConnection: Connection,
    props: NatsProperties,
    taskExecutor: TaskExecutor,
    private val usecase: PlaceOrderHandler,
) : AbstractResponder<PlaceOrderResponse>(natsConnection, props, taskExecutor, ApiSubject.PLACE_ORDER) {
    override fun handleRequest(msg: Message): PlaceOrderResponse {
        val request = byteToObject(msg.data, PlaceOrder::class.java)
        return try {
            usecase.execute(request)
        } catch (e: Exception) {
            PlaceOrderResponse(error = e.message)
        }
    }
}

@Service
class PlaceOrderHandler(
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun execute(request: PlaceOrder): PlaceOrderResponse {
        val (productId, quantity) = request

        val product =
            productRepository
                .findById(productId)
                .orElseThrow { NoSuchElementException("해당 보물을 찾을 수 없습니다.") }
        if (product.quantity < quantity) unableToJoin("재고가 부족하여 해적단에 합류할 수 없습니다!")
        product.quantity -= quantity
        val order =
            Order(
                product = product,
                quantity = quantity,
                status = OrderStatus.COMPLETED,
            )
        val completeOrder = orderRepository.save(order)
        return PlaceOrderResponse(orderId = completeOrder.id)
    }
}

@Service
class PlaceOrderHandler(
    natsConnection: Connection,
    props: NatsProperties,
) : AbstractRequester<PlaceOrder, OrderResponse>(
    natsConnection,
    props,
    ApiSubject.PLACE_ORDER,
    OrderResponse::class.java,
) {
    fun execute(request: PlaceOrder): OrderResponse {
        return sendRequest(request)
    }
}
```
추상화된 코드를 통해서 코드를 단순화 시킨다.

스웨거를 통해서 요청을 하면 주문 요청을 하고 성공을 하면 `orderId`가 넘어온다.

또한 재고 분량을 체크해서 재고가 0이거나 주문시 구입 수량에 따라 에러 메세지가 넘어오는 것도 확인할 수 있다.

이런 방식으로 현재 예약 및 구매확정 코드도 완성해 놨다.

```text
Order
    - 주문하기 (NATS Request-Reply)
    - 주문한 정보 가져오기 (NATS Request-Reply)

Product
    - 전체 상품 가져오기 (QueryDSL)
    - 상품아이디로 해당 상품 가져오기 (NATS Request-Reply)

Reservation
    - 상품 예약하기 (NATS Request-Reply)
    - 예약 상품 구매 확정 (NATS Request-Reply)
    - 예약 취소 (NATS Request-Reply)
```

진행하면서 몇가지 문제가 발생한 부분은 코드를 보완해서 수정을 해서 전체적으로 올린다.

그리고 기존에 테스트한 정보는 `NATS`에서 `Stream`이나 `subject`가 꼬일 수 있다.

그래서 [NATS UI - Nui](http://localhost:31311/)에서 [STREAM]탭에서 `DELETE` 또는 `PURGE`를 눌러서 깔끔한 상태에서 전체적으로 완성된 코드를 테스트 해보는것을 권장한다.

# 테스트 하면서 마주하게 된 문제점

솔직히 `NATS`를 이용해 `RESTful API`를 대체할 수 있을지 의문이긴 하다.

먼저 `Consumer`측 문제를 먼저 보고자 한다.

지금처럼 도메인을 나눠서 서버를 구성하는게 아닌 하나의 서버에 온갖 기능을 다 구겨넣고 있는데 `Responder`가 늘어나면서 문제가 발생한다.

그 중에 하나가 쓰레드 풀 문제이다.

먼저 [NATS UI - Nui](http://localhost:31311/)를 통해 내가 만든 스트림내에 `consumers`정보를 보면 알 수 있었던 부분이다.

만일 컨슈머가 제대로 풀링을 하고 있다면 각 소비자들의 `WAITING`에 1이 찍히게 된다.

```text
나 구독하고 있어요~~~
```
하는 일종의 시그널인데 쓰레드풀이 늘어나면 초기 쓰레드 풀 갯수 이상의 소비자가 있다면 시그널을 보내지 못한다.

그래서 해당 `subject`로 메시지를 보내면 `PENDING`상태로 들어가게 되는데 이유는 해당 주제를 구독하는 소비자가 없기 떄문이다.

따라서 정말 이런 경우는 없겠지만 하나의 서버로 응답을 하는 `Responder`를 구현하겠다면 다음과 같이 

```kotlin
@EnableAsync
@Configuration
class AsyncConfig {
    @Bean(name = ["natsTaskExecutor"])
    fun taskExecutor(): TaskExecutor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 15
        executor.maxPoolSize = 30
        executor.queueCapacity = 10

        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)

        executor.setThreadNamePrefix("NATS-task-")

        executor.setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())

        executor.initialize()
        return executor
    }
}
```
설정을 따로 해주는게 좋다.

여기서 `corePoolSize`는 최소한 `Responder`수에 여유 풀 사이즈를 두어서 세팅을 해야 한다.

```kotlin
abstract class AbstractResponder<T>(
    private val natsConnection: Connection,
    private val props: NatsProperties,
    @Qualifier("natsTaskExecutor")
    private val taskExecutor: TaskExecutor,
    private val apiSubject: ApiSubject, // 도메인별 설정 (durable, subject 등)
) {
    protected val log = logger<AbstractResponder<*>>()
    private val subscriptions = mutableListOf<JetStreamSubscription>()

    @PostConstruct
    fun init() {
        val jsm = natsConnection.jetStreamManagement()
        val streamName = props.apiStreamName
        val subject = apiSubject.subject
        val durable = apiSubject.durable

        setupConsumer(jsm, streamName, subject, durable)

        val js = natsConnection.jetStream()
        taskExecutor.execute { runPolling(js, streamName, durable, subject) }
        log.info("### Polling Start: $subject")
    }

    private fun setupConsumer(
        jsm: JetStreamManagement,
        streamName: String,
        subject: String,
        durable: String,
    ) {
        val consumerConfig =
            ConsumerConfiguration
                .builder()
                .durable(durable)
                .filterSubject(subject)
                .maxDeliver(props.maxDelivery)
                .ackWait(Duration.ofSeconds(30))
                .build()
        jsm.addOrUpdateConsumer(streamName, consumerConfig)
    }

    private fun runPolling(
        js: JetStream,
        streamName: String,
        durable: String,
        subject: String,
    ) {
        val options = PullSubscribeOptions.bind(streamName, durable)
        val sub = js.subscribe(subject, options)
        synchronized(subscriptions) { subscriptions.add(sub) }

        while (!Thread.currentThread().isInterrupted) {
            try {
                val messages = sub.fetch(10, Duration.ofMillis(500))
                for (msg in messages) {
                    processMessage(msg)
                }
            } catch (_: InterruptedException) {
                log.info("[$subject] 폴링 스레드가 종료 신호를 받아 정지합니다.")
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                if (Thread.currentThread().isInterrupted) break
                log.error("Polling Error [$subject]: ${e.message}")
                try {
                    Thread.sleep(2000)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun processMessage(msg: Message) {
        val meta = msg.metaData()
        val deliveredCount = meta.deliveredCount()
        try {
            val replyTo = msg.headers?.getFirst(props.apiHeader)

            log.info("메시지 수신 [${msg.subject}] - ReplyTo: $replyTo")

            val response = handleRequest(msg)

            if (replyTo != null && response != null) {
                natsConnection.publish(replyTo, objectToByte(response))
            }
            msg.ack()
        } catch (e: Exception) {
            if (deliveredCount >= props.maxDelivery) {
                log.error("최대 재시도 횟수 초과 -> Seq: ${meta.streamSequence()}")
                msg.term()
            } else {
                val delaySeconds = (deliveredCount * 2L).coerceAtLeast(1L)
                val delay = Duration.ofSeconds(delaySeconds)
                msg.nakWithDelay(delay)
            }
        }
    }

    /**
     * 이 부분이 구현할 포인트
     */
    protected abstract fun handleRequest(msg: Message): T?

    @PreDestroy
    fun stop() {
        synchronized(subscriptions) {
            subscriptions.forEach {
                try {
                    it.unsubscribe()
                } catch (_: Exception) {
                    log.warn("구독 해제 실패")
                }
            }
            subscriptions.clear()
        }
    }
}
```

기존의 `AbstractResponder`에서 이것을 명시적으로 사용하도록 처리하자. 

# Next Step

지금 `NATS`의 `Request-Reply` 테스트를 진행하면서 몇 가지 보완해야 할 부분은 주문 부분이다.

그래서 다음 브랜치는 기존에 설정한 `Redis`를 통해 분산락을 적용할 생각이다.

[번외 - Redis를 이용한 분삭락 전략](https://github.com/basquiat78/spring-boot-message-brokers/tree/04-with-distributed-lock)