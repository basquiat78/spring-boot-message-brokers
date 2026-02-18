# Kafka Streams

이런 걸 생각해 보자.

```text
1. 해적단 합류 총 랭킹 집계를 내고 싶은데?
2. 해적단 예약 합류 총 랭킹 집계를 내고 싶은데>
3. 해적단 예약 합류하다가 취소한 집계를 내고 싶은데? 
```

만일 지금처럼 `NATS`의 `Request-Reply`가 아닌 이벤트 방식으로 이것을 운영한다면 충분히 가능할 것이다.

이것을 가능케 할려고 하는게 `Kafka Streams`이다.

단순히 `pub/sub`이 아닌 로그를 통해 어떤 정보를 가공할 수 있는데 그렇다면 지금같은 구조에서 어떻게 하면 좋을까?

방법은 딱 하나다.

`NATS`를 통해 어떤 요청들이 들어올 때 그것을 처리하고 `Kafka Streams`를 통해 정보를 모으면 된다.

지금까지 진행해 오면서 같은 공통 `BrokerChannel`과 `BrokerType`을 통해서 공통 토픽에 대해서 `pub/sub`을 진행을 해 왔다.

이제는 구조를 좀 나눠볼 필요가 있다.

# Stream Processor

## Topology

우리가 하고자 하는 것은 일종의 흐름으로 외부에서 바라볼 필요가 있다.

지금까지 `pub/sub`은 `메시지 발행 > 브로커 > 메시지 소모`라는 아주 단순한 방식으로 구현하고 진행해 왔다.

그런데 이런 생각을 해 볼 수 있다. 

```text
좋아!
다 이해했어!

그런데 이렇게 메시지가 오고가고 소모를 하는 그 와중에 특정 데이터를 원하는 대로 가공을 할 수 있지 않을까???
```

`pub/sub`은 단순하지만 어쨰든 이것은 가장 기본이 되는 부분이다.

이것을 크게 잡아서 다음과 같이 설명을 할 수 있다.

### SOURCE

소스라는 것을 딱히 설명할 필요가 있을까?

데이터가 스트림즈를 통해 흘러갈 때 처음 데이터가 진입하는 시점에서 이것을 `Source`라고 할 수 있을 것이다.

가장 기본적인 내용이지만 결국 우리가 데이터를 처리하려면 데이터를 읽어와야 한다.

`pub/sub`의 개념이 쉬우면서도 가장 기본이 되는 이유는 이런 부분이다.

우리가 어느 토픽으로 데이터를 발행할 것인지가 바로 시작점이다.

### Processor

그렇다면 이제는 `Kafka`를 통해서 이렇게 들어온 데이터를 어느 토픽에서 읽고 처리를 하게 된다.

토픽으로 상품을 조회하는 데이터가 들어오게 된다면 이것을 먼저 가공할 수 있도록 변환하는 작업이 필요하다.

그리고 상품 아이디 별로 분류를 하는 작업과 집계를 통해서 원하는 데이터로 가공을 하게 된다.

### Sink

이렇게 가공된 데이터를 다른 곳으로 `publish`하고 다른 곳에서 소비할 수도 있다.

여기서는 `State Store`, 일명 상태 저장소에 저장하고 이것을 읽어가도록 할 것이다.


# Stream Topic을 결정하자.

`NATS`의 `Request-Reply`을 위해 이전 브랜치에서 `ApiSubject`을 설정했다.

여기서 우리가 필요한 토픽은 다음과 같다.

- 합류 (basquiat.order.create)
- 예약 합류 (basquiat.reservation.create)
- 예약 합류 확정 (basquiat.reservation.confirm)
- 예약 합류 취소 (basquiat.reservation.cancel)
- 해적단 보물 조회 (basquiat.product.fetch)

해적단 보물 조회는 어떤 보물이 인기가 많은지 집계를 할 필요가 있다고 판단하고 부가적으로 추가를 했다.

```yaml
app:
  messaging:
    use-amqp: false
    use-redis: false
    use-kafka: true

kafka:
  stream-aggregator: onepiece-stream-aggregator
  state-store-dir: "./kafka-streams-state"
  consumer-id: basquiat-local-consumer
```
이전 브랜치에서는 `NATS`를 위해서 나머지 전부 `false`로 두었는데 여기서 `Kafka`를 활성화 시키고 `Configuration`에서 스트림즈 관련 구성을 하기 위해 아래 정보를 추가로 넣는다.

