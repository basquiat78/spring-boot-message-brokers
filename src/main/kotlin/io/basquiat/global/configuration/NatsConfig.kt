package io.basquiat.global.configuration

import io.basquiat.global.broker.common.code.BrokerChannel
import io.basquiat.global.listener.NatsErrorListener
import io.basquiat.global.properties.NatsProperties
import io.basquiat.global.utils.logger
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

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
        val streamName = props.apiStreamName
        val allowSubjects = props.apiAllowSubject

        val streamConfig =
            StreamConfiguration
                .builder()
                .name(streamName)
                .subjects(allowSubjects)
                .allowMessageTtl(true)
                .storageType(StorageType.File)
                // .replicas(3)
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