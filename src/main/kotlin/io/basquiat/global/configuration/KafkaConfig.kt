package io.basquiat.global.configuration

import io.basquiat.global.annotations.ConditionalOnKafka
import io.basquiat.global.broker.kafka.channel.KafkaStreamTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.errors.DefaultProductionExceptionHandler
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.kafka.annotation.EnableKafkaStreams
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaStreamsConfiguration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaAdmin

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