그리고 로그가 너무 많기 때문에 불필요한 로그를 찍지 않도록 `logback-spring.xml`에 `kafka`관련 로그는 `WARN`으로 설정을 해놨다.

먼저 스트림즈용 토픽을 모은 `enum`을 하나 만들자.

```kotlin
enum class KafkaStreamTopic(
    val topic: String,
    val aggregationKey: String,
) {
    PRODUCT_FETCH("basquiat.product.fetch", "search-join-store"),
    ;

    companion object {
        val allTopic: List<String>
            get() = entries.map { it.topic }

        fun findByName(name: String) = entries.find { it.topic == name }
    }
}
```

스트림즈를 통해 정보를 모아야 하기 때문에 스트림즈 토픽으로 보낼 스트림즈용 `publisher`를 따로 작업한다.

참고로 최초 로드를 할 때 토픽을 미리 생성하고 그것을 처리하는 `Processor`가 존재해야 에러가 나지 않는다.

일단 먼저 보물을 조회할때마다 어떤 데이터 처리를 해 볼 생각이다.


```kotlin
@ConditionalOnKafka
@Service("kafkaStreamPublisher")
class KafkaStreamPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) {
    private val log = logger<KafkaStreamPublisher>()

    fun <T : Any> publish(
        kafkaStreamTopic: KafkaStreamTopic,
        key: String,
        message: T,
    ) {
        kafkaTemplate
            .send(kafkaStreamTopic.topic, key, message)
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
여기서 `key`는 보물 아이디, 합류 아이디, 합류 예약 아이디 정보가 들어올 것이다. 

이것을 통해서 스트림즈가 `groupBy`나 `count`등 특정 작업을 통해서 데이터를 가공할 것이다.

기존의 `KafkaConfig`에서는 단순한 리스트 컨테이너 팩토리만 설정을 했는데 이제는 스트림즈를 사용할 것이기 때문에 해당 설정 빈을 추가한다.

그리고 기존에 테스트했던 `KafkaEventSubscriber`은 이제는 빈으로 등록하지 않도록 주석을 처리했다.

왜냐하면 `BrokerChannel`에 있는 토픽을 등록하도록 했지만 이제는 이것을 사용하지 않을 것이기 때문이다.

그 부분은 아래 코드를 보면 확인할 수 있다.

```kotlin
@Configuration
@EnableKafkaStreams
@ConditionalOnKafka
class KafkaConfig(
    private val props: KafkaProperties,
    @Value($$"${kafka.stream-aggregator:onepiece-stream-aggregator}")
    private val streamAggregator: String,
    @Value($$"${kafka.state-store-dir:./kafka-streams-state}")
    private val stateStoreDir: String,
    @Value($$"${kafka.consumer-id:basquiat-local-consumer}")
    private val consumerId: String,
) {
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

    /**
     * 스트림즈를 위한 토픽을 생성한다.
     */
    @Bean
    fun createTopics(): KafkaAdmin.NewTopics {
        // 먼저 스트림즈용 토픽을 미리 생성한다.
        val topics =
            KafkaStreamTopic.entries.map {
                TopicBuilder
                    .name(it.topic)
                    .partitions(3)
                    .replicas(1)
                    .build()
            }
        return KafkaAdmin.NewTopics(*topics.toTypedArray())
    }

    @Bean(name = [KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME])
    fun kStreamConfig(): KafkaStreamsConfiguration {
        val config = mutableMapOf<String, Any>()
        config[StreamsConfig.APPLICATION_ID_CONFIG] = streamAggregator
        config[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = props.bootstrapServers

        config[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String().javaClass
        config[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = Serdes.String().javaClass

        // 역직렬화 에러 핸들러 설정
        config[StreamsConfig.DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            LogAndContinueExceptionHandler::class.java
        // 직렬화 에러 핸들러 설정
        config[StreamsConfig.PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG] =
            DefaultProductionExceptionHandler::class.java

        // 병렬 처리 성능 개선을 위해 설정
        config[StreamsConfig.NUM_STREAM_THREADS_CONFIG] = Runtime.getRuntime().availableProcessors()
        config[StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG] = StreamsConfig.OPTIMIZE

        // 만일 클러스터 구성으로 설정하게 되면 복제본 숫자로 세팅한다.
        // 일반적으로 홀수로 최소 3개 이상으로 세팅할텐데 그에 맞춰서 설정을 한다.
        // 여기서는 로컬로 한대만 띄우기 때문에 1
        config[StreamsConfig.REPLICATION_FACTOR_CONFIG] = 1

        // RocksDB 상태 저장소의 일관성을 위한 지연 시간 설정을 명시적으로 하자.
        config[StreamsConfig.ACCEPTABLE_RECOVERY_LAG_CONFIG] = 10_000L
        // macos에서 테스트하기 때문에 다음과 같이 로컬에서 state store 경로를 지정을 하자.
        // 그렇지 않으면 rebalancing 에러가 발생해서 작동을 하지 않는다.
        // 프로젝트 root에 해당 폴더가 생성되고 rocksdb같은 폴더와 특정 정보들이 이곳에 생성된다.
        config[StreamsConfig.STATE_DIR_CONFIG] = stateStoreDir
        config[StreamsConfig.COMMIT_INTERVAL_MS_CONFIG] = 1000

        config[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = 300000
        config[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 45000
        config[ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG] = 3000
        // 컨슈머에서 서버가 죽었다 뜰 때 나 아까 그넘이라는 것을 알려주는 고유한 값.
        // 지금은 로컬이기 때문에 컨슈머가 하나라 하드코딩을 하고 있지만 실제로는 컨슈머마다 값을 다르게 세팅해야 한다.
        config[ConsumerConfig.GROUP_INSTANCE_ID_CONFIG] = consumerId

        return KafkaStreamsConfiguration(config)
    }
}
```
로컬에서 테스트하기 때문에 몇가지 특이 설정이 있는데 그중에 하나가 `config[StreamsConfig.STATE_DIR_CONFIG] = stateStoreDir`이다.

`State Store`는 일종의 저장소이다.

그래서 저장을 하기 위한 공간이 필요한데 이것을 따로 설정하지 않으면 현재 `macOs`의 경우 저장소를 생성하지 못해 문제가 발생한다.

또한 저장소에 저장할 때 `Serdes`를 통해 `String`타입으로 `key:value`로 저장하도록 설정했다.

이것은 어디까지나 성격에 따라 달라질 수 있기 때문에 이 부분은 조절을 해야 한다.

스트링 타입으로 두면 좀 편하기 때문에 여기서는 스트링 타입으로 설정을 했다.

# 보물을 조회시 어떤 보물을 얼마나 조회했는지 집계를 해보자.

```kotlin
@Component
class ProductFetchStreamProcessor {
    @Bean
    fun productFetchCountStream(builder: StreamsBuilder): KStream<String, Long> {
        val stream =
            builder.stream(
                KafkaStreamTopic.PRODUCT_FETCH.topic,
                Consumed.with(Serdes.String(), Serdes.String()),
            )

        val countBuilder =
            stream
                .groupByKey()
                .count(
                    Materialized
                        .`as`<String, Long, KeyValueStore<Bytes, ByteArray>>(
                            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        ).withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()),
                )

        return countBuilder.toStream()
    }
}
```
단순하게 들어온 정보로부터 `groupByKey`를 통해서 상품별로 그룹을 하고 조회 카운터를 `aggregationKey`에 갱신을 할 것이다.

기본적으로 `Consumed.with(Serdes.String(), Serdes.String())`에서는 이렇게 되어 있지만 반환할때는 이 값을 `Long`으로 반환하도록 설정을 했다.

그리고 우리는 기존의 `NATS`의 `FetchProductHandler`를 변경하자.

예를 들면 이런 식이다.

```text
오늘 해적단 합류를 할려고 보물을 몇 사람이 보고 있어요!
```

이런거 알려주고 싶지 않은가???

그렇다면 `ProductFetchStreamProcessor`를 통해서 `State Store`에 저장된 정보를 조회하는 서비스를 하나 만들어 보자

```kotlin
@Service
class StreamStateStoreQueryService(
    private val factoryBean: StreamsBuilderFactoryBean,
) {
    private val log = logger<StreamStateStoreQueryService>()

    private val defaultCount: Long = 0L

    /**
     *  상품의 실시간 조회수 가져오기
     */
    fun getProductViewCount(productId: String): Long {
        val kafkaStreams = factoryBean.kafkaStreams ?: return defaultCount

        // 어떤 이유로 State Store가 RUNNING이 아닐 때는 서비스가 중단되는 것을 막기 위해 기본값을 던지도록 한다.
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            log.warn("Kafka Streams State : ${kafkaStreams.state()}")
            return defaultCount
        }

        return try {
            val store =
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        QueryableStoreTypes.keyValueStore<String, Long>(),
                    ),
                )
            store.get(productId) ?: defaultCount
        } catch (e: InvalidStateStoreException) {
            // 리밸런싱 중이거나 상태가 변했을 때 잡아서 로그만 남기고 기본 값을 던지도록 한다.
            log.error("Kafka Streams Status is REBALANCING or Change: ${e.message}")
            defaultCount
        }
    }
}
```
위 코드를 보면 `KafkaStreams`에서 `ProductFetchStreamProcessor`에 의해 `aggregationKey`로 저장된 정보를 가져오도록 한다.

이것을 이제는 `FetchProductHandler`에서 사용하도록 변경한다.

```kotlin
/**
 * 기존 dto에 viewCount를 추가한다.
 */
data class ProductDto(
    override val id: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val viewCount: Long? = null,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val createdAt: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    val updatedAt: LocalDateTime,
) : LongIdentifiable {
    companion object {
        /**
         * entity to dto convert
         * @param entity
         * @return ProductDto
         */
        fun toDto(entity: Product) =
            with(entity) {
                ProductDto(
                    id = id!!,
                    name = name,
                    price = price,
                    quantity = quantity,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            }
    }

    fun withViewCount(viewCount: Long) = this.copy(viewCount = viewCount)
}

@Service
class FetchProductHandler(
    private val productService: ProductService,
    private val kafkaStreamPublisher: KafkaStreamPublisher,
    private val streamStateStoreQueryService: StreamStateStoreQueryService,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId

        // 스트림즈로 publish하면 앞서 정의한 Processor가 데이터를 처리한다.
        kafkaStreamPublisher.publish(
            kafkaStreamTopic = KafkaStreamTopic.PRODUCT_FETCH,
            key = productId.toString(),
            message = "PRODUCT_VIEW_COUNT_EVENT",
        )

        val product =
            productService
                .findByIdForCache(productId)

        // 조회 이후 state store를 통해서 조회수를 가져온다.
        val viewCount = streamStateStoreQueryService.getProductViewCount(productId.toString())
        return FetchProductResponse(product = product.withViewCount(viewCount))
    }
}
```
이제 스웨거를 통해서 조회를 해보자.


```json
{
  "path": "/api/products/1",
  "status": 200,
  "result": {
    "id": 1,
    "name": "[품절주의] 몽키 D. 루피 고무고무 열매",
    "price": 5656000,
    "quantity": 0,
    "viewCount": 0,
    "createdAt": "2026-02-12T13:40:48.593326",
    "updatedAt": "2026-02-12T13:40:48.593326"
  },
  "timestamp": "2026-02-15T17:10:46.777182"
}
```
최초 0이지만 클릭을 할 때마다 카운트가 증가하게 된다.

다만 이 방식은 좀 타이밍상으로 문제가 있다.

예를 들면 순차적으로 늘어나길 기대하겠지만 `Processor`가 처리하고 커밋을 치는 간격이 존재할 수 있다.

그래서 1, 2, 3 처럼 순차적으로 증가되기도 하지만 4, 4, 6처럼 건너띄고 넘어오는 경우도 있다.

아마도 5를 처리하는 시간이 좀 걸려서 4를 가져오고 다음 처리에는 제대로 가져와서 6을 가져오기도 한다.

이 방식은 이것보다는 아래와 같은 방식으로 처리하는 것이 좋을 것이다.

```text
1. 어떤 사람이 보물을 조회했다.
2. 바로 이 정보를 보여주지 않고 웹소켓이나 딜레이를 주고 풀링 방식으로 이 정보를 가져온다.
3. 보고 있는 사람에게 이 정보를 가져와서 `이 보물을 20명이 보고 있습니다.`라고 알림을 준다.
```

예를 들면 위에서 `Sink`를 설명했는데 `ProductFetchStreamProcessor`내에서 처리를 하고 다른 토픽으로 `publish`를 하는 것이다.

기존 코드 대신에 아래처럼 처리하면 된다.

```kotlin
@Bean
fun productFetchCountStreamBySink(builder: StreamsBuilder): KStream<String, Long> {
    val stream = builder.stream(
        KafkaStreamTopic.PRODUCT_FETCH.topic,
        Consumed.with(Serdes.String(), Serdes.String()),
    )

    val countTable = stream
        .groupByKey()
        .count(
            Materialized.`as`<String, Long, KeyValueStore<Bytes, ByteArray>>(
                KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
            ).withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Long()),
        )

    val resultStream = countTable.toStream()
    resultStream.to(
        "basquiat.product.view.count",
        Produced.with(Serdes.String(), Serdes.Long())
    )
    return resultStream
}
```
물론 이때 토픽 역시 미리 등록을 해야 한다는 것을 잊지 말자.

아마도 이 방법이 최적화된 방법일 것이다.


# 하지만 이건 전체 누적 조건이다.

만일 일별로 묶어서 처리하고 싶은 경우가 있을 수 있다.

이럴 때는 `windowedBy`를 통해서 이것을 처리가능하다.

기존에 사용하는 녀석은 빈으로 등록하지 않도록 주석처리한다.

```kotlin
@Component
class DailyProductFetchStreamProcessor {
    @Bean
    fun productFetchCountStream(builder: StreamsBuilder): KStream<String, Long> {
        val stream =
            builder.stream(
                KafkaStreamTopic.PRODUCT_FETCH.topic,
                Consumed.with(Serdes.String(), Serdes.String()),
            )

        val dailyCountBuilder =
            stream
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
                .count(
                    Materialized
                        .`as`<String, Long, WindowStore<Bytes, ByteArray>>(
                            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        ).withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()),
                )

        return dailyCountBuilder
            .toStream()
            .map { windowedKey, value ->
                KeyValue(windowedKey.key(), value)
            }
    }
}
```

조회 조건도 변경된다.

```kotlin
@Service
class StreamStateStoreQueryService(
    private val factoryBean: StreamsBuilderFactoryBean,
) {
    private val log = logger<StreamStateStoreQueryService>()

    private val defaultCount: Long = 0L

    /**
     *  상품의 실시간 조회수 가져오기
     */
    fun getProductViewCount(productId: String): Long {
        val kafkaStreams = factoryBean.kafkaStreams ?: return defaultCount

        // 어떤 이유로 State Store가 RUNNING이 아닐 때는 서비스가 중단되는 것을 막기 위해 기본값을 던지도록 한다.
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            log.warn("Kafka Streams State : ${kafkaStreams.state()}")
            return defaultCount
        }

        return try {
            val store =
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        QueryableStoreTypes.keyValueStore<String, Long>(),
                    ),
                )
            store.get(productId) ?: defaultCount
        } catch (e: InvalidStateStoreException) {
            // 리밸런싱 중이거나 상태가 변했을 때 잡아서 로그만 남기고 기본 값을 던지도록 한다.
            log.error("Kafka Streams Status is REBALANCING or Change: ${e.message}")
            defaultCount
        }
    }

    fun getTodayProductViewCount(productId: String): Long {
        val kafkaStreams = factoryBean.kafkaStreams ?: return defaultCount
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) return defaultCount

        return try {
            // 1. WindowStore 타입으로 가져오기
            val store =
                kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(
                        KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        QueryableStoreTypes.windowStore<String, Long>(),
                    ),
                )

            val now = Instant.now()
            val startOfDay =
                LocalDateTime
                    .of(LocalDate.now(), LocalTime.MIN)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()

            val iterator = store.backwardFetch(productId, startOfDay, now)

            if (iterator.hasNext()) {
                iterator.next().value
            } else {
                defaultCount
            }
        } catch (e: Exception) {
            defaultCount
        }
    }
}
```

핸들러에서도 새로 만든 메소드를 호출하도록 변경하자.

```kotlin
@Service
class FetchProductHandler(
    private val productService: ProductService,
    private val kafkaStreamPublisher: KafkaStreamPublisher,
    private val streamStateStoreQueryService: StreamStateStoreQueryService,
) {
    fun execute(request: FetchProduct): FetchProductResponse {
        val productId = request.productId

        // 스트림즈로 publish하면 앞서 정의한 Processor가 데이터를 처리한다.
        kafkaStreamPublisher.publish(
            kafkaStreamTopic = KafkaStreamTopic.PRODUCT_FETCH,
            key = productId.toString(),
            message = "PRODUCT_VIEW_COUNT_EVENT",
        )

        val product =
            productService
                .findByIdForCache(productId)

        // 조회 이후 state store를 통해서 조회수를 가져온다.
        val viewCount = streamStateStoreQueryService.getTodayProductViewCount(productId.toString())
        return FetchProductResponse(product = product.withViewCount(viewCount))
    }
}
```

이 방식으로 사용하게 되면 몇가지 주의점이 있다.

`windowedBy`를 통해서 `WindowStore`를 쓰고 있다.

그리고 우리는 하루를 기준으로 처리하도록 코드를 작성했다.

이것은 세그먼트로 나눠서 작성을 하고 하루가 지나면 다시 `Window`를 만들기 때문에 데일리로 카운트를 작성하게 된다.

그리고 이렇게 만들어진 경우에는 데이터가 `Retention Policy`에 의해 삭제가 된다.

즉, 수명이 존재한다.

따라서 하루가 지나면 이 정보를 DB에 넣어서 데일리로 데이터를 집계할 수 있도록 하는게 좋다.

그렇다면 하루에 한번 돌때 이전의 `Window`정보를 가져와서 무언가를 수행하도록 작성를 해볼 생각이다.

먼저 스케쥴을 한번 작성해 보자.

```kotlin
class DailyProductDBExporterProcessor : ContextualProcessor<Windowed<String>, Long, Void, Void>() {
    private val log = logger<DailyProductDBExporterScheduler>()

    override fun init(context: ProcessorContext<Void, Void>) {
        super.init(context)

        context().schedule(Duration.ofDays(1), PunctuationType.WALL_CLOCK_TIME) { timestamp ->
            val store: WindowStore<String, Long> = context().getStateStore(KafkaStreamTopic.PRODUCT_FETCH.aggregationKey)

            val startOfYesterday =
                LocalDate
                    .now()
                    .minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()

            val endOfYesterday =
                LocalDate
                    .now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .minusNanos(1)
                    .toInstant()

            store.fetchAll(startOfYesterday, endOfYesterday).use { iterator ->
                iterator.forEach { entry ->
                    // window의 entry를 통해서 저장된 상품 아이디 정보를 가져온다.
                    val productId = entry.key.key()
                    val viewCount = entry.value
                    log.info("period: $startOfYesterday - $endOfYesterday")
                    log.info("productId: $productId / viewCount: $viewCount")
                    // TODO 집계 테이블에 넣는 로직을 실행하자.
                }
            }
            log.info("[Scheduler] 어제 저장된 Window Store 데이터 처리 완료")
        }
    }

    /**
     * 아무것도 안할 것이기 때문에 그냥 패스
     */
    override fun process(record: Record<Windowed<String>, Long>?) {
        return
    }
}
```
우리가 만든 `DailyProductFetchStreamProcessor`은 하루를 기준으로 만들도록 처리했다.

```kotlin
@Component
class DailyProductFetchStreamProcessor {
    @Bean
    fun productFetchCountStream(builder: StreamsBuilder): KStream<String, Long> {
        val stream =
            builder.stream(
                KafkaStreamTopic.PRODUCT_FETCH.topic,
                Consumed.with(Serdes.String(), Serdes.String()),
            )

        val dailyCountStream =
            stream
                .groupByKey()
                .windowedBy(
                    TimeWindows
                        .ofSizeWithNoGrace(Duration.ofDays(1))
                        .advanceBy(Duration.ofDays(1)),
                ).count(
                    Materialized
                        .`as`<String, Long, WindowStore<Bytes, ByteArray>>(
                            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
                        ).withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.Long()),
                ).toStream()

        dailyCountStream.process(
            ProcessorSupplier { DailyProductDBExporterProcessor() },
            KafkaStreamTopic.PRODUCT_FETCH.aggregationKey,
        )

        return dailyCountStream.map { windowedKey, value ->
            KeyValue(windowedKey.key(), value)
        }
    }
}
```
위 코드에서 `ProcessorSupplier`에 스케쥴 프로세서를 넣어두도록 하면 된다.

사실 여기에는 몇가지 문제가 있다.

스케쥴 프로세서 내부에 `context().schedule(Duration.ofDays(1), PunctuationType.WALL_CLOCK_TIME) { `이런 부분이 있는데 이것은 시간상 문제를 보인다.

예를 들면 서버가 실행된 시점으로부터 하루이기 때문이다.

그래서 `Duration.ofDays(1)`을 초단위로 30초를 두고 실행이 되는 것을 보도록 하자.

현재는 어제 날짜로 생성된 정보가 없기 때문에 그냥 완료이후 로그만 찍힐 것이다.

그러면 차라리 한시간을 두고 스케쥴이 돌도록 하자.

그리고 보완을 해보자.

실제로 카프카와는 별개로 스케쥴 관련 이런 문제를 경험한 적이 있기 때문이다.

```kotlin
class DailyProductDBExporterProcessor : ContextualProcessor<Windowed<String>, Long, Void, Void>() {
    private val log = logger<DailyProductDBExporterProcessor>()

    // 정산 여부를 위한 변수 설정
    private var lastExportedDate: LocalDate? = null

    override fun init(context: ProcessorContext<Void, Void>) {
        super.init(context)

//        context().schedule(Duration.ofDays(1), PunctuationType.WALL_CLOCK_TIME) {
        context().schedule(Duration.ofMinutes(30), PunctuationType.WALL_CLOCK_TIME) { timestamp ->
            val now = LocalDateTime.now()
            val today = now.toLocalDate()

            // 만일 자정이거나 오전 1시가 아니라면 실행하지 않는다.
            if (now.hour !in 0..1) return@schedule
            // 마지막 정산 날짜가 오늘이다? 그러면 중복 실행 방지를 위해 실행하지 않는다.
            if (lastExportedDate == today) return@schedule

            val store: WindowStore<String, Long> = context().getStateStore(KafkaStreamTopic.PRODUCT_FETCH.aggregationKey)

            val startOfYesterday =
                LocalDate
                    .now()
                    .minusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()

            val endOfYesterday =
                LocalDate
                    .now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .minusNanos(1)
                    .toInstant()

            store.fetchAll(startOfYesterday, endOfYesterday).use { iterator ->
                iterator.forEach { entry ->
                    // window의 entry를 통해서 저장된 상품 아이디 정보를 가져온다.
                    val productId = entry.key.key()
                    val viewCount = entry.value
                    log.info("period: $startOfYesterday - $endOfYesterday")
                    log.info("productId: $productId / viewCount: $viewCount")
                    // TODO 집계 테이블에 넣는 로직을 실행하자.
                }
            }
            // 실행이 완료되면 마지막 정산 날짜를 오늘로 세팅한다.
            lastExportedDate = today
            log.info("[Scheduler] 어제 저장된 Window Store 데이터 처리 완료")
        }
    }

    /**
     * 아무것도 안할 것이기 때문에 그냥 패스
     */
    override fun process(record: Record<Windowed<String>, Long>?) {
        return
    }
}
```
나는 일단 30분마다 스케쥴을 돌고 조건이 맞으면 집계를 하도록 처리를 했다.

물론 이 방식이 좋을지는 잘 모르겠다. 컨슈머가 여러대라면 이 방법도 그다지 좋아 보이진 않는다.

하지만 이런 방식도 적용할 수 있다는 것을 보여주고 싶었다.


# At a Glance

`RabbitMQ`에서 `NATS` 그리고 `Kafka Streams`을 이용한 방법등을 알아보았다.

끗!

# Next Step

[Reids Sentinel Distributed Lock](https://github.com/basquiat78/spring-boot-message-brokers/tree/08-redis-sentienl-rlock